# Set Plan Schedule UI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix task schedule editing persistence and redesign the Plan page schedule controls so they match the user's `internal_proposal/set_plan` references and the frozen Plan prototype style.

**Architecture:** Keep schedule state owned by `PlannedTask` and update it through the existing detail-sheet save path; the schedule editor should edit local draft state and persist immediately for existing tasks when `Save schedule` is tapped. UI work must follow `docs/prototype_reference_workflow.md`: read prototype source first, map to Compose, then verify against screenshots and emulator behavior.

**Tech Stack:** Kotlin, Jetpack Compose, Java Time (`LocalTime`), Android Toast/Snackbar/Dialog feedback, Gradle unit tests, emulator screenshot/manual smoke verification, git.

---

## Source Inputs

- User notes: `D:\Desktop\HKUST\25-26spring\comp4521\project\internal_proposal\set_plan\what_i_want.md`
- Current native UI screenshot: `D:\Desktop\HKUST\25-26spring\comp4521\project\internal_proposal\set_plan\planing_page_now.jpg`
- Schedule wheel references:
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\internal_proposal\set_plan\double_scrollable_list.jpg`
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\internal_proposal\set_plan\curent_time_highlight.jpg`
- Plan style reference:
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\internal_proposal\set_plan\reference_ui.png`
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\app_design_freeze\snapshots\plan_default.png`
- Prototype workflow: `D:\Desktop\HKUST\25-26spring\comp4521\project\attention_coach_android\docs\prototype_reference_workflow.md`
- Prototype source to inspect before UI work:
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\ui_prototype\index.html`
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\ui_prototype\styles.css`
  - `D:\Desktop\HKUST\25-26spring\comp4521\project\ui_prototype\app.js`

## User Intent Summary

1. After creating a task, reopening it and changing `startTime` / `duration` through `Save schedule` must persist the new values. Reopening the same task should show the updated schedule, not the original start time.
2. Tapping `Save schedule` should show a floating feedback message with exactly: `Schedule is saved`.
3. The schedule time selector should be a two-column wheel:
   - left column: hour `0..23`;
   - right column: minute values in 5-minute steps: `00, 05, ... 55`.
4. When reopening the schedule editor, the wheel should start near the last saved hour/minute and highlight that value, instead of starting at `12:00 AM`.
5. Move the start-time wheel area upward and reserve more space for duration.
6. Duration should be a non-scrollable `2 x 3` grid:
   - preset buttons: `15 min`, `30 min`, `45 min`, `60 min`, `90 min`;
   - one custom numeric input with fixed `min` unit.
7. The Plan page schedule summary currently clips. Replace it with a horizontal layout:
   - current duration on the left;
   - current start time on the right.
8. The Plan page visual style should align with `plan_default.png` / `reference_ui.png`, but the current layout order must remain:
   - top row: Duration + Priority;
   - below: Targets large input.

## File Structure

Modify:

- `app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt`
  - Keep preset duration values.
  - Add hour and minute option helpers for the two-column wheel.

- `app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt`
  - Cover hour options, minute options, and duration presets including custom input boundaries if helper logic is added.

- `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt`
  - If store tests are used for persistence, ensure `updatePlan()` can update `startTime` and `durationMinutes` after task creation.

- `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
  - Add regression coverage that updates a created task's schedule and verifies reopening-by-id returns the updated values.

- `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
  - Persist schedule immediately for existing tasks when the schedule editor saves.
  - Show `Schedule is saved` feedback.
  - Replace the vertical schedule summary card with a horizontal duration/start-time status layout.
  - Align Plan page field styling with prototype references while preserving Duration/Priority above Targets.

- `app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
  - Replace the single list of times with two wheel columns.
  - Add highlighted selected row.
  - Set initial scroll position from the task's saved `startTime`.
  - Replace scrollable duration row with fixed `2 x 3` duration controls.
  - Add custom duration input with fixed `min` suffix.

- `app/src/main/java/com/example/attentioncoach/ui/Formatting.kt`
  - Add small formatting helpers if needed, e.g. `twoDigitMinuteLabel()` or `hour24Label()`.

- `app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt`
  - Only modify if prototype-alignment colors/spacing require reusable tokens. Avoid one-off color invention.

Create:

- `docs/set_plan_schedule_ui_smoke_test.md`
  - Record emulator/manual verification results and any remaining visual differences.

Do not modify:

- `docs/emulator_screenshots/` unless a later screenshot archival task explicitly chooses to track it.
- Reminder/Alarm platform code, unless schedule persistence changes reveal a direct scheduling bug.

## Chunk 1: Persist Schedule Edits After Task Creation

### Task 1: Schedule Persistence Regression

**Files:**
- Modify: `app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt` only if needed
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`

