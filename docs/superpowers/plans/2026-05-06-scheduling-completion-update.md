# Attention Coach Scheduling And Completion Update Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update Attention Coach so task review input, completion toggling, calendar behavior, scheduling, and one-step focus confirmations match the latest product decisions.

**Architecture:** Keep domain behavior testable in `domain/` first, then map it into Compose screens in `ui/`, and use small Android platform adapters in `platform/` only for alarm permission and reminder scheduling. Do not mix visual-only alignment with these functional changes; each subfeature gets its own test/build verification and commit.

**Tech Stack:** Kotlin, Jetpack Compose, Java Time (`LocalDate`, `LocalTime`, `YearMonth`, `Clock`), Android `AlarmManager`, Gradle unit tests, git.

---

## Scope And Assumptions

- Work in `D:\Desktop\HKUST\25-26spring\comp4521\project\attention_coach_android`.
- Leave the existing untracked `docs/emulator_screenshots/` directory out of all feature commits unless a later UI-alignment task explicitly decides to add it.
- Before every commit run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: both tasks complete successfully with `BUILD SUCCESSFUL`.

- Use one commit per completed or modified subfeature.
- The app is currently in-memory/demo-state based. Scheduling and exact-alarm permission should be wired in a way that works for the demo, but durable persistence is out of scope unless already present.
- Completing a task by tapping the hollow circle marks it `FINISHED` without inventing actual focus time. Unchecking a completed task returns it to `PLANNED`, clears actual focus and review-only fields, and hides Review again.
- Finish from the focus timer still records actual active focus minutes. Manual circle completion does not.
- Exact alarm permission on Android 12+ is a special app access (`SCHEDULE_EXACT_ALARM` + `AlarmManager.canScheduleExactAlarms()`), not a normal runtime permission dialog.
- Calendar calculations must be produced by `java.time`, not hard-coded day/month grids. For example, `LocalDate.of(2026, 5, 7).dayOfWeek` must be `THURSDAY`, and May 2026 must have 31 days.

## File Structure

Modify:

- `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
  - Change `mismatchReason` default to blank.
  - Add task scheduling state, preferably `startTime: LocalTime? = null`.
  - Remove `planningNote` from `PlannedTask` if the edit surface no longer uses it; otherwise keep it only as an unused migration-safe field and stop exposing it in UI. Prefer removal if the compile surface is small enough.

- `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
  - Update `createTask` and `updatePlan` signatures for `target`, `durationMinutes`, `priority`, and `startTime`.
  - Add `toggleTaskCompletion(taskId: Long)`.
  - Ensure `exitWork()` never changes task status, actual focus, or review fields.
  - Ensure `finishWork()` still writes actual focus and marks `FINISHED`.

- `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
  - Replace or supplement `TaskGrouper` with a no-divider list sorter.
  - Add pure completion helpers if needed.
  - Keep `WeekTimeline.weekFor()` based on `LocalDate`, no hard-coded dates.

- `app/src/main/java/com/example/attentioncoach/domain/DatePickerOptions.kt`
  - Confirm month options and date grids use `YearMonth.lengthOfMonth()`.
  - Add helper coverage for system-date-style calendar correctness if it belongs here.

- `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
  - Update demo tasks for removed planning notes and optional start times.

- `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
  - Initialize `selectedDate` from system today, not a hard-coded May 2026 date.
  - Pass task completion toggle into Today.
  - Pass start time into create/update flows.
  - Trigger reminder scheduling when a task has a date + start time and alarm permission allows it.

- `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`
  - Remove week-row pink/blue indicator dots.
  - Remove completed-section divider logic.
  - Render all tasks in one list.
  - Add strikethrough on completed task titles.
  - Replace task-entry right arrow with a hollow circle toggle.

- `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
  - Remove Planning note UI.
  - Reorder Plan layout to Duration, Priority, Targets large input.
  - Keep Review hidden for unfinished tasks.
  - Keep Review reason blank unless user types.
  - Add duration/start-time entry point.

