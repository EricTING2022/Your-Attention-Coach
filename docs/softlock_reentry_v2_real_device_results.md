# Softlock Re-entry V2 Real-device Results

Date: 2026-05-15  
Branch: `feature/softlock-reentry-v2`

## Layer 0 Result

Status: Passed.

Reported result:

- Focus timer starts.
- Pause, finish, and exit flows work.
- Apps whitelist and notification interval persist after app restart.

## Layer 1 Result

Status: Passed with one follow-up refinement.

Layer 1 acceptance requires `AC_ForegroundV2` log evidence that the Accessibility observer can distinguish:

- Attention Coach;
- an Apps whitelist app;
- launcher;
- a non-whitelist app.

Use:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2
```

Record the observed `chosenPackage` values here after testing.

### User-provided log summary

Launcher:

- `chosenPackage=com.sec.android.app.launcher`

Whitelist app:

- `chosenPackage=com.android.chrome`

Attention Coach during focus timer:

- `chosenPackage=com.example.attentioncoach`

Attention Coach without focus timer:

- A transient System UI event appeared first:
  - `eventPackage=com.android.systemui`
  - `rootPackage=com.example.attentioncoach`
  - `chosenPackage=com.android.systemui`
- A correct Attention Coach event followed immediately:
  - `chosenPackage=com.example.attentioncoach`

Non-whitelist app:

- `chosenPackage=com.openrice.android`

### Interpretation

The real device can distinguish Attention Coach, a whitelist app, launcher, and a non-whitelist app. This satisfies the core Layer 1 requirement.

Two refinements were identified from the logs:

1. Accessibility events are event-driven, not a fixed polling cycle. The repeated lines are duplicate window/content events from the system, not a 2-second monitoring loop.
2. System UI can emit transient events while the actual app remains visible in `rootPackage`. For foreground classification, `rootPackage` should be preferred when `eventPackage` is `com.android.systemui`.

### Follow-up change

After this test, duplicate foreground observations were throttled to one record per package every 5 seconds, while package changes are still recorded immediately. This reduces noisy logs and repeated SharedPreferences writes without delaying launcher / whitelist / non-whitelist transitions.

## Layer 1 Follow-up Result

Status: Passed with foreground source rule refined.

User-provided logs after duplicate throttling showed:

- Accessibility service connected successfully.
- Attention Coach was observed as `rootPackage=com.example.attentioncoach`.
- Launcher was observed as `rootPackage=com.sec.android.app.launcher`.
- Chrome was observed as `rootPackage=com.android.chrome`.
- OpenRice was observed as `rootPackage=com.openrice.android`.

The logs also showed transient event-source packages that were not the actual foreground root:

- `eventPackage=com.sec.android.app.launcher` while `rootPackage=com.example.attentioncoach`;
- `eventPackage=com.example.attentioncoach` while `rootPackage=com.sec.android.app.launcher`;
- `eventPackage=com.google.android.googlequicksearchbox` while `rootPackage=com.sec.android.app.launcher`.

Updated understanding:

- `eventPackage` is the source of an Accessibility event, not always the current foreground app.
- `rootInActiveWindow.packageName` is the primary foreground signal for this real device.
- `eventPackage` should only be used when `rootPackage` is missing.

The foreground selection rule was updated to prefer `rootPackage`, then fall back to `eventPackage`, then to `windowPackages`.

## Layer 2 Result

Status: Partially passed, with screen-off refinement added.

Layer 2 adds a pure presence classifier and monitor diagnostic log:

```text
AC_PresenceV2: rawPackage=<package> source=<source> ageMillis=<age> presence=<presence> launcherPackages=<packages>
```

Expected presence mapping:

- `com.example.attentioncoach` -> `IN_ATTENTION_COACH`;
- any Apps whitelist package, such as Chrome -> `IN_WHITELIST_APP`;
- Android launcher package -> `IN_LAUNCHER`;
- other non-whitelist packages -> `IN_OTHER_APP`;
- stale or missing observation -> `UNKNOWN`.

Layer 2 intentionally does not change re-entry notification behavior yet. It only proves the classification layer that Layer 3 will use.

### Screen-off finding

User-provided screen-off logs showed this sequence:

- Before screen-off, the last reliable raw package was `com.example.attentioncoach`.
- After the screen had been off long enough, that observation became stale and classified as `UNKNOWN`.
- Accessibility then emitted lock-screen / System UI events:
  - `rootPackage=com.android.systemui`
  - `chosenPackage=com.android.systemui`
- The classifier previously treated `com.android.systemui` as `IN_OTHER_APP`.

This is not the intended product behavior. System UI / lock-screen is not a user-selected app and should not mean the user has left focus into a non-whitelist app.

Refinement added:

- `com.android.systemui` is classified as `UNKNOWN`, not `IN_OTHER_APP`.
- The monitor keeps a `lastStablePresence`.
- If the latest classified presence is `UNKNOWN`, the effective presence remains the previous stable value.

Expected effect:

- Focus page -> screen off should remain effectively `IN_ATTENTION_COACH`.
- Whitelist app -> screen off should remain effectively `IN_WHITELIST_APP`.
- Launcher / non-whitelist app -> screen off should keep the previous violating state, because the previous stable state was already `IN_LAUNCHER` or `IN_OTHER_APP`.

## Layer 3 Result

Status: Pending real-device test.

Layer 3 changes the screen-on re-entry decision source:

- Before: `FocusMonitorService` used `UsageStatsManager.latestForegroundPackage()` to decide whether to show a re-entry reminder.
- Now: `FocusMonitorService` uses the Layer 2 `FocusPresence` result.

Policy:

- `IN_ATTENTION_COACH`: clear visible re-entry reminder and reset violation/cooldown state.
- `IN_WHITELIST_APP`: clear visible re-entry reminder and reset violation/cooldown state.
- `IN_LAUNCHER`: start a 2-second grace period, then remind; repeat by notification interval.
- `IN_OTHER_APP`: start a 2-second grace period, then remind; repeat by notification interval.
- `UNKNOWN`: do not treat as whitelist; log degraded detection and keep existing state.

Screen-off behavior:

- Layer 3 intentionally skips screen-on re-entry policy while the device is not interactive.
- Screen-off alarm reminders are reserved for Layer 4.

New diagnostic tag:

```text
AC_ReentryV2
```

Expected log shape:

```text
AC_ReentryV2: presence=<presence> reason=<reason> shouldNotify=<true|false> shouldClear=<true|false> violationStarted=<millis|null> lastNotification=<millis|null>
```

## Layer 4 Result

Status: Pending real-device test.

Layer 4 adds screen-off re-entry alarms.

Implementation summary:

- `FocusMonitorService` persists a narrow `ReentryMonitorState` containing only active task id/title, effective presence, violation start time, last notification time, and notification interval.
- `ReentryReminderReceiver` uses a single-shot AlarmManager chain while the screen is off.
- Screen-off logic reads the last reliable effective `FocusPresence`; it does not use `com.android.systemui` as a user app signal.
- Returning to screen-on monitoring cancels the pending re-entry alarm.
- Stopping focus monitoring clears persisted re-entry state, visible re-entry notification, and pending alarm.

Expected behavior:

- Screen off from `IN_ATTENTION_COACH`: no screen-off reminder.
- Screen off from `IN_WHITELIST_APP`: no screen-off reminder.
- Screen off from `IN_LAUNCHER`: screen-off reminder repeats by notification interval.
- Screen off from `IN_OTHER_APP`: screen-off reminder repeats by notification interval.
- Returning to Attention Coach cancels visible reminders and pending alarms.

New diagnostic tag:

```text
AC_ReentryAlarmV2
```

Expected log shape:

```text
AC_ReentryAlarmV2: screenOff presence=<presence> reason=<reason> schedule=<true|false> clear=<true|false> delay=<millis> violationStarted=<millis|null> lastNotification=<millis|null>
AC_ReentryAlarmV2: alarm presence=<presence> reason=<reason> delay=<millis> task=<taskId>
```

## Layer 4 Result Update

Status: Passed by user real-device testing.

Reported result:

- Screen off from focus page does not remind.
- Screen off from whitelist app does not remind.
- Screen off from launcher reminds and repeats by interval.
- Screen off from non-whitelist app reminds and repeats by interval.

Follow-up observation:

- In an unsafe screen-off state, the reminder notification was delivered, but it did not surface as a lockscreen full-screen reminder on the test device.
- Safe screen-off still produced repeated screen-off diagnostic logs every poll cycle, for example repeated `presence=IN_ATTENTION_COACH reason=SELF` lines.

Layer 4.1 addresses these without changing the V2 presence/state machine:

- add a lockscreen `ReentryLockscreenActivity` route for the first unsafe screen-off reminder in a violation chain;
- pause normal foreground polling while the screen is off;
- resume polling on `ACTION_SCREEN_ON`;
- keep the AlarmManager chain responsible for unsafe screen-off repeats.

## Layer 4.1 Result

Status: Pending real-device test.

Expected result:

- Unsafe screen-off reminder uses the full-screen route once when Android allows it.
- The `Return to focus` button opens Attention Coach through the existing re-entry intent.
- Safe screen-off no longer logs repeated 3-second polling checks.
- Unsafe screen-off repeats are driven by `AC_ReentryAlarmV2: alarm ...` rather than foreground polling.
