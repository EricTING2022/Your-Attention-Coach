# Attention Coach Schedule & Completion Update Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update Attention Coach so review fields are blank by default, focus confirmations are simpler, calendar logic follows the system date, timeline dots disappear, tasks can be completed from the list, and task planning supports start time + duration reminders.

**Architecture:** Keep this iteration surgical: domain model changes live in `domain/`, Compose interaction changes stay in `ui/`, and Android alarm permission/scheduling lives in `platform/`. There is no persistence layer yet, so model migrations are simple constructor/test updates rather than Room schema changes.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Java Time (`LocalDate`, `LocalTime`, `YearMonth`, `Clock`), Android `AlarmManager`, Android exact alarm settings intent, JUnit 4.

---

## File Structure

### Domain

- Modify `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
  - Remove default `"Attention faded"` from `mismatchReason`.
  - Add `startTime: LocalTime? = null` to `PlannedTask`.
  - Keep `planningNote` only if needed during transition; final UI should not expose it. Prefer removing it from the model if all call sites are updated in the same task.
- Modify `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
  - Update seeds so review reasons are explicit only for reviewed demo tasks.
  - Add start times for some demo tasks.
- Modify `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
  - Update `createTask()` and `updatePlan()` signatures for `startTime` and no planning note.
  - Add `toggleTaskCompleted(taskId: Long)`.
  - Add `updateSchedule(taskId, startTime, durationMinutes)`.
- Modify `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
  - Update `TaskGrouper` so Today list is one list, not open/completed sections.
  - Keep paused tasks sorted first.
  - Add or update calendar helper rules if useful.
- Create `app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt`
  - Centralize `today(clock)`, `weekFor(date)`, and `daysForMonth(yearMonth)` so the calendar is testable.
- Create `app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt`
  - Generate 5-minute start-time slots and fixed demo-friendly duration options.

### UI

- Modify `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
  - Initialize `selectedDate` from `LocalDate.now()`, not hard-coded `2026-05-05`.
  - Wire `onToggleTaskCompleted`.
  - Wire schedule updates from task detail.
  - Trigger alarm permission request when a task with start time is saved.
- Modify `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`
  - Remove timeline day indicator dots entirely.
  - Remove divider/completed-section rendering.
  - Replace right chevron with a completion circle.
  - Strike through completed task titles.
  - Keep tapping the card body opening task detail.
- Modify `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
  - Remove planning note from Plan page.
  - Reorder Plan fields to `Duration`, `Priority`, then a large `Targets` input.
  - Make Duration a read-only control that opens a schedule editor.
  - Save `startTime` and `durationMinutes`.
  - Ensure Review reason starts blank unless the task already has a saved reason.
- Modify `app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt`
  - Change Finish and Exit from two-step to one confirmation dialog.
- Create `app/src/main/java/com/example/attentioncoach/ui/ScheduleEditorDialog.kt`
  - Bottom/sheet-style editor inspired by the provided reference:
    - start-time wheel/list with 5-minute increments;
    - duration chips/list: `1`, `15`, `30`, `45`, `60`, `90` minutes;
    - Save/Cancel actions.

### Platform

- Modify `app/src/main/AndroidManifest.xml`
  - Add `android.permission.SCHEDULE_EXACT_ALARM`.
  - Add a broadcast receiver for reminders if scheduling is implemented in this iteration.
- Create `app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt`
  - Wrap `AlarmManager.canScheduleExactAlarms()`.
  - Build `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)`.
- Create `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
  - Schedule a task-start reminder using `AlarmManager.setExactAndAllowWhileIdle()` when permission is available.
  - No-op safely when `startTime == null` or permission is unavailable.
- Create `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
  - Show a notification for scheduled reminders.

### Tests

- Modify `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`.
- Modify `app/src/test/java/com/example/attentioncoach/domain/DatePickerOptionsTest.kt` if month/year helpers move.
- Create `app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt`.
- Create `app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt`.
- Create `app/src/test/java/com/example/attentioncoach/domain/TaskCompletionRulesTest.kt` if keeping completion tests separate.

### Commands

Run before every commit:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

If sandbox blocks Gradle cache access, rerun the same command with escalation. Do not skip verification.

---