- `app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt`
  - Simplify Finish and Exit from two-step confirmation to one-step confirmation.

- `app/src/main/AndroidManifest.xml`
  - Add exact alarm permission.
  - Register reminder receiver if a receiver is implemented.

Create if needed:

- `app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt`
  - Pure helpers for `today(clock)`, month days, weekday labels, and system-calendar testability.

- `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
  - Pure helper for converting task date/start time into trigger millis and deciding whether a reminder is schedulable.

- `app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt`
  - Android adapter for checking/requesting exact alarm access.

- `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
  - Android adapter around `AlarmManager`.

- `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
  - Broadcast receiver that posts a simple reminder notification.

- `app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
  - Compose editor for start time and duration if keeping it separate makes `TaskDetailSheet.kt` easier to read.

Tests:

- `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
  - Review reason default, update plan, completion toggle, exit/finish behavior.

- `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
  - Task list ordering without completed group semantics.

- `app/src/test/java/com/example/attentioncoach/domain/DatePickerOptionsTest.kt`
  - Calendar correctness for May 2026 and month lengths.

- Create `app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt` if `CalendarRules.kt` is created.

- Create `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt` if `ReminderRules.kt` is created.

## Chunk 1: Review Reason Default

### Task 1: Blank Review Reason

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
- Inspect: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`

- [ ] **Step 1: Write failing test for blank default reason**

Add to `AppStateTest.kt`:

```kotlin
@Test
fun newTasksUseBlankReviewReasonByDefault() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    val created = store.createTask(
        date = LocalDate.of(2026, 5, 7),
        title = "Blank reason task"
    )

    assertEquals("", created.mismatchReason)
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.attentioncoach.domain.AppStateTest.newTasksUseBlankReviewReasonByDefault"
```

Expected: FAIL because `mismatchReason` defaults to `"Attention faded"`.

- [ ] **Step 3: Implement minimal model change**

In `Models.kt`, change:

```kotlin
val mismatchReason: String = "Attention faded",
```

to:

```kotlin
val mismatchReason: String = "",
```

Check `TaskDetailSheet.kt`; if it initializes review reason from `task.mismatchReason`, no UI change is needed for this subfeature.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/Models.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt
git commit -m "fix: default review reason to blank"
```

## Chunk 2: One-Step Focus Confirmations

### Task 2: Simplify Exit And Finish Confirmation

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt`
- Test: existing `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`

- [ ] **Step 1: Inspect current confirm state**

Find the current two-step state variables in `FocusScreens.kt`, likely `exitConfirmStep` and `finishConfirmStep`.

- [ ] **Step 2: Replace two-step state with one-step booleans**

Use:

```kotlin
var showExitConfirm by remember { mutableStateOf(false) }
var showFinishConfirm by remember { mutableStateOf(false) }
```

Keep three work buttons: Pause, Finish, Exit.

- [ ] **Step 3: Update Finish confirmation copy and action**

Use a single dialog:

- Title: `Finish this task?`
- Body: `This will stop the timer and record your actual focus time.`
- Confirm: `Finish`
- Cancel: `Cancel`
- Confirm action: close dialog and call `onFinish()`.

- [ ] **Step 4: Update Exit confirmation copy and action**

Use a single dialog:

- Title: `Exit focus block?`
- Body: `This stops the current timer without changing the plan or recording focus time.`
- Confirm: `Exit`
- Cancel: `Cancel`
- Confirm action: close dialog and call `onExit()`.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt
git commit -m "fix: use single focus confirmations"
```

## Chunk 3: System Calendar Correctness

### Task 3: Remove Hard-Coded Today And Add Calendar Tests

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify or create: `app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/DatePickerOptionsTest.kt`
- Create if needed: `app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt`

- [ ] **Step 1: Write failing tests for real calendar facts**

Add tests covering:

```kotlin
@Test
fun maySevenTwentyTwentySixIsThursday() {
    assertEquals(DayOfWeek.THURSDAY, LocalDate.of(2026, 5, 7).dayOfWeek)
}

@Test
fun mayTwentyTwentySixHasThirtyOneDays() {
    assertEquals(31, YearMonth.of(2026, 5).lengthOfMonth())
}
```

