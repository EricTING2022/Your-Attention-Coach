# Start Time Lockscreen Reminder Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make task start-time reminders remain active and visible across screen-on and screen-off states until the user enters Attention Coach to respond.

**Architecture:** Keep the existing single-shot alarm chain in `TaskReminderScheduler` and `TaskReminderReceiver`. Make the smallest changes needed to fix reminder acknowledgement semantics, remove notification auto-expiry, and add a lockscreen-safe Activity route for full-screen reminder delivery. Do not rewrite scheduling, task state, focus timer, re-entry monitoring, settings, or UI unrelated to start-time reminder delivery.

**Tech Stack:** Kotlin, Android AlarmManager, BroadcastReceiver, Notification channels, Jetpack Compose MainActivity route, Android lockscreen Activity APIs.

---

## Product Target

Start time reminder state is independent from task completion state.

- `reminder active`: the task start time has arrived and the user has not entered Attention Coach to respond.
- `acknowledged`: the user enters Attention Coach, either by tapping a reminder or by manually opening the app while due reminders exist.
- Notification dismiss, notification timeout, heads-up timeout, and whitelist app usage do not acknowledge the reminder.
- If multiple due reminders are active and the user manually opens Attention Coach, MVP behavior acknowledges all active due reminders.
- Whitelist apps are unrelated to start-time acknowledgement.

## Current Code To Preserve

The current alarm direction is correct and should not be replaced:

1. `TaskReminderScheduler.schedule(...)`
2. `AlarmManager.setExactAndAllowWhileIdle(...)`
3. `TaskReminderReceiver.onReceive(...)`
4. Show notification
5. If not acknowledged, schedule the next single-shot alarm after the selected notification interval

Keep this chain. Do not convert to `setRepeating`. Do not switch to `setAlarmClock()` in this task unless the user explicitly approves after manual testing shows `setExactAndAllowWhileIdle()` is insufficient.

## Non-Goals

- Do not change soft lock / re-entry behavior.
- Do not redesign Settings, Tasks, Plan, or Focus UI.
- Do not add emulator/ADB smoke testing for this subtask; the user will manually debug on device/emulator.
- Do not make a broad reminder persistence layer. Use existing SharedPreferences acknowledgement first.
- Do not change task status, completion, priority sorting, insights, or review logic.

## Files

**Modify:**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
- `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
- `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt` only if a helper call is needed; avoid changing scheduling behavior
- `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt` or a new focused test file if domain helpers are introduced

**Create:**

- `app/src/main/java/com/example/attentioncoach/platform/ReminderActivity.kt`

## Design Decisions

### Notification Lifetime

Remove `setTimeoutAfter(BANNER_TIMEOUT_MILLIS)` from start-time reminder notifications.

Reason: `setTimeoutAfter` removes the notification object itself. The desired behavior is that a heads-up banner may naturally collapse, but the notification should remain in the shade or lockscreen until the user enters Attention Coach or the next reminder replaces it.

### Full-Screen Route

Change the start-time reminder full-screen intent from broadcast to Activity.

Preferred route:

```text
TaskReminderReceiver
  -> Notification fullScreenIntent
  -> ReminderActivity
  -> MainActivity with ACTION_SCHEDULED_REMINDER + task_id
  -> AppShell opens task detail and acknowledges start-time reminder
```

Why not directly launch MainActivity as full-screen first:

- `ReminderActivity` can be tiny and lockscreen-specific.
- It avoids accidentally applying lockscreen behavior to all MainActivity launches.
- It keeps existing Compose navigation mostly unchanged.

### ReminderActivity Behavior

`ReminderActivity` should:

- call `setShowWhenLocked(true)` on Android O_MR1+;
- call `setTurnScreenOn(true)` on Android O_MR1+;
- use legacy window flags for older APIs if needed;
- read `task_id` and `task_title` from intent extras;
- show minimal text:
  - `Scheduled focus time`
  - task title
  - one primary button: `Open Attention Coach`
- opening the button launches MainActivity with `TaskReminderReceiver.ACTION_SCHEDULED_REMINDER` and `TaskReminderReceiver.EXTRA_TASK_ID`;
- finish itself after launching MainActivity.

This Activity is an enhancement. The high-importance public lockscreen notification remains the fallback if full-screen intent is restricted by Android.

### Acknowledgement Semantics

Move acknowledgement from "notification broadcast click" to "Attention Coach entry".

Required behavior:

- Tapping notification or ReminderActivity opens Attention Coach for that task and acknowledges that task.
- Manual app open acknowledges all currently active due reminders.
- Dismissing notification does not acknowledge.
- Notification timeout does not acknowledge.
- Whitelist app usage does not acknowledge.

Implementation shape:

- Keep SharedPreferences acknowledgement storage in `TaskReminderReceiver` for now.
- Expose focused public helper methods from `TaskReminderReceiver`:
  - `acknowledgeReminder(context, taskId)`
  - `acknowledgeDueReminders(context, taskIds)`
  - `cancelReminder(context, taskId)` if needed by `acknowledgeReminder`
- `MainActivity.captureScheduledReminderIntent(...)` should still set `scheduledReminderTaskId`.
- `AppShell` should call acknowledgement after it consumes `scheduledReminderTaskId`.
- For manual app open, add a narrow hook when app enters Attention Coach normally:
  - compute due active reminders from current in-memory tasks;
  - acknowledge those due reminders.

Important: because this app currently keeps tasks in memory, MVP due-reminder detection can use the existing in-memory `tasks` list and `startTime` values. Do not introduce Room/DataStore in this subtask.

## Chunk 1: Unit-Level Reminder Semantics

**Files:**

- Modify or create test: `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt` only if a pure helper is useful

- [ ] **Step 1: Add a pure due-reminder test**

Test a helper that identifies tasks whose start time is at or before `now` and are not completed by reminder acknowledgement. Keep this helper small if introduced.

Expected cases:

- future task is not due;
- task with no start time is not due;
- past or current start time is due;
- due detection uses task date + start time, not only local time.

- [ ] **Step 2: Run the focused unit test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: new test fails if helper is not implemented.

- [ ] **Step 3: Implement only the pure helper if needed**

Do not add Android dependencies to domain tests.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

Commit only the helper/test files for this chunk.

Suggested commit:

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt
git commit -m "test: cover start reminder acknowledgement rules"
```

