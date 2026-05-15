# Softlock Re-entry V2 Manual Test Guide

Date: 2026-05-15  
Branch: `feature/softlock-reentry-v2`  
Layer: 0 - Clean baseline

## Purpose

Layer 0 proves that the new V2 worktree starts from a stable app baseline before rebuilding softlock re-entry. This layer does not add Accessibility foreground detection or new reminder behavior.

The goal is to confirm that the current app still supports the core focus workflow and settings persistence before any softlock-specific implementation begins.

## Build Verification

Run from:

```text
D:\Desktop\HKUST\25-26spring\comp4521\project\attention_coach_android\.worktrees\softlock-reentry-v2
```

Commands:

```powershell
git status --short --branch
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected result:

- Git branch is `feature/softlock-reentry-v2`.
- Worktree is clean except for planned documentation edits before commit.
- Unit tests pass.
- Debug APK builds successfully.

## Manual Smoke Test

Install the debug build on the test phone, then run the checks below.

### S00-1: Start Focus Timer

Steps:

1. Open Attention Coach.
2. Select or create a task with a duration.
3. Tap `Start work block`.

Expected result:

- Focus timer opens.
- Timer starts counting down from the task duration.
- The app does not crash.

### S00-2: Pause Flow

Steps:

1. While the focus timer is running, tap `Pause`.
2. Observe the pause screen.

Expected result:

- Pause countdown opens.
- Pause countdown starts from the configured default pause duration.
- Returning from pause resumes the focus workflow.

### S00-3: Finish Flow

Steps:

1. Start a focus timer.
2. Tap `Finish`.
3. Confirm the finish dialog.
4. Return to the task list.

Expected result:

- The active focus session ends.
- The task becomes finished.
- Actual focus time is recorded for the task.
- The app returns to the normal task flow.

### S00-4: Exit Flow

Steps:

1. Start a focus timer.
2. Tap `Exit`.
3. Confirm the exit dialog.

Expected result:

- The active focus session ends.
- The task plan is not overwritten as a finished task.
- The app returns to the normal task flow.

### S00-5: Settings Persistence

Steps:

1. Open `Settings`.
2. Open `Apps whitelist`.
3. Add or remove a whitelist app.
4. Change the notification interval.
5. Fully close and reopen Attention Coach.

Expected result:

- Apps whitelist persists after reopening.
- Notification interval persists after reopening.
- The settings page displays the current whitelist count and interval, not hardcoded values.

## Layer 0 Pass Criteria

Layer 0 is considered passed when:

- `testDebugUnitTest` passes.
- `assembleDebug` passes.
- The app installs and opens on the test phone.
- The focus timer can start.
- Pause, finish, and exit flows do not break.
- Apps whitelist and notification interval persist after app restart.

## Layer 0 Result

Status: Passed by manual real-device smoke testing.

Result reported on 2026-05-15:

- Focus timer starts successfully.
- Pause, finish, and exit flows work.
- Settings persistence works for Apps whitelist and notification interval.
- No Layer 0 blocking issue was reported.

## Notes For Later Layers

Do not use Layer 0 to judge softlock correctness. The current main baseline still relies on UsageStats-based focus monitoring, which is known to be insufficient for reliable whitelist / launcher / non-whitelist distinction on the real device.

Layer 1 will add the primary Accessibility foreground observer and must be tested separately with foreground package logs.

## Layer 1 Manual Test: Primary Foreground Observer

Layer 1 only verifies foreground package detection. It does not validate re-entry reminder behavior yet.

### Setup

1. Install the Layer 1 debug APK.
2. Open Attention Coach.
3. Go to `Settings`.
4. Tap `Foreground detection`.
5. In Android Accessibility settings, enable the Attention Coach service.
6. Return to Attention Coach.

Expected setup result:

- Settings shows `Foreground detection` as `Ready` after the service is enabled.
- Logcat shows `AC_ForegroundV2: connected` after the service starts.

### Log Capture

Use:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2
```