## Chunk 1: Review Defaults And One-Step Focus Confirmations

### Task 1: Remove Default Review Reason

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `AppStateTest`:

```kotlin
@Test
fun newTasksHaveBlankReviewReasonByDefault() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    val created = store.createTask(
        date = LocalDate.of(2026, 5, 7),
        title = "Blank reason task"
    )

    assertEquals("", created.mismatchReason)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest
```

Expected: FAIL because `PlannedTask.mismatchReason` currently defaults to `"Attention faded"`.

- [ ] **Step 3: Implement the minimal domain change**

In `Models.kt`, change:

```kotlin
val mismatchReason: String = "Attention faded",
```

to:

```kotlin
val mismatchReason: String = "",
```

In `DemoTaskRepository.kt`, explicitly set `mismatchReason = "Attention faded"` only for demo tasks that should display an existing review reason. Do not give planned/new tasks a reason.

- [ ] **Step 4: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/domain/Models.kt app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt
git -C 'project\attention_coach_android' commit -m 'fix: start review reason blank'
```

### Task 2: Make Finish And Exit One-Step Confirmations

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt`

- [ ] **Step 1: Inspect current confirmation code**

Find `finishConfirmStep` and `exitConfirmStep` in `FocusScreens.kt`.

- [ ] **Step 2: Replace step counters with booleans**

Use:

```kotlin
var confirmExit by remember { mutableStateOf(false) }
var confirmFinish by remember { mutableStateOf(false) }
```

Back handler:

```kotlin
BackHandler { confirmExit = true }
```

Buttons:

```kotlin
OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(54.dp)) {
    Text("Pause", fontWeight = FontWeight.Bold)
}
Button(
    onClick = { confirmFinish = true },
    modifier = Modifier.weight(1f).height(54.dp),
    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleGreen)
) {
    Text("Finish", fontWeight = FontWeight.Bold)
}
Button(
    onClick = { confirmExit = true },
    modifier = Modifier.weight(1f).height(54.dp),
    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.RedChipText)
) {
    Text("Exit", fontWeight = FontWeight.Bold)
}
```

Finish dialog confirm button should call `onFinish()` directly. Exit dialog confirm button should call `onExit()` directly.

- [ ] **Step 3: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/ui/FocusScreens.kt
git -C 'project\attention_coach_android' commit -m 'fix: simplify focus confirmations'
```

---

## Chunk 2: Calendar Correctness And Timeline Cleanup

### Task 3: Use System Date And Central Calendar Rules

**Files:**
- Create: `app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
- Create: `app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt`

- [ ] **Step 1: Write failing calendar tests**

Create `CalendarRulesTest.kt`:

```kotlin
package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

class CalendarRulesTest {
    @Test
    fun maySevenTwentyTwentySixIsThursday() {
        assertEquals(DayOfWeek.THURSDAY, LocalDate.of(2026, 5, 7).dayOfWeek)
    }

    @Test
    fun monthLengthUsesJavaTimeCalendarRules() {
        assertEquals(31, CalendarRules.daysForMonth(YearMonth.of(2026, 5)).size)
        assertEquals(28, CalendarRules.daysForMonth(YearMonth.of(2026, 2)).size)
        assertEquals(29, CalendarRules.daysForMonth(YearMonth.of(2024, 2)).size)
    }

    @Test
    fun todayComesFromInjectedClock() {
        val clock = Clock.fixed(
            Instant.parse("2026-05-06T12:00:00Z"),
            ZoneId.of("UTC")
        )

        assertEquals(LocalDate.of(2026, 5, 6), CalendarRules.today(clock))
    }

    @Test
    fun weekStartsOnSundayForSelectedDate() {
        assertEquals(
            LocalDate.of(2026, 5, 3),
            CalendarRules.weekFor(LocalDate.of(2026, 5, 7)).first()
        )
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest
```

Expected: FAIL because `CalendarRules` does not exist.

- [ ] **Step 3: Implement calendar rules**

Create `CalendarRules.kt`:

```kotlin
package com.example.attentioncoach.domain

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

object CalendarRules {
    fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate {
        return LocalDate.now(clock)
    }

    fun weekFor(selectedDate: LocalDate): List<LocalDate> {
        val sunday = selectedDate.minusDays(selectedDate.dayOfWeek.value % 7L)
        return (0L..6L).map { sunday.plusDays(it) }
    }

    fun daysForMonth(yearMonth: YearMonth): List<LocalDate> {
        return (1..yearMonth.lengthOfMonth()).map { day -> yearMonth.atDay(day) }
    }
}
```

In `PlanningRules.kt`, replace `WeekTimeline.weekFor()` implementation with delegation:

```kotlin
object WeekTimeline {
    fun weekFor(selectedDate: LocalDate): List<LocalDate> {
        return CalendarRules.weekFor(selectedDate)
    }
}
```

In `AppShell.kt`, replace:

```kotlin
var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 5, 5)) }
```

with:

```kotlin
var selectedDate by remember { mutableStateOf(CalendarRules.today()) }
```

Add import:

```kotlin
import com.example.attentioncoach.domain.CalendarRules
```

- [ ] **Step 4: Update month grid implementation**

In `TodayScreen.kt`, inside `MonthGrid`, replace manual day loop:

```kotlin
for (day in 1..pickerMonth.lengthOfMonth()) add(pickerMonth.atDay(day))
```

with:

```kotlin
CalendarRules.daysForMonth(pickerMonth).forEach { add(it) }
```

Add import:

```kotlin
import com.example.attentioncoach.domain.CalendarRules
```

- [ ] **Step 5: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/domain/CalendarRules.kt app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt app/src/test/java/com/example/attentioncoach/domain/CalendarRulesTest.kt
git -C 'project\attention_coach_android' commit -m 'feat: sync calendar with system date'
```

### Task 4: Remove Timeline Dots

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`

- [ ] **Step 1: Remove indicator row from `WeekDayCell`**

Delete this block:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Box(Modifier.size(14.dp).clip(CircleShape).background(UiTokens.GoogleBlue.copy(alpha = 0.75f)))
    if (date.dayOfMonth == 5) Box(Modifier.size(14.dp).clip(CircleShape).background(UiTokens.DateAccent))
}
```

Do not replace it with placeholder spacing. The timeline should contain weekday + date only.

- [ ] **Step 2: Remove unused imports**

If `CircleShape` is still needed elsewhere in `TodayScreen.kt`, keep it. If not, remove it.

- [ ] **Step 3: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git -C 'project\attention_coach_android' commit -m 'fix: remove timeline indicators'
```

---

## Chunk 3: One List Completion UX

### Task 5: Add Domain Toggle Completion Rule

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`

- [ ] **Step 1: Write failing tests**

Add to `AppStateTest`:

```kotlin
@Test
fun toggleTaskCompletedMarksPlannedTaskFinished() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.toggleTaskCompleted(1L)

    assertEquals(TaskStatus.FINISHED, store.taskById(1L)?.status)
}

@Test
fun toggleTaskCompletedAgainReturnsTaskToPlanned() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.toggleTaskCompleted(1L)
    store.toggleTaskCompleted(1L)

    val task = store.taskById(1L)
    assertEquals(TaskStatus.PLANNED, task?.status)
    assertEquals(0, task?.actualFocusMinutes)
}

@Test
fun taskListKeepsSingleListWithPausedTasksFirst() {
    val tasks = DemoTaskRepository.seed().filter { it.date == LocalDate.of(2026, 5, 5) }

    val sorted = TaskGrouper.todayList(tasks)

    assertEquals(TaskStatus.PAUSED, sorted.first().status)
    assertEquals(tasks.size, sorted.size)
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest
```

Expected: FAIL because `toggleTaskCompleted()` and `todayList()` do not exist.

- [ ] **Step 3: Implement store toggle**

In `AttentionCoachStore.kt`, add:

```kotlin
fun toggleTaskCompleted(taskId: Long) {
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
        task.copy(status = TaskStatus.FINISHED)
    }
}
```

- [ ] **Step 4: Implement single-list sorting**

In `PlanningRules.kt`, add:

```kotlin
fun todayList(tasks: List<PlannedTask>): List<PlannedTask> {
    return tasks.sortedWith(
        compareByDescending<PlannedTask> { it.status == TaskStatus.PAUSED }
            .thenBy { it.id }
    )
}
```

Keep `group()` temporarily if other tests still use it, but Today UI should stop using groups in the next task.

- [ ] **Step 5: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt
git -C 'project\attention_coach_android' commit -m 'feat: add task completion toggle rules'
```