If `DatePickerOptions` exposes month days, assert it returns exactly `1..31` for May 2026 and correctly starts under Thursday for May 7 selection.

- [ ] **Step 2: Add testable today helper if needed**

Create `CalendarRules.kt`:

```kotlin
package com.example.attentioncoach.domain

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

object CalendarRules {
    fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate = LocalDate.now(clock)

    fun daysInMonth(yearMonth: YearMonth): List<LocalDate> {
        return (1..yearMonth.lengthOfMonth()).map { day -> yearMonth.atDay(day) }
    }
}
```

Add test with a fixed clock:

```kotlin
@Test
fun todayUsesProvidedClock() {
    val clock = Clock.fixed(
        LocalDate.of(2026, 5, 6).atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant(),
        ZoneId.of("America/Los_Angeles")
    )

    assertEquals(LocalDate.of(2026, 5, 6), CalendarRules.today(clock))
}
```

- [ ] **Step 3: Replace hard-coded selected date**

In `AppShell.kt`, replace hard-coded:

```kotlin
LocalDate.of(2026, 5, 5)
```

for initial selected date with:

```kotlin
CalendarRules.today()
```

Do not hard-code today elsewhere.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt app/src/test/java/com/example/attentioncoach/domain/DatePickerOptionsTest.kt app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt
git commit -m "fix: use system calendar dates"
```

If `CalendarRules.kt` is not needed, omit it from `git add`.

## Chunk 4: Week Timeline Dot Removal

### Task 4: Remove Timeline Indicator Dots

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`

- [ ] **Step 1: Locate week day cell indicator row**

Find `WeekDayCell` in `TodayScreen.kt`. Locate the `Row` or `Box` elements that draw small blue/pink dots under each day.

- [ ] **Step 2: Remove dot rendering completely**

Delete the dot row and related special-case logic, including any `date.dayOfMonth == 5` demo condition.

Keep:

- weekday label
- day number
- selected day highlight
- week swipe behavior

- [ ] **Step 3: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git commit -m "fix: remove timeline day indicators"
```

## Chunk 5: Single Task List And Completion Toggle

### Task 5: Domain Completion Toggle

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`

- [ ] **Step 1: Write failing tests for toggle behavior**

Add tests:

```kotlin
@Test
fun toggleCompletionMarksPlannedTaskFinishedWithoutActualFocus() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.toggleTaskCompletion(1L)

    val task = store.taskById(1L)
    assertEquals(TaskStatus.FINISHED, task?.status)
    assertEquals(0, task?.actualFocusMinutes)
}

@Test
fun toggleCompletionAgainReturnsTaskToPlannedAndClearsReviewFields() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.toggleTaskCompletion(1L)
    store.saveReview(1L, "Done", "Reason", "Next")
    store.toggleTaskCompletion(1L)

    val task = store.taskById(1L)
    assertEquals(TaskStatus.PLANNED, task?.status)
    assertEquals(0, task?.actualFocusMinutes)
    assertEquals("", task?.actualCompletion)
    assertEquals("", task?.mismatchReason)
    assertEquals("", task?.nextAdjustment)
}
```

- [ ] **Step 2: Write failing test for list sorting without groups**

In `PlanningRulesTest.kt`, replace completed-group expectations with a single list rule:

```kotlin
@Test
fun taskListKeepsPausedFirstWithoutCompletedDividerModel() {
    val sorted = TaskListSorter.sortForToday(
        listOf(
            task(id = 1L, status = TaskStatus.PLANNED),
            task(id = 2L, status = TaskStatus.FINISHED),
            task(id = 3L, status = TaskStatus.PAUSED)
        )
    )

    assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
}
```

If there is no helper factory, create the test tasks inline with the existing `PlannedTask` constructor.

