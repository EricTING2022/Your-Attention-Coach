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

## Notes For Later Layers

Do not use Layer 0 to judge softlock correctness. The current main baseline still relies on UsageStats-based focus monitoring, which is known to be insufficient for reliable whitelist / launcher / non-whitelist distinction on the real device.

Layer 1 will add the primary Accessibility foreground observer and must be tested separately with foreground package logs.