### Task 6: Replace Completed Section With Strike-Through And Circle Toggle

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`

- [ ] **Step 1: Change `TodayScreen` API**

Update signature:

```kotlin
fun TodayScreen(
    selectedDate: LocalDate,
    tasks: List<PlannedTask>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskSelected: (Long) -> Unit,
    onToggleTaskCompleted: (Long) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Stop using open/completed groups**

Replace:

```kotlin
val groups = remember(tasks) { TaskGrouper.group(tasks) }
```

with:

```kotlin
val visibleTasks = remember(tasks) { TaskGrouper.todayList(tasks) }
```

Render one list:

```kotlin
items(visibleTasks, key = { it.id }) { task ->
    TaskCard(
        task = task,
        onClick = { onTaskSelected(task.id) },
        onToggleCompleted = { onToggleTaskCompleted(task.id) }
    )
}
```

Delete the `HorizontalDivider` section entirely.

- [ ] **Step 3: Update `TaskCard`**

Change signature:

```kotlin
private fun TaskCard(
    task: PlannedTask,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit
)
```

Use a completion state helper:

```kotlin
private fun PlannedTask.isDoneForList(): Boolean {
    return status == TaskStatus.FINISHED || status == TaskStatus.REVIEWED
}
```

Title style:

```kotlin
Text(
    task.title,
    color = if (task.isDoneForList()) UiTokens.InkSoft else UiTokens.Ink,
    textDecoration = if (task.isDoneForList()) TextDecoration.LineThrough else TextDecoration.None,
    fontSize = 17.sp,
    lineHeight = 21.sp,
    fontWeight = FontWeight.Bold,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

Add imports:

```kotlin
import androidx.compose.ui.text.style.TextDecoration
import com.example.attentioncoach.domain.TaskStatus
```

Replace right chevron with a circle:

```kotlin
Box(
    modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .border(2.dp, if (task.isDoneForList()) UiTokens.GoogleGreen else UiTokens.Outline, CircleShape)
        .background(if (task.isDoneForList()) UiTokens.LowChipBg else androidx.compose.ui.graphics.Color.Transparent)
        .clickable(onClick = onToggleCompleted),
    contentAlignment = Alignment.Center
) {
    if (task.isDoneForList()) {
        Text("✓", color = UiTokens.GoogleGreen, fontWeight = FontWeight.Bold)
    }
}
```

Important: only the circle toggles completion; the rest of the card opens detail.

- [ ] **Step 4: Wire in `AppShell`**

In `TopLevelScreen`, add `onToggleTaskCompleted`.

In `AttentionCoachApp`, implement:

```kotlin
onToggleTaskCompleted = { taskId ->
    tasks = tasks.map { task ->
        if (task.id != taskId) {
            task
        } else if (task.status == TaskStatus.FINISHED || task.status == TaskStatus.REVIEWED) {
            task.copy(
                status = TaskStatus.PLANNED,
                actualFocusMinutes = 0,
                actualCompletion = "",
                mismatchReason = "",
                nextAdjustment = ""
            )
        } else {
            task.copy(status = TaskStatus.FINISHED)
        }
    }
}
```

- [ ] **Step 5: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git -C 'project\attention_coach_android' commit -m 'feat: toggle completion from task list'
```

---

## Chunk 4: Schedule-Aware Plan Page

### Task 7: Add Start Time And Schedule Options

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
- Create: `app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt`
- Create: `app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`

- [ ] **Step 1: Write failing schedule tests**

Create `ScheduleOptionsTest.kt`:

```kotlin
package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ScheduleOptionsTest {
    @Test
    fun startTimeOptionsUseFiveMinuteIntervals() {
        val options = ScheduleOptions.startTimes()

        assertEquals(LocalTime.of(0, 0), options.first())
        assertEquals(LocalTime.of(23, 55), options.last())
        assertTrue(options.contains(LocalTime.of(7, 50)))
        assertEquals(288, options.size)
    }

    @Test
    fun durationOptionsMatchPlanningSheetChoices() {
        assertEquals(listOf(1, 15, 30, 45, 60, 90), ScheduleOptions.durationMinutes)
    }
}
```

Add to `AppStateTest`:

```kotlin
@Test
fun updateScheduleStoresStartTimeAndDuration() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())

    store.updateSchedule(1L, LocalTime.of(7, 50), 45)

    val task = store.taskById(1L)
    assertEquals(LocalTime.of(7, 50), task?.startTime)
    assertEquals(45, task?.durationMinutes)
}
```

Add import:

```kotlin
import java.time.LocalTime
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest
```

Expected: FAIL because `startTime`, `ScheduleOptions`, and `updateSchedule()` do not exist.

- [ ] **Step 3: Update model**

In `Models.kt`, add:

```kotlin
import java.time.LocalTime
```

Add field:

```kotlin
val startTime: LocalTime? = null,
```

Place it after `date` or before `durationMinutes` so schedule fields stay together.

- [ ] **Step 4: Implement schedule options**

Create `ScheduleOptions.kt`:

```kotlin
package com.example.attentioncoach.domain

import java.time.LocalTime

object ScheduleOptions {
    val durationMinutes: List<Int> = listOf(1, 15, 30, 45, 60, 90)

    fun startTimes(): List<LocalTime> {
        return (0 until 24 * 60 step 5).map { minuteOfDay ->
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        }
    }
}
```

- [ ] **Step 5: Update store**

In `AttentionCoachStore.kt`, add optional `startTime` to `createTask()`:

```kotlin
startTime: LocalTime? = null,
```

and pass it into `PlannedTask`.

Add:

```kotlin
fun updateSchedule(taskId: Long, startTime: LocalTime?, durationMinutes: Int) {
    val task = tasksById[taskId] ?: return
    tasksById[taskId] = task.copy(
        startTime = startTime,
        durationMinutes = durationMinutes.coerceAtLeast(1)
    )
}
```

Add import:

```kotlin
import java.time.LocalTime
```

- [ ] **Step 6: Update demo seeds**

In `DemoTaskRepository.kt`, add import:

```kotlin
import java.time.LocalTime
```

Set sample `startTime` values, for example:

```kotlin
startTime = LocalTime.of(7, 50),
```

for one or two tasks. It is fine for some tasks to remain unscheduled.

- [ ] **Step 7: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/domain/Models.kt app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt
git -C 'project\attention_coach_android' commit -m 'feat: add task start time schedule model'
```

### Task 8: Rebuild Plan Page Layout And Schedule Editor

**Files:**
- Create: `app/src/main/java/com/example/attentioncoach/ui/ScheduleEditorDialog.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/Formatting.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`

- [ ] **Step 1: Add time formatting helpers**

In `Formatting.kt`, add:

```kotlin
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.shortTime(): String {
    return format(timeFormatter)
}
```

- [ ] **Step 2: Create schedule editor dialog**

Create `ScheduleEditorDialog.kt`:

```kotlin
package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.attentioncoach.domain.ScheduleOptions
import java.time.LocalTime

@Composable
fun ScheduleEditorDialog(
    initialStartTime: LocalTime?,
    initialDurationMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (LocalTime?, Int) -> Unit
) {
    var startTime by remember(initialStartTime) { mutableStateOf(initialStartTime) }
    var duration by remember(initialDurationMinutes) { mutableStateOf(initialDurationMinutes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Schedule", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("X", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Text("Start time", color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(UiTokens.Page)
                ) {
                    item {
                        ScheduleOption(
                            text = "No fixed time",
                            selected = startTime == null,
                            onClick = { startTime = null }
                        )
                    }
                    items(ScheduleOptions.startTimes()) { option ->
                        ScheduleOption(
                            text = option.shortTime(),
                            selected = option == startTime,
                            onClick = { startTime = option }
                        )
                    }
                }
                Text("Duration", color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ScheduleOptions.durationMinutes.forEach { minutes ->
                        ScheduleOptionChip(
                            text = if (minutes < 60) "${minutes}m" else "${minutes / 60}${if (minutes % 60 == 0) "h" else ".5h"}",
                            selected = minutes == duration,
                            onClick = { duration = minutes },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onSave(startTime, duration) },
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleBlue),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Save schedule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScheduleOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) androidx.compose.ui.graphics.Color.White else UiTokens.Ink,
        fontSize = if (selected) 22.sp else 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) UiTokens.GoogleGreen else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    )
}