### S01-1: Attention Coach Foreground

Steps:

1. Open Attention Coach.
2. Navigate between Tasks, Insights, and Settings.

Expected log evidence:

- `chosenPackage=com.example.attentioncoach` appears, or Attention Coach is otherwise visible as `eventPackage`, `rootPackage`, or a window package.
- If a transient `eventPackage=com.android.systemui` appears while `rootPackage=com.example.attentioncoach`, the observer should classify the foreground as Attention Coach.

### S01-2: Whitelist App Foreground

Steps:

1. Open Chrome or another app in Apps whitelist.
2. Stay in that app for several seconds.

Expected log evidence:

- The app package appears as `chosenPackage`, for example `chosenPackage=com.android.chrome`.

### S01-3: Launcher Foreground

Steps:

1. Press Home.
2. Stay on the launcher for several seconds.

Expected log evidence:

- The launcher package appears as `chosenPackage`.
- The launcher package must be non-null and different from the whitelist package.

### S01-4: Non-whitelist App Foreground

Steps:

1. Open an app that is not in Apps whitelist.
2. Stay in that app for several seconds.

Expected log evidence:

- That app's package appears as `chosenPackage`.
- It must be distinguishable from Attention Coach, whitelist apps, and launcher.

### S01-5: Screen-off Does Not Erase Last Reliable Package

Steps:

1. Open Attention Coach, then turn the screen off.
2. Turn the screen on and check recent logs.
3. Open a whitelist app, then turn the screen off.
4. Turn the screen on and check recent logs.

Expected log evidence:

- Screen-off does not create fake foreground packages.
- The observer does not collapse all states into `chosenPackage=null` immediately after screen-off.

### Layer 1 Pass Criteria

Layer 1 is considered passed only when real-device logs prove all of these:

- Attention Coach package can be observed.
- A whitelist app package can be observed.
- Launcher package can be observed.
- A non-whitelist app package can be observed.
- The four states are distinguishable by package.
- Duplicate logs for the same package are throttled to roughly 5 seconds, but switching to a different package is still recorded immediately.

If `chosenPackage` stays `null`, or if whitelist / launcher / non-whitelist all look the same, stop before Layer 2. The Accessibility observer must be fixed before any re-entry policy is implemented.

## Layer 2 Manual Test: Presence Classifier

Layer 2 converts raw foreground observations into product states. It still does not change the re-entry notification policy.

### Setup

1. Install the Layer 2 debug APK.
2. Enable `Foreground detection` in Android Accessibility settings.
3. Add Chrome, or another test app, to `Apps whitelist`.
4. Start a focus timer so `FocusMonitorService` is running.

### Log Capture