- [ ] **Step 3: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.attentioncoach.domain.AppStateTest.toggleCompletion*"
```

Expected: FAIL because `toggleTaskCompletion` does not exist.

- [ ] **Step 4: Implement `toggleTaskCompletion`**

In `AttentionCoachStore.kt`:

```kotlin
fun toggleTaskCompletion(taskId: Long) {
    val task = tasksById[taskId] ?: return
    tasksById[taskId] = if (task.status == TaskStatus.FINISHED || task.status == TaskStatus.REVIEWED) {
        task.copy(
            status = TaskStatus.PLANNED,
            actualFocusMinutes = 0,
            actualCompletion = "",
            mismatchReason = "",
            nextAdjustment = ""
        )
    } else {
        task.copy(
            status = TaskStatus.FINISHED,
            actualFocusMinutes = 0
        )
    }
}
```

- [ ] **Step 5: Implement no-divider sorter**

In `PlanningRules.kt` add:

```kotlin
object TaskListSorter {
    fun sortForToday(tasks: List<PlannedTask>): List<PlannedTask> {
        return tasks.sortedWith(
            compareByDescending<PlannedTask> { it.status == TaskStatus.PAUSED }
                .thenBy { it.status == TaskStatus.FINISHED || it.status == TaskStatus.REVIEWED }
                .thenBy { it.id }
        )
    }
}
```

Keep `TaskGrouper` only if existing code/tests still need it during the transition; remove it later only if all references are gone.

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt
git commit -m "feat: add task completion toggle rules"
```

### Task 6: Today UI Completion Toggle And Strikethrough

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`

- [ ] **Step 1: Pass toggle handler from app state**

In `AppShell.kt`, pass a new callback to `TodayScreen`:

```kotlin
onToggleTaskComplete = { taskId ->
    store.toggleTaskCompletion(taskId)
    tasks = store.tasksForDate(selectedDate)
}
```

- [ ] **Step 2: Render one task list**

In `TodayScreen.kt`, replace:

```kotlin
val groups = TaskGrouper.group(tasks)
```

with:

```kotlin
val visibleTasks = TaskListSorter.sortForToday(tasks)
```

Render `visibleTasks` once. Remove the horizontal divider between open and completed tasks.

- [ ] **Step 3: Replace arrow with hollow circle**

In `TaskCard`, replace the right-side `>` text with a circular button:

```kotlin
Box(
    modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .border(2.dp, if (task.isCompleted()) GoogleBlue else Border, CircleShape)
        .background(if (task.isCompleted()) GoogleBlue.copy(alpha = 0.10f) else Color.Transparent)
        .clickable { onToggleComplete(task.id) },
    contentAlignment = Alignment.Center
) {
    if (task.isCompleted()) {
        Icon(Icons.Rounded.Check, contentDescription = "Mark planned", tint = GoogleBlue, modifier = Modifier.size(18.dp))
    }
}
```

Use existing icons if available. If `Icons.Rounded.Check` is unavailable, use `Icons.Default.Check`.

- [ ] **Step 4: Add completed title strikethrough**

Use:

```kotlin
textDecoration = if (task.isCompleted()) TextDecoration.LineThrough else TextDecoration.None
```

Make the line obvious by also lowering alpha or using muted text only if it does not harm readability.

- [ ] **Step 5: Keep card click behavior**

Clicking the card body still opens task detail. Clicking the circle only toggles completion.

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git commit -m "feat: toggle task completion from today"
```

## Chunk 6: Plan Sheet Scheduling Fields