@Composable
private fun ScheduleOptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) UiTokens.LowChipBg else UiTokens.Page)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text, color = if (selected) UiTokens.GoogleGreen else UiTokens.InkSoft, fontWeight = FontWeight.Bold)
    }
}
```

Note: If six duration chips do not fit cleanly on narrow screens, change the duration chip row to a `LazyRow` in implementation.

- [ ] **Step 3: Reorder `PlanPage`**

In `TaskDetailSheet.kt`:

- Remove `var note`.
- Remove `FieldLabel("Planning note"... )` and its `OutlinedTextField`.
- Add `var startTime by remember(task.id) { mutableStateOf(task.startTime) }`.
- Add `var showScheduleEditor by remember { mutableStateOf(false) }`.
- Render fields in this order:
  - Duration schedule control;
  - Priority dropdown;
  - Targets large input.

Schedule control:

```kotlin
FieldLabel("Duration", UiTokens.LowChipText)
Box(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(androidx.compose.ui.graphics.Color.White)
        .clickable { showScheduleEditor = true }
        .padding(18.dp)
) {
    Column {
        Text(startTime?.shortTime() ?: "No fixed time", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${duration.toIntOrNull() ?: task.durationMinutes} min", color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
    }
}
```

Targets field:

```kotlin
FieldLabel("Targets", UiTokens.GoogleBlue)
OutlinedTextField(
    value = target,
    onValueChange = { target = it },
    minLines = 6,
    modifier = Modifier.fillMaxWidth()
)
```

On save:

```kotlin
val updated = task.copy(
    title = if (isCreateMode) title.trim().ifBlank { "Untitled task" } else task.title,
    startTime = startTime,
    target = target,
    durationMinutes = duration.toIntOrNull()?.coerceAtLeast(1) ?: task.durationMinutes,
    priority = priority,
    status = if (isCreateMode) TaskStatus.PLANNED else task.status
)
```

If schedule editor is open:

```kotlin
if (showScheduleEditor) {
    ScheduleEditorDialog(
        initialStartTime = startTime,
        initialDurationMinutes = duration.toIntOrNull() ?: task.durationMinutes,
        onDismiss = { showScheduleEditor = false },
        onSave = { selectedStartTime, selectedDuration ->
            startTime = selectedStartTime
            duration = selectedDuration.toString()
            showScheduleEditor = false
        }
    )
}
```

- [ ] **Step 4: Update AppShell draft task**

When creating `draftTask`, do not pass `planningNote`. If model keeps `planningNote`, leave `planningNote = ""` only until model cleanup.

- [ ] **Step 5: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/ui/ScheduleEditorDialog.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/ui/Formatting.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt
git -C 'project\attention_coach_android' commit -m 'feat: add schedule editor to plan sheet'
```

---

## Chunk 5: Alarm Permission And Reminder Hook

### Task 9: Add Exact Alarm Permission Helper

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt`

- [ ] **Step 1: Update manifest**

Add:

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

Keep existing notification and foreground-service permissions.

- [ ] **Step 2: Add helper**

Create `AlarmPermissionHelper.kt`:

```kotlin
package com.example.attentioncoach.platform

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object AlarmPermissionHelper {
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}
```

- [ ] **Step 3: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/AndroidManifest.xml app/src/main/java/com/example/attentioncoach/platform/AlarmPermissionHelper.kt
git -C 'project\attention_coach_android' commit -m 'feat: add exact alarm permission helper'
```

### Task 10: Request Alarm Permission When Scheduling A Task

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Optional create: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`

- [ ] **Step 1: Wire permission request**

In `AppShell.kt`, add imports:

```kotlin
import androidx.core.content.ContextCompat
import com.example.attentioncoach.platform.AlarmPermissionHelper
```

If `ContextCompat` is unavailable, use `context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))` directly and add `android.content.Intent` import.

Add helper near bottom of `AppShell.kt`:

```kotlin
private fun requestAlarmPermissionIfNeeded(context: android.content.Context, task: PlannedTask) {
    if (task.startTime == null) return
    if (AlarmPermissionHelper.canScheduleExactAlarms(context)) return
    context.startActivity(
        AlarmPermissionHelper.requestIntent(context).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
```

Call it after saving or creating a task:

```kotlin
onSavePlan = { updated ->
    tasks = tasks.replaceTask(updated)
    requestAlarmPermissionIfNeeded(context, updated)
}
```

and:

```kotlin
onCreateTask = { created ->
    tasks = tasks + created
    requestAlarmPermissionIfNeeded(context, created)
    ...
}
```

- [ ] **Step 2: Optional scheduling no-op**

If time permits in this same task, create `TaskReminderScheduler.kt` with a safe no-op interface now and real receiver later:

```kotlin
package com.example.attentioncoach.platform

import android.content.Context
import com.example.attentioncoach.domain.PlannedTask

object TaskReminderScheduler {
    fun scheduleIfAllowed(context: Context, task: PlannedTask) {
        if (task.startTime == null) return
        if (!AlarmPermissionHelper.canScheduleExactAlarms(context)) return
        // Full AlarmManager receiver can be implemented after permission flow is smoke-tested.
    }
}
```

Call it after permission check only if added.

- [ ] **Step 3: Run verification**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git -C 'project\attention_coach_android' add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt
git -C 'project\attention_coach_android' commit -m 'feat: request alarm access for scheduled tasks'
```

---

## Chunk 6: Final Verification And Smoke Notes

### Task 11: Emulator Smoke And Notes

**Files:**
- Modify: `docs/active_monitoring_smoke_test.md`

- [ ] **Step 1: Build final APK**

Run:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on emulator**

Run with the available emulator id:

```powershell
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 install -r 'project\attention_coach_android\app\build\outputs\apk\debug\app-debug.apk'
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 shell am start -n com.example.attentioncoach/.MainActivity
```

Expected: install `Success`, app launches.

- [ ] **Step 3: Smoke checklist**

Verify manually/emulator:

- App opens on system today.
- Week row has no blue/pink dots.
- May 7, 2026 displays under Thursday when navigating to that date.
- Add creates a task.
- Task list has no divider section.
- Completion circle toggles finished/planned.
- Finished title has a clear strike-through.
- Card body still opens detail.
- Plan page has no planning note.
- Plan page order is Duration, Priority, Targets.
- Duration opens schedule editor.
- Start-time list scrolls and includes `07:50`.
- Duration options include `1`, `15`, `30`, `45`, `60`, `90`.
- Saving a start time triggers the exact alarm permission screen if permission is not already granted.
- Review reason is blank for newly finished tasks until user types.
- Finish and Exit each require only one confirmation.

- [ ] **Step 4: Record smoke notes**

Append to `docs/active_monitoring_smoke_test.md`:

```markdown
## 2026-05-06 Schedule & Completion Smoke Addendum

Build under test: debug APK after schedule/completion commits.

Verified:

- ...

Limitations:

- ...
```

- [ ] **Step 5: Final verification**

Run again:

```powershell
& 'project\attention_coach_android\gradlew.bat' -p 'project\attention_coach_android' testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git -C 'project\attention_coach_android' add docs/active_monitoring_smoke_test.md
git -C 'project\attention_coach_android' commit -m 'test: record schedule completion smoke coverage'
```

---

## Notes And Assumptions

- User asked for no repeated permission questions. Within this environment, tool-level escalation may still be required by the harness for Gradle/ADB. Use existing approved prefixes where possible; if a required command is blocked by sandboxing, request escalation with the exact command because the harness requires it.
- Do not commit `docs/emulator_screenshots/` unless explicitly converting it into curated acceptance artifacts.
- The completion circle should be independent from card-body navigation.
- Uncompleting a task resets it to `PLANNED` and clears actual/review fields to avoid stale review state.
- Exact alarm access is not a normal runtime permission. On Android 12+, the app must send the user to system settings via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- If full AlarmManager receiver implementation grows too large, land the permission request first and keep scheduling no-op as a separate follow-up. Do not fake a reminder.
- Use `docs/prototype_reference_workflow.md` for UI changes: read `project/ui_prototype/index.html`, `styles.css`, and `app.js` sections before visual edits, then compare against frozen snapshots where relevant.
