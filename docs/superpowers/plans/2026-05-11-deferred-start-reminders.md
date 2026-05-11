# Deferred Start Reminders Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Defer task start-time reminders while a focus block is running, release all overdue reminders after focus ends, notify only the highest-priority overdue task, and mark every overdue task entry with the user-provided alarm icon until that task is handled.

**Architecture:** Keep the existing `TaskReminderScheduler` / `TaskReminderReceiver` single-shot alarm chain. Add small SharedPreferences-backed stores for active focus state and start reminder due/deferred state, then let `AppShell` use in-memory task priority to choose which overdue task receives a notification. Do not introduce Room/DataStore, do not rewrite focus timer, and do not redesign task cards beyond adding the alarm marker.

**Tech Stack:** Kotlin, Jetpack Compose, Android AlarmManager, BroadcastReceiver, NotificationManager, SharedPreferences, vector/drawable asset.

---

## Product Semantics

### Core Rule

If a task reaches `startTime` while the user is already in a focus timer:

```text
start-time alarm fires
-> focus is active
-> do not show start-time notification
-> do not acknowledge reminder
-> store task as deferred start reminder
```

When the current focus block ends:

```text
focus finishes/exits
-> all deferred reminders become active due reminders
-> all active due tasks can show an alarm marker in the task list
-> only the highest-priority active due task gets a notification
```

### Priority Selection

When multiple due reminders exist, choose the notification target using the same priority order as the task list:

1. `Urgent & important`
2. `Urgent`
3. `Important`
4. `Not urgent`
5. same priority: lower task id first

This means "release all" does not mean "notify all". It means every overdue task enters the due queue, but only the top task interrupts the user.

### Acknowledgement

A start reminder is acknowledged only when the user handles that task:

- opens the corresponding task detail;
- taps the notification for that task;
- starts focus for that task.

The following actions do not acknowledge due reminders:

- notification heads-up disappearing;
- user dismissing notification;
- user opening the main Tasks page;
- user entering a whitelist app;
- focus ending.

When the user opens the main Tasks page, visible start-time notification may be cancelled, but the overdue state remains so task entries keep showing the alarm marker.

### Task List Marker

Every task whose start time has passed and has not been acknowledged should show an alarm marker on its task entry.

Asset requirement:

- Use the user-provided black outline alarm clock image as the visual reference.
- Preferred implementation: add an app drawable/vector named `ic_start_due_alarm`.
- If the exact source image is not available as a local file during implementation, recreate it as a simple black outline vector matching the provided reference: round alarm body, two bells, two legs, and clock hands.
- Place the icon near existing entry metadata where it does not crowd the completion circle.

## Existing Code To Preserve

Keep the current start-time reminder direction:

1. `TaskReminderScheduler.schedule(...)`
2. `AlarmManager.setExactAndAllowWhileIdle(...)`
3. `TaskReminderReceiver.onReceive(...)`
4. If not deferred and not acknowledged, show notification and schedule next single-shot alarm

Do not convert to repeating alarm.
Do not introduce `setAlarmClock()` in this task.
Do not remove the lockscreen `ReminderActivity` route created by the previous plan.

## Non-Goals

- Do not implement re-entry soft lock changes in this task.
- Do not redesign the task card layout beyond adding the alarm marker.
- Do not implement a full persistent task database.
- Do not change priority sorting rules.
- Do not change review, insights, settings, or schedule editor UI.
- Do not acknowledge all due reminders just because the app enters foreground.

## Files

**Create:**

- `app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt`
- `app/src/main/java/com/example/attentioncoach/platform/StartReminderStore.kt`
- `app/src/main/res/drawable/ic_start_due_alarm.xml`

**Modify:**

- `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
- `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`
- `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
- `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`

## Data Model

Use SharedPreferences for MVP.

### FocusSessionStore

Responsibility: expose whether a focus timer is currently active.

State:

```kotlin
focusActive: Boolean
activeTaskId: Long?
```

API:

```kotlin
class FocusSessionStore(private val context: Context) {
    fun setActive(taskId: Long)
    fun clearActive()
    fun isActive(): Boolean
}
```

### StartReminderStore

Responsibility: track start reminders that have reached due time but are not yet acknowledged.

State:

```text
deferredTaskIds: Set<Long>
activeDueTaskIds: Set<Long>
acknowledgedTaskIds: Set<Long>
```

API:

```kotlin
class StartReminderStore(private val context: Context) {
    fun defer(taskId: Long)
    fun releaseDeferred(): Set<Long>
    fun markActiveDue(taskId: Long)
    fun activeDueIds(): Set<Long>
    fun acknowledge(taskId: Long)
    fun isAcknowledged(taskId: Long): Boolean
    fun clearNotificationOnly(taskId: Long)
}
```

Keep this store narrow. It should not know task titles, priorities, or UI state.

## Domain Rules

Add pure helpers to `ReminderRules`.

```kotlin
fun highestPriorityDueTask(
    tasks: List<PlannedTask>,
    activeDueIds: Set<Long>
): PlannedTask?
```

Expected behavior:

- ignores ids not found in `tasks`;
- ignores finished/reviewed tasks;
- sorts by priority rank, then id;
- returns `null` if no eligible due task exists.