- [ ] **Step 1: Write the failing persistence test**

Add this test to `AppStateTest.kt`:

```kotlin
@Test
fun createdTaskScheduleCanBeEditedAfterCreation() {
    val store = AttentionCoachStore(DemoTaskRepository.seed())
    val created = store.createTask(
        date = LocalDate.of(2026, 5, 10),
        title = "Editable schedule",
        startTime = LocalTime.of(9, 0),
        durationMinutes = 30
    )

    store.updatePlan(
        taskId = created.id,
        target = created.target,
        startTime = LocalTime.of(17, 5),
        durationMinutes = 45,
        priority = created.priority
    )

    val reopened = store.taskById(created.id)
    assertEquals(LocalTime.of(17, 5), reopened?.startTime)
    assertEquals(45, reopened?.durationMinutes)
}
```

- [ ] **Step 2: Run focused test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.attentioncoach.domain.AppStateTest.createdTaskScheduleCanBeEditedAfterCreation"
```

Expected:
- PASS if domain already supports this; then the bug is UI persistence only.
- FAIL if `updatePlan()` does not handle schedule edits; fix domain before UI.

- [ ] **Step 3: Persist schedule on `Save schedule` for existing tasks**

In `TaskDetailSheet.kt`, update the `TaskScheduleEditor(onSave = ...)` path:

```kotlin
onSave = { selectedStartTime, selectedDuration ->
    startTime = selectedStartTime
    duration = selectedDuration
    val updated = task.copy(
        target = target,
        startTime = selectedStartTime,
        durationMinutes = selectedDuration,
        priority = priority,
        status = if (isCreateMode) TaskStatus.PLANNED else task.status
    )
    if (!isCreateMode) {
        onSavePlan(updated)
        showScheduleSavedFeedback = true
    }
    showScheduleEditor = false
}
```

For create mode, only update local draft state; final persistence still happens through `Save task`.

- [ ] **Step 4: Add floating feedback**

Use either Compose `AlertDialog`, `SnackbarHost`, or Android `Toast`. Prefer the least invasive implementation:

```kotlin
Toast.makeText(context, "Schedule is saved", Toast.LENGTH_SHORT).show()
```

If using Toast, add `val context = LocalContext.current` in `PlanPage`.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/test/java/com/example/attentioncoach/domain/AppStateTest.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/domain/AttentionCoachStore.kt
git commit -m "fix: persist task schedule edits"
```

Omit any file that was not changed.

## Chunk 2: Two-Column Start-Time Wheel

### Task 2: Hour And Minute Options

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt`

- [ ] **Step 1: Add failing option tests**

Add:

```kotlin
@Test
fun hourOptionsCoverZeroThroughTwentyThree() {
    assertEquals((0..23).toList(), ScheduleOptions.hours)
}

@Test
fun minuteOptionsUseFiveMinuteSteps() {
    assertEquals(listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55), ScheduleOptions.minutes)
}
```

- [ ] **Step 2: Run focused tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.attentioncoach.domain.ScheduleOptionsTest"
```

Expected: FAIL until `hours` and `minutes` are implemented.

- [ ] **Step 3: Implement minimal helpers**

In `ScheduleOptions.kt`:

```kotlin
val hours: List<Int> = (0..23).toList()
val minutes: List<Int> = (0..55 step 5).toList()
```

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt
git commit -m "feat: add schedule wheel options"
```

### Task 3: Replace Single Time List With Two Wheel Columns

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/Formatting.kt` if formatting helpers are needed

- [ ] **Step 1: Read references before editing**

Open and visually inspect:

- `internal_proposal/set_plan/double_scrollable_list.jpg`
- `internal_proposal/set_plan/curent_time_highlight.jpg`

Implementation target:

- two scrollable columns;
- selected row has a wide green highlight;
- non-selected rows fade/dim;
- editor reopens near the previously saved value.

- [ ] **Step 2: Replace `LazyColumn` time list**

In `TaskScheduleEditor.kt`, remove the single `LazyColumn` that renders `ScheduleOptions.startTimes()`.

Add:

```kotlin
val initialTime = initialStartTime ?: LocalTime.now().withMinute((LocalTime.now().minute / 5) * 5)
var selectedHour by remember(initialStartTime) { mutableStateOf(initialTime.hour) }
var selectedMinute by remember(initialStartTime) { mutableStateOf((initialTime.minute / 5) * 5) }
val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedHour - 2).coerceAtLeast(0))
val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = ((selectedMinute / 5) - 2).coerceAtLeast(0))
```

- [ ] **Step 3: Add `WheelColumn` composable**

Create a private composable in `TaskScheduleEditor.kt`:

```kotlin
@Composable
private fun WheelColumn(
    values: List<Int>,
    selectedValue: Int,
    label: (Int) -> String,
    state: LazyListState,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier.height(220.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(values) { value ->
            val selected = value == selectedValue
            Text(
                text = label(value),
                color = if (selected) Color.White else UiTokens.InkSoft.copy(alpha = 0.55f),
                fontSize = if (selected) 26.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) UiTokens.GoogleGreen else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
```

Adjust imports for `LazyListState`, `rememberLazyListState`, `TextAlign`, and `Color`.

- [ ] **Step 4: Save selected hour/minute**

When the user taps `Save schedule`, call:

```kotlin
onSave(LocalTime.of(selectedHour, selectedMinute), selectedDuration)
```

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt app/src/main/java/com/example/attentioncoach/ui/Formatting.kt
git commit -m "feat: use two-column schedule wheel"
```

Omit `Formatting.kt` if unchanged.

## Chunk 3: Duration Grid And Custom Minutes

### Task 4: Fixed 2 x 3 Duration Controls

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`

- [ ] **Step 1: Replace scrollable `LazyRow` duration selector**

Remove the current horizontally scrollable duration row.

Add a fixed grid:

```kotlin
val presetDurations = ScheduleOptions.durationMinutes
var customDurationText by remember(initialDurationMinutes) {
    mutableStateOf(if (initialDurationMinutes in presetDurations) "" else initialDurationMinutes.toString())
}
```

Render two rows of three cells:

- row 1: `15`, `30`, `45`;
- row 2: `60`, `90`, custom input.

- [ ] **Step 2: Add custom duration input**

Use an `OutlinedTextField` with:

- digits only;
- fixed suffix `min`;
- selecting custom value updates `selectedDuration`.

Example:

```kotlin
OutlinedTextField(
    value = customDurationText,
    onValueChange = { value ->
        val digits = value.filter(Char::isDigit).take(3)
        customDurationText = digits
        digits.toIntOrNull()?.takeIf { it > 0 }?.let { selectedDuration = it }
    },
    suffix = { Text("min") },
    singleLine = true
)
```

- [ ] **Step 3: Make duration area non-scrollable**

Use `Column` + `Row`, not `LazyRow`. Confirm the six cells fit without horizontal scrolling on the emulator width used by the project.

- [ ] **Step 4: Move time wheel upward**

Reduce top padding above the wheel and keep the wheel height around `210..230.dp`. The duration section should have enough space that the two rows and save button remain visible without awkward clipping.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt
git commit -m "feat: add fixed duration grid"
```

## Chunk 4: Plan Page Schedule Summary

### Task 5: Horizontal Duration / Start-Time Status Layout

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/Formatting.kt` if needed

- [ ] **Step 1: Locate `ScheduleField`**

In `TaskDetailSheet.kt`, find the current schedule display composable. It currently stacks start time and duration vertically, causing clipping in the Plan page.

- [ ] **Step 2: Replace with horizontal layout**

Target layout:

- one white rounded card;
- left side: `DURATION` label + current duration, e.g. `45 min`;
- right side: `START TIME` label + current start time, e.g. `5:05 PM`;
- values should not clip.

Example:

```kotlin
Row(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
) {
    ScheduleSummaryValue(label = "DURATION", value = "$durationMinutes min", modifier = Modifier.weight(1f))
    ScheduleSummaryValue(label = "START TIME", value = startTime?.shortTimeLabel() ?: "Not set", modifier = Modifier.weight(1f))
}
```

- [ ] **Step 3: Preserve click behavior**

Tapping anywhere in the schedule card still opens `TaskScheduleEditor`.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/ui/Formatting.kt
git commit -m "fix: show schedule summary horizontally"
```

Omit `Formatting.kt` if unchanged.

## Chunk 5: Plan Page Prototype Alignment

### Task 6: Align Plan Page Visual Style

**Files:**
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt` only if reusable tokens are needed
- Inspect only: `project/ui_prototype/index.html`
- Inspect only: `project/ui_prototype/styles.css`
- Inspect only: `project/ui_prototype/app.js`

