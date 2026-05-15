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