## Chunk 1: Domain Rules For Due Queue Priority

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`

- [ ] **Step 1: Write failing tests**

Add tests:

- highest priority due task is selected from multiple active due ids;
- same priority chooses lower id;
- finished/reviewed due tasks are ignored;
- unknown ids are ignored.

- [ ] **Step 2: Verify red**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: FAIL because `highestPriorityDueTask` does not exist.

- [ ] **Step 3: Implement minimal domain helper**

Implement `highestPriorityDueTask(...)` in `ReminderRules`.

- [ ] **Step 4: Verify green**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt
git commit -m "test: cover deferred start reminder priority"
```

## Chunk 2: Reminder State Stores

**Files:**

- Create: `app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/StartReminderStore.kt`

- [ ] **Step 1: Implement FocusSessionStore**

Use SharedPreferences. Keep values simple:

- `focus_active`
- `active_task_id`

- [ ] **Step 2: Implement StartReminderStore**

Use SharedPreferences string sets for ids.

When `acknowledge(taskId)` is called:

- add id to acknowledged;
- remove id from deferred;
- remove id from active due.

- [ ] **Step 3: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt app/src/main/java/com/example/attentioncoach/platform/StartReminderStore.kt
git commit -m "feat: add start reminder state stores"
```

## Chunk 3: Defer Receiver During Active Focus

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`

- [ ] **Step 1: Check focus state before notifying**

At the start of `onReceive()` after reading valid task id:

```text
if FocusSessionStore.isActive():
    StartReminderStore.defer(taskId)
    return
```

Do not acknowledge the reminder.
Do not schedule normal repeat while focus is active.

- [ ] **Step 2: Mark active due when not focused**

If focus is not active and task is not acknowledged:

- call `StartReminderStore.markActiveDue(taskId)`;
- show notification;
- schedule next single-shot repeat as today.

- [ ] **Step 3: Align acknowledgement helper**

`TaskReminderReceiver.acknowledgeReminder(...)` should also call `StartReminderStore.acknowledge(taskId)`.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "feat: defer start reminders during focus"
```

## Chunk 4: Release Deferred Reminders After Focus Ends

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt` if a public notify helper is needed

- [ ] **Step 1: Set focus active state**

When `activeWorkTask != null && activeWork?.isPaused == false`, call:

```kotlin
FocusSessionStore(context).setActive(activeWorkTask.id)
```

When focus finishes/exits or active work becomes null:

```kotlin
FocusSessionStore(context).clearActive()
```

Pause should still count as inside the focus flow if the user cannot leave during pause. If current app behavior stops monitoring on pause, do not broaden scope in this task unless necessary.

- [ ] **Step 2: Release deferred on Finish and Exit**

In focus `onFinish` and `onExit` handlers:

- clear focus active;
- call `StartReminderStore.releaseDeferred()`;
- compute `activeDueIds`;
- choose `ReminderRules.highestPriorityDueTask(tasks, activeDueIds)`;
- show notification only for the selected task.

All released ids remain active due, even if they are not selected for notification.

- [ ] **Step 3: Cancel visible notification on main app entry only**

When the user opens the main Tasks page, it is acceptable to cancel visible start-time notification, but do not acknowledge active due ids.

Do not keep the previous behavior that acknowledges all due reminders on app foreground.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "feat: release deferred start reminders after focus"
```

## Chunk 5: Alarm Marker On Task Entries

**Files:**

- Create: `app/src/main/res/drawable/ic_start_due_alarm.xml`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt` if due ids need to be passed down

- [ ] **Step 1: Add alarm icon asset**

Create `ic_start_due_alarm.xml` as a black outline vector based on the user-provided alarm image.

Keep it monochrome so Compose can tint it later if needed.

- [ ] **Step 2: Pass active due ids to TodayScreen**

`AppShell` should read `StartReminderStore.activeDueIds()` and pass the set down to task list UI.

Avoid making TodayScreen read SharedPreferences directly.

- [ ] **Step 3: Render marker on due task entries**

For each task card where `task.id in activeDueIds`:

- show the alarm icon near metadata;
- keep the completion circle behavior unchanged;
- do not add text labels for the marker in MVP.

- [ ] **Step 4: Acknowledge when opening/starting task**

When the user opens a due task detail or starts focus for that task:

- call `TaskReminderReceiver.acknowledgeReminder(context, taskId)`;
- remove the alarm marker.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/res/drawable/ic_start_due_alarm.xml app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt
git commit -m "feat: mark overdue start reminders in task list"
```

## Manual Debug Checklist

No emulator automation is required unless requested. Manual checks:

1. Start a focus block.
2. Create or wait for multiple other tasks to pass start time during that focus block.
3. Confirm no start-time notification appears while focus is running.
4. Finish or exit focus.
5. Confirm only the highest-priority overdue task sends a notification.
6. Confirm all overdue tasks show the alarm marker in the task list.
7. Open the main Tasks page; visible notification may disappear, but markers should remain.
8. Open a marked task detail; that task's marker should disappear.
9. Start a marked task; that task's marker should disappear.
10. Confirm lower-priority overdue tasks remain marked until opened/started.

## Acceptance Criteria

- Start-time alarms firing during focus are deferred, not acknowledged.
- Focus end releases all deferred tasks into active due state.
- Only the highest-priority active due task emits a notification after focus ends.
- All active due tasks can be visually identified in the task list with the alarm marker.
- Opening the main Tasks page cancels visible notification but does not clear due state.
- Opening or starting a specific due task acknowledges only that task.
- Whitelist app usage does not acknowledge start reminders.
- Existing lockscreen reminder route remains intact.
- `testDebugUnitTest` and `assembleDebug` pass before every commit.