Use both tags:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2 AC_PresenceV2
```

### S02-1: Attention Coach Presence

Steps:

1. Stay on the focus timer page.

Expected log evidence:

- `AC_ForegroundV2` shows `chosenPackage=com.example.attentioncoach`.
- `AC_PresenceV2` shows `presence=IN_ATTENTION_COACH`.

### S02-2: Whitelist App Presence

Steps:

1. Open Chrome or another app in Apps whitelist.

Expected log evidence:

- `rawPackage` is the whitelist package, for example `com.android.chrome`.
- `presence=IN_WHITELIST_APP`.

### S02-3: Launcher Presence

Steps:

1. Press Home and stay on launcher.

Expected log evidence:

- `rawPackage` is the launcher package.
- `presence=IN_LAUNCHER`.

### S02-4: Non-whitelist App Presence

Steps:

1. Open an app that is not in Apps whitelist.

Expected log evidence:

- `rawPackage` is that app package.
- `presence=IN_OTHER_APP`.

### S02-5: Stale / Missing Observation

This is mainly covered by unit tests. On device, if Accessibility is disabled or stops reporting fresh data, `AC_PresenceV2` may show `classified=UNKNOWN`.

### S02-6: Screen-off Keeps Last Stable Presence

Steps:

1. Start from the focus timer page.
2. Turn the screen off for at least 15 seconds.
3. Turn the screen on and inspect `AC_PresenceV2`.
4. Repeat from a whitelist app, launcher, and non-whitelist app.

Expected log evidence:

- If the raw package becomes `com.android.systemui`, `classified=UNKNOWN`.
- The effective `presence` remains the previous stable state.
- Focus page before screen-off remains effectively `presence=IN_ATTENTION_COACH`.
- Whitelist app before screen-off remains effectively `presence=IN_WHITELIST_APP`.
- Launcher before screen-off remains effectively `presence=IN_LAUNCHER`.
- Non-whitelist app before screen-off remains effectively `presence=IN_OTHER_APP`.

### Layer 2 Pass Criteria

Layer 2 is considered passed when:

- Attention Coach is classified as `IN_ATTENTION_COACH`.
- Apps whitelist app is classified as `IN_WHITELIST_APP`.
- Launcher is classified as `IN_LAUNCHER`.
- Non-whitelist app is classified as `IN_OTHER_APP`.
- `UNKNOWN` is only used when the raw observation is missing or stale.
- System UI / lock-screen does not overwrite the last stable foreground presence.

## Layer 3 Manual Test: Screen-on Re-entry Policy

Layer 3 uses `FocusPresence` as the only input for screen-on re-entry reminders. It no longer uses `UsageStatsManager` to decide whether to remind.

Screen-off repeat reminders are intentionally not implemented in this layer. When the screen is off, the service logs and skips screen-on policy; Layer 4 will add AlarmManager-based screen-off reminders.

### Setup

1. Install the Layer 3 debug APK.
2. Enable `Foreground detection` in Android Accessibility settings.
3. Add Chrome, or another test app, to `Apps whitelist`.
4. Set `Notification interval` to a short value such as `30s`.
5. Start a focus timer.

### Log Capture

Use:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2 AC_PresenceV2 AC_ReentryV2
```

### S03-1: Focus Page Does Not Remind

Steps:

1. Stay on the focus timer page for at least 10 seconds.

Expected result:

- No re-entry banner appears.
- `AC_ReentryV2` shows `presence=IN_ATTENTION_COACH` and `reason=SELF`.

### S03-2: Launcher Reminds After Grace

Steps:

1. From focus timer, press Home.
2. Stay on launcher.

Expected result:

- No immediate reminder during the 2-second grace period.
- A re-entry reminder appears after grace.
- `AC_ReentryV2` shows `presence=IN_LAUNCHER`.

### S03-3: Whitelist App Suppresses Reminder

Steps:

1. From focus timer, open Chrome or another Apps whitelist app.
2. Stay in that app.

Expected result:

- No re-entry reminder appears.
- If a previous reminder was visible, it is cleared.
- `AC_ReentryV2` shows `presence=IN_WHITELIST_APP` and `reason=NEEDED_APP`.

### S03-4: Launcher To Whitelist Clears Reminder

Steps:

1. From focus timer, go to launcher and wait for a re-entry reminder.
2. Open the whitelist app.

Expected result:

- The visible re-entry reminder is cleared.
- No repeated reminder appears while staying in the whitelist app.

### S03-5: Whitelist To Launcher Reminds Again

Steps:

1. From the whitelist app, press Home.
2. Stay on launcher.

Expected result:

- A new 2-second grace period starts.
- A re-entry reminder appears after grace.

### S03-6: Non-whitelist App Reminds

Steps:

1. Open an app that is not in Apps whitelist.

Expected result:

- A re-entry reminder appears after grace.
- `AC_ReentryV2` shows `presence=IN_OTHER_APP`.

### S03-7: Returning To Attention Coach Clears Reminder

Steps:

1. Trigger a re-entry reminder from launcher or a non-whitelist app.
2. Return to Attention Coach.

Expected result:

- The visible re-entry reminder is cleared.
- `AC_ReentryV2` shows `presence=IN_ATTENTION_COACH` and `reason=SELF`.

### Layer 3 Pass Criteria

Layer 3 is considered passed when:

- Focus page never triggers a re-entry reminder.
- Whitelist app never triggers a re-entry reminder.
- Launcher triggers a reminder after the 2-second grace period.
- Non-whitelist app triggers a reminder after the 2-second grace period.
- Moving from launcher/non-whitelist app to whitelist app clears and suppresses reminders.
- Moving from whitelist app to launcher/non-whitelist app starts a new grace period and reminds again.
- Returning to Attention Coach clears the reminder.

## Layer 4 Manual Test: Screen-off Re-entry Alarms

Layer 4 adds screen-off reminders using a single-shot AlarmManager chain. It uses the last reliable `FocusPresence` from Layer 2/3; it does not classify `com.android.systemui` as a user app.

### Setup

1. Install the Layer 4 debug APK.
2. Enable `Foreground detection` in Android Accessibility settings.
3. Add Chrome, or another test app, to `Apps whitelist`.
4. Set `Notification interval` to a short value such as `30s`.
5. Start a focus timer.

### Log Capture

Use:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2 AC_PresenceV2 AC_ReentryV2 AC_ReentryAlarmV2
```

### S04-1: Focus Page To Screen Off Does Not Remind

Steps:

1. Stay on the focus timer page.
2. Turn the screen off.
3. Wait longer than one notification interval.

Expected result:

- No re-entry reminder appears.
- `AC_ReentryAlarmV2` shows `presence=IN_ATTENTION_COACH` with `clear=true` or no scheduled alarm.

### S04-2: Whitelist App To Screen Off Does Not Remind

Steps:

1. Open Chrome or another Apps whitelist app.
2. Turn the screen off.
3. Wait longer than one notification interval.

Expected result:

- No re-entry reminder appears.
- `AC_ReentryAlarmV2` shows `presence=IN_WHITELIST_APP` with `clear=true` or no scheduled alarm.

### S04-3: Launcher To Screen Off Repeats Reminder

Steps:

1. From focus timer, press Home.
2. Wait until the app classifies launcher as `IN_LAUNCHER`.
3. Turn the screen off.
4. Wait for the first reminder and at least one repeat interval.

Expected result:

- Re-entry reminder appears while the screen is off / locked.
- Reminder repeats by the configured notification interval.
- `AC_ReentryAlarmV2` shows `presence=IN_LAUNCHER`.

### S04-4: Non-whitelist App To Screen Off Repeats Reminder

Steps:

1. Open a non-whitelist app.
2. Wait until the app classifies it as `IN_OTHER_APP`.
3. Turn the screen off.
4. Wait for the first reminder and at least one repeat interval.

Expected result:

- Re-entry reminder appears while the screen is off / locked.
- Reminder repeats by the configured notification interval.
- `AC_ReentryAlarmV2` shows `presence=IN_OTHER_APP`.

### S04-5: Return To Attention Coach Cancels Alarm

Steps:

1. Trigger a screen-off reminder from launcher or a non-whitelist app.
2. Unlock and return to Attention Coach.
3. Wait longer than one notification interval.

Expected result:

- Visible re-entry reminder is cleared.
- Pending screen-off alarm is cancelled.
- No additional re-entry reminder appears after returning to Attention Coach.

### Layer 4 Pass Criteria

Layer 4 is considered passed when:

- Screen off from focus page does not remind.
- Screen off from whitelist app does not remind.
- Screen off from launcher reminds and repeats by interval.
- Screen off from non-whitelist app reminds and repeats by interval.
- Returning to Attention Coach cancels the visible reminder and pending alarm.
- Lock-screen / System UI events do not create a new violation by themselves.