### Task 7: Remove Planning Notes And Add Start Time To Plan State

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`

- [ ] **Step 1: Write failing tests for schedule-aware plan update**

Add to `AppStateTest.kt`:

```kotlin
@Test
fun updatePlanStoresStartTimeDurationPriorityAndTarget() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.updatePlan(
        taskId = 1L,
        target = "Create the Compose shell.",
        startTime = LocalTime.of(9, 30),
        durationMinutes = 45,
        priority = Priority.URGENT
    )

    val task = store.taskById(1L)
    assertEquals("Create the Compose shell.", task?.target)
    assertEquals(LocalTime.of(9, 30), task?.startTime)
    assertEquals(45, task?.durationMinutes)
    assertEquals(Priority.URGENT, task?.priority)
}
```

- [ ] **Step 2: Update model**

In `Models.kt`, import `java.time.LocalTime` and update `PlannedTask`:

```kotlin
val startTime: LocalTime? = null,
val durationMinutes: Int,
```

Remove `planningNote` if compile updates are manageable. If removing it creates broad churn, stop exposing it in UI and leave it as an internal field with default `""` for now. The preferred final state is no Planning Note field in the model.

- [ ] **Step 3: Update store signatures**

Update `createTask`:

```kotlin
fun createTask(
    date: LocalDate,
    title: String,
    target: String = "",
    startTime: LocalTime? = null,
    durationMinutes: Int = 30,
    priority: Priority = Priority.IMPORTANT
): PlannedTask
```

Update `updatePlan`:

```kotlin
fun updatePlan(
    taskId: Long,
    target: String,
    startTime: LocalTime?,
    durationMinutes: Int,
    priority: Priority
)
```

- [ ] **Step 4: Update demo data and app call sites**

Remove `planningNote = ...` arguments from `DemoTaskRepository.kt`, `AppShell.kt`, and tests. Add optional start times to demo tasks where it helps the timeline feel real, but do not invent a new scheduling feature beyond the requested fields.

- [ ] **Step 5: Reorder Plan UI**

In `TaskDetailSheet.kt`, Plan page order becomes:

1. Duration row/card, showing start time if set plus duration.
2. Priority dropdown.
3. Targets large input.

Remove the Planning Note label and text area entirely.

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/Models.kt app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt
git commit -m "feat: add task schedule fields"
```

### Task 8: Duration And Start-Time Editor

**Files:**
- Create: `app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Create: `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt` if pure reminder calculations are added now

- [ ] **Step 1: Define editor behavior**

The Duration area in Plan is no longer a free text field. Tapping it opens a full-width dialog or bottom sheet with:

- Start time selector in 5-minute increments.
- Duration selector with common values plus custom minute entry if already easy in the existing UI.
- Save button.
- Cancel or close button.

Keep the first implementation simple and reliable:

- `LazyColumn` for start-time options.
- Horizontal selectable chips for durations: `15`, `30`, `45`, `60`, `90`.
- Selected values visibly highlighted.

- [ ] **Step 2: Implement `TaskScheduleEditor`**

Create composable signature:

```kotlin
@Composable
fun TaskScheduleEditor(
    initialStartTime: LocalTime?,
    initialDurationMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (LocalTime?, Int) -> Unit
)
```

Generate times:

```kotlin
private fun dayTimes(): List<LocalTime> =
    (0 until 24 * 60 step 5).map { minutes -> LocalTime.MIDNIGHT.plusMinutes(minutes.toLong()) }
```

Keep labels single-line, e.g. `7:50 AM`, using local formatting helper if one exists.

- [ ] **Step 3: Wire editor into Plan page**

In `TaskDetailSheet.kt`, clicking Duration sets `showScheduleEditor = true`. On save, update local `startTime` and `durationMinutes` state; the existing `Save task` or `Save plan` writes it back.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt
git commit -m "feat: add task schedule editor"
```

## Chunk 7: Alarm Permission And Reminder Scheduling

### Task 9: Exact Alarm Permission And Reminder Adapter

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
- Create: `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
- Create if needed: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify if needed: `app/src/main/java/com/example/attentioncoach/MainActivity.kt`

- [ ] **Step 1: Write pure reminder tests**

Create `ReminderRulesTest.kt`:

```kotlin
@Test
fun triggerMillisUsesTaskDateAndStartTime() {
    val zone = ZoneId.of("America/Los_Angeles")

    val trigger = ReminderRules.triggerAtMillis(
        date = LocalDate.of(2026, 5, 7),
        startTime = LocalTime.of(7, 50),
        zoneId = zone
    )

    assertEquals(
        ZonedDateTime.of(2026, 5, 7, 7, 50, 0, 0, zone).toInstant().toEpochMilli(),
        trigger
    )
}

