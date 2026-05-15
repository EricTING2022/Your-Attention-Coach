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

Status: Pending real-device test.

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