- [ ] **Step 1: Follow prototype reference workflow**

Read these before editing:

```powershell
Get-Content ..\ui_prototype\index.html
Get-Content ..\ui_prototype\styles.css
Get-Content ..\ui_prototype\app.js
```

Relevant CSS/JS areas:

- `segmented-control`
- `field`
- `duration-control`
- `priority-menu`
- Plan page/sheet layout selectors
- Plan/Review tab interaction code

- [ ] **Step 2: Compare against visual references**

Open:

- `../app_design_freeze/snapshots/plan_default.png`
- `../internal_proposal/set_plan/reference_ui.png`
- `../internal_proposal/set_plan/planing_page_now.jpg`

Record the specific differences before editing:

- current native typography is too large/heavy in field controls;
- current text-field borders are too dark compared with prototype;
- current page has too much empty lower whitespace but the top controls are visually cramped;
- Plan page should feel like the frozen prototype, while still keeping Duration/Priority above Targets.

- [ ] **Step 3: Tune Plan page surfaces**

Adjust only Plan page controls, not Review page unless shared code forces it:

- rounded field/cards closer to prototype radius;
- softer border color;
- white input surfaces on pale page background;
- label colors and dot labels matching existing prototype palette;
- consistent field heights for Duration and Priority;
- large Targets input under top row.

- [ ] **Step 4: Keep layout order unchanged**

The final Plan page must be:

1. Optional Title field in create mode.
2. Row: Duration + Priority.
3. Targets large input.
4. Primary action button.

Do not move Targets above Duration/Priority even though `plan_default.png` originally had Targets first.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt
git commit -m "style: align plan page schedule form"
```

Omit `UiTokens.kt` if unchanged.

## Chunk 6: Emulator Verification And Notes

### Task 7: Smoke Test Set Plan Flow

**Files:**
- Create: `docs/set_plan_schedule_ui_smoke_test.md`

- [ ] **Step 1: Run build verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on emulator if available**

Use the existing Android SDK/AVD workflow. At minimum:

```powershell
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: emulator is listed and APK install succeeds.

- [ ] **Step 3: Manual smoke checklist**

Verify on device/emulator:

- Create a task.
- Reopen the task.
- Open schedule editor.
- Change start time and duration.
- Tap `Save schedule`.
- Confirm floating feedback says `Schedule is saved`.
- Exit the task.
- Reopen the same task.
- Confirm the updated start time and duration are still shown.
- Open schedule editor again.
- Confirm hour/minute wheels are positioned near the last saved values.
- Confirm selected hour/minute are highlighted.
- Confirm duration controls are a fixed `2 x 3` area, not a horizontal scroll list.
- Confirm custom duration input accepts digits and keeps fixed `min` unit.
- Confirm Plan page schedule summary shows duration left and start time right without clipping.
- Confirm Plan page style is closer to `plan_default.png` / `reference_ui.png`.

- [ ] **Step 4: Record results**

Create `docs/set_plan_schedule_ui_smoke_test.md` with:

- build commit hash;
- emulator/device used;
- checked items;
- screenshots captured, if any;
- remaining known differences.

- [ ] **Step 5: Final verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add docs/set_plan_schedule_ui_smoke_test.md
git commit -m "test: record set plan schedule ui smoke coverage"
```

## Acceptance Criteria

- Existing tasks can update `startTime` and `durationMinutes` after creation.
- `Save schedule` persists changes immediately for existing tasks.
- `Save schedule` shows floating feedback: `Schedule is saved`.
- Reopening a task shows the last saved start time and duration.
- Reopening the schedule editor scrolls/highlights to the last saved hour/minute.
- Start-time editor uses two scrollable columns: hours `0..23`, minutes in 5-minute steps.
- Duration controls are a fixed `2 x 3` layout with five presets plus custom numeric input.
- Duration custom input displays a fixed `min` unit and accepts digits only.
- Plan page schedule summary is horizontal: duration left, start time right.
- No schedule summary text clips on the target emulator viewport.
- Plan page visually aligns with the frozen/prototype style while preserving Duration/Priority above Targets.
- `testDebugUnitTest` and `assembleDebug` pass before every commit.