@Test
fun taskWithoutStartTimeIsNotSchedulable() {
    assertFalse(ReminderRules.isSchedulable(null))
}
```

- [ ] **Step 2: Implement `ReminderRules`**

```kotlin
object ReminderRules {
    fun isSchedulable(startTime: LocalTime?): Boolean = startTime != null

    fun triggerAtMillis(date: LocalDate, startTime: LocalTime, zoneId: ZoneId): Long {
        return ZonedDateTime.of(date, startTime, zoneId).toInstant().toEpochMilli()
    }
}
```

- [ ] **Step 3: Add manifest permission**

Add:

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

If adding a receiver:

```xml
<receiver
    android:name=".platform.TaskReminderReceiver"
    android:exported="false" />
```

- [ ] **Step 4: Implement alarm permission helper**

`AlarmPermissionHelper.kt`:

```kotlin
class AlarmPermissionHelper(private val context: Context) {
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
```

- [ ] **Step 5: Implement reminder scheduler**

Use `AlarmManager.setExactAndAllowWhileIdle()` only when `canScheduleExactAlarms()` is true. If permission is not available, return a small result enum instead of silently failing:

```kotlin
enum class ReminderScheduleResult { SCHEDULED, NEEDS_EXACT_ALARM_PERMISSION, NO_START_TIME }
```

- [ ] **Step 6: Wire scheduling after plan save**

When a task with `startTime != null` is saved:

- Check exact alarm permission.
- If allowed, schedule reminder.
- If not allowed, show a concise in-app prompt with a button that opens exact alarm settings.
- Do not block saving the task if permission is denied.

- [ ] **Step 7: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/MainActivity.kt
git commit -m "feat: request alarm access for task reminders"
```

Omit any files not created.

## Chunk 8: Final Verification And Smoke Notes

### Task 10: End-To-End Verification Notes

**Files:**
- Modify: `docs/active_monitoring_smoke_test.md` or create `docs/scheduling_completion_smoke_test.md`

- [ ] **Step 1: Run full local verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install and smoke test if emulator/ADB is available**

Use existing emulator workflow. Smoke-test:

- Review reason starts blank.
- Exit confirmation is one dialog.
- Finish confirmation is one dialog.
- Today opens to actual system date.
- Week timeline has no dots.
- May 2026 date grid is correct; May 7, 2026 is Thursday.
- Completed tasks stay in the same list with title strikethrough.
- Hollow circle toggles task completion and uncompletion.
- Planning note is gone.
- Duration opens start-time/duration editor.
- Saving a start time either schedules reminder or clearly prompts for exact alarm access.

- [ ] **Step 3: Record limitations honestly**

If exact-alarm permission, notification permission, emulator background behavior, or UsageStats cannot be fully verified, document the limitation clearly. Do not claim full verification for anything not actually tested.

- [ ] **Step 4: Run final verification again**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit smoke notes**

```powershell
git add docs/active_monitoring_smoke_test.md docs/scheduling_completion_smoke_test.md
git commit -m "test: record scheduling completion smoke coverage"
```

Omit any docs not modified or created.

## Final Acceptance Criteria

- New and existing review reason fields do not show `"Attention faded"` unless the user types it.
- Exit and Finish each require exactly one confirmation dialog.
- Calendar initializes from system today and month/day grids come from real calendar rules.
- Week timeline has no blue or pink event dots.
- Today task list has no completed section and no divider.
- Completed tasks are visibly struck through.
- Task entry right affordance is a hollow circle toggle, not an arrow.
- Tapping the circle completes/uncompletes the task without opening the detail sheet.
- Tapping the task body still opens the detail sheet.
- Plan page has Duration, Priority, and one large Targets input; no Planning Note.
- Duration opens a start-time/duration editor.
- Setting a start time can ask for exact alarm access and schedules only when permitted.
- `testDebugUnitTest` and `assembleDebug` pass before every commit.