## Chunk 2: Notification Lifetime and Acknowledgement Helpers

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`

- [ ] **Step 1: Remove notification auto-timeout**

Remove `builder.setTimeoutAfter(BANNER_TIMEOUT_MILLIS)` from start-time notification creation.

Delete `BANNER_TIMEOUT_MILLIS` if no longer used.

- [ ] **Step 2: Expose acknowledgement helpers**

Make acknowledgement and cancellation available to app entry points without exposing unrelated receiver internals.

Target API:

```kotlin
fun acknowledgeReminder(context: Context, taskId: Long)

fun acknowledgeReminders(context: Context, taskIds: Iterable<Long>)
```

Each acknowledgement should:

- mark the task reminder acknowledged in SharedPreferences;
- cancel pending alarm for that task;
- cancel the visible notification for that task.

- [ ] **Step 3: Keep receiver chain behavior unchanged**

`onReceive()` should still:

- ignore already acknowledged task reminders;
- show notification for unacknowledged reminders;
- schedule the next single-shot alarm.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

Suggested commit:

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "fix: keep start reminders until app acknowledgement"
```

## Chunk 3: Lockscreen ReminderActivity

**Files:**

- Create: `app/src/main/java/com/example/attentioncoach/platform/ReminderActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`

- [ ] **Step 1: Create ReminderActivity**

Minimal Activity responsibilities:

- enable show-when-locked and turn-screen-on;
- read `task_id` and `task_title`;
- show a simple Android view or Compose content;
- primary button opens MainActivity with `ACTION_SCHEDULED_REMINDER`.

Keep UI plain. This task is about lockscreen delivery, not visual polish.

- [ ] **Step 2: Register ReminderActivity**

Add to `AndroidManifest.xml`.

Use the existing app theme unless a no-actionbar theme is already available.

- [ ] **Step 3: Change full-screen intent target**

In `TaskReminderReceiver.buildNotification(...)`, set:

```text
contentIntent -> MainActivity scheduled reminder route or ReminderActivity
fullScreenIntent -> ReminderActivity
```

The notification content tap may use the same Activity route for consistency.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

Suggested commit:

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/attentioncoach/platform/ReminderActivity.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "feat: add lockscreen start reminder route"
```

## Chunk 4: App Entry Acknowledgement

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt` if helper signatures need adjustment

- [ ] **Step 1: Acknowledge reminder route**

When `scheduledReminderTaskId` is consumed in `AppShell`, call `TaskReminderReceiver.acknowledgeReminder(context, taskId)` after confirming the task exists.

This preserves current behavior of opening the task detail.

- [ ] **Step 2: Acknowledge manual app entry**

On normal app foreground entry, acknowledge all currently due reminders from the in-memory task list.

Keep this narrow:

- no persistent task repository;
- no new background worker;
- no UI changes.

Implementation option:

- `MainActivity` owns an `appEnteredAt` state tick updated in `onStart()`;
- pass it into `AttentionCoachApp`;
- `AppShell` uses `LaunchedEffect(appEnteredAt, tasks)` to acknowledge due reminders.

Only acknowledge tasks that:

- have a start time;
- scheduled date + start time is <= current time;
- are not already finished/reviewed if the existing product treats completed tasks as no longer needing start reminder.

- [ ] **Step 3: Do not acknowledge whitelist apps**

No code path outside Attention Coach should call `acknowledgeReminder`.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

Suggested commit:

```powershell
git add app/src/main/java/com/example/attentioncoach/MainActivity.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "fix: acknowledge start reminders on app entry"
```

## Manual Debug Checklist For User

No ADB emulator smoke is required from Codex for this subtask. After implementation, user manual checks:

1. Create a task with start time one or two minutes in the future.
2. Turn screen off before start time.
3. At start time, confirm the device wakes or presents a visible lockscreen reminder.
4. Do not enter Attention Coach; confirm the reminder continues at the configured interval.
5. Enter a whitelist app; confirm this does not acknowledge the start-time reminder.
6. Enter Attention Coach through the reminder; confirm reminder stops.
7. Create another due task, then manually open Attention Coach instead of tapping notification; confirm all active due reminders stop.

## Acceptance Criteria

- Start-time alarm chain remains single-shot and uses `setExactAndAllowWhileIdle()`.
- Start-time notification no longer auto-removes itself via `setTimeoutAfter`.
- Notification/full-screen path uses an Activity, not a broadcast.
- A lockscreen-safe Activity exists and can open Attention Coach.
- Entering Attention Coach acknowledges the relevant active start-time reminder.
- Dismissing or ignoring notification does not acknowledge.
- Whitelist app usage does not acknowledge.
- `testDebugUnitTest` and `assembleDebug` pass before every commit.

