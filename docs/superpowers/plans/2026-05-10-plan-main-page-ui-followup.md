# Plan And Main Page UI Follow-Up Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the current Android main page, plan page, and schedule editor into alignment with the latest `internal_proposal/set_plan` and `internal_proposal/main_page` requirements, then verify the behavior directly on an emulator.

**Architecture:** Keep the existing Compose structure and state model. Add small domain helpers only where UI behavior needs testable rules, then update the relevant Compose screens surgically: `TodayScreen`, `TaskDetailSheet`, `TaskScheduleEditor`, `AppShell`, and shared UI tokens/resources. The latest `internal_proposal/main_page/draft_plan.md` overrides older prototype indicator behavior: date indicators become one blue unfinished-task dot, not the old blue/pink schedule/review pair.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Java Time, JUnit4, Android emulator/ADB.

---

## Context Sources

- Latest set-plan requirements: `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/set_plan/what_i_want.md`
- Current set-plan screenshots:
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/set_plan/plan_page.jpg`
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/set_plan/schedule_editing.jpg`
- Set-plan visual reference:
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/set_plan/reference_ui.png`
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/docs/design_references/set_plan_schedule_ui/generated_plan_page_reference_source.png`
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/docs/design_references/set_plan_schedule_ui/generated_schedule_editor_reference.png`
- Latest main-page requirements: `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/main_page/draft_plan.md`
- Current main-page screenshots:
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/main_page/main_page_now.jpg`
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/main_page/calendar_page_now.jpg`
- Main-page visual reference:
  - `D:/Desktop/HKUST/25-26spring/comp4521/project/internal_proposal/main_page/reference.png`
- Required UI workflow: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/docs/prototype_reference_workflow.md`

## Current Interpretation

### Set Plan

- Start time and duration must remain editable after a task is created.
- Saving schedule should persist immediately and show a visible floating message: `Schedule is saved`.
- The schedule editor should use two vertical wheels: hour `0..23` and minute `0,5,...55`.
- The selected wheel value should stay visually centered. Users should change the selected value by scrolling, not only by tapping values.
- Reopening the schedule editor should position the wheel at the saved time and show the selected hour/minute highlighted in the center.
- The start-time wheel should move upward slightly so the duration section has more room.
- Duration options should be a fixed, non-draggable 2x3 grid:
  - `15 min`
  - `30 min`
  - `45 min`
  - `60 min`
  - `90 min`
  - custom numeric input with fixed `min` unit
- The plan page schedule summary must stay horizontal: duration on the left, start time on the right.
- Plan page keeps the layout order: duration + priority on top, targets below.
- Back, close, chevron, more, and save symbols should use a consistent Google/Material-style icon treatment.

### Main Page

- Bottom navigation should look closer to Google app Material style, not raw text letters.
- `PLANNED FOCUS` and `REVIEWED` summary cards must show full label/value text without clipping.
- Remove the timeline hint text: `Swipe the week row to move between weeks`.
- Add clickable left/right week arrows near the week row while keeping horizontal swipe navigation.
- If a date has at least one unfinished task, show one blue dot under that date in both week timeline and month calendar.
- If all tasks on that date are completed/reviewed, hide the blue dot.
- Do not use the older two-dot pink/blue reminder/review indicator logic.
- Audit arrows/close/more symbols across main page, time line, calendar, and edit pages for consistent Material-style treatment.

## Pre-Execution Requirement

Before touching implementation code, start the Pixel_7 emulator and install the current APK so every UI change can be checked interactively.

- [ ] Run ADB check:

```powershell
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
```

Expected: either no emulator yet, or an existing device. Current observed device was `adb-R5CW61ZKR5W-jk0FxM._adb-tls-connect._tcp`.

- [ ] Start Pixel_7 AVD if no emulator is running:

```powershell
& 'C:\Users\11345\AppData\Local\Android\Sdk\emulator\emulator.exe' -avd Pixel_7
```

Expected: `adb devices` eventually shows an `emulator-####` device.

- [ ] Build and install baseline:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: Gradle build succeeds and APK installs.

- [ ] Capture baseline screenshots for current main page, plan page, and schedule editor into `docs/emulator_screenshots/`.

## Chunk 1: Testable Date Indicator Rules

**Files:**
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`

### Task 1: Add a small domain rule for unfinished-date dots

- [ ] **Step 1: Write failing tests**

Add tests:

```kotlin
@Test
fun dateIndicatorShowsBlueDotWhenAnyTaskIsUnfinished() {
    val tasks = listOf(
        task(id = 1, status = TaskStatus.FINISHED),
        task(id = 2, status = TaskStatus.PLANNED)
    )

    assertTrue(DateIndicatorRules.hasUnfinishedTaskDot(tasks))
}

@Test
fun dateIndicatorHidesDotWhenAllTasksAreComplete() {
    val tasks = listOf(
        task(id = 1, status = TaskStatus.FINISHED),
        task(id = 2, status = TaskStatus.REVIEWED)
    )

    assertFalse(DateIndicatorRules.hasUnfinishedTaskDot(tasks))
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: failure because `DateIndicatorRules` does not exist.

- [ ] **Step 3: Implement minimal rule**

Add:

```kotlin
object DateIndicatorRules {
    fun hasUnfinishedTaskDot(tasks: List<PlannedTask>): Boolean {
        return tasks.any { it.status != TaskStatus.FINISHED && it.status != TaskStatus.REVIEWED }
    }
}
```

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt
git commit -m "test: cover unfinished date indicators"
```

## Chunk 2: Main Page Timeline, Calendar Dots, Summary Cards, Bottom Nav

**Files:**
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt`
- Create if needed: vector drawables under `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/res/drawable/`

### Task 2: Add week navigation arrows and remove the hint

- [ ] **Step 1: Read required prototype/internal sources**

Read:

```powershell
rg -n "week-planner|week-row|week-hint|date-title-button" ..\ui_prototype\styles.css
rg -n "renderWeek|weekRow|pointer" ..\ui_prototype\app.js
Get-Content ..\internal_proposal\main_page\draft_plan.md -Encoding UTF8
```

- [ ] **Step 2: Update `WeekTimelineHeader`**

Change the week area so it:

- keeps swipe gesture;
- removes the hint text;
- adds left and right clickable arrows that call `onDateSelected(selectedDate.minusDays(7))` and `onDateSelected(selectedDate.plusDays(7))`;
- uses Material-style icon surfaces, not raw `>` text.

- [ ] **Step 3: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git commit -m "feat: add week navigation arrows"
```

### Task 3: Show one blue unfinished-task dot on week and month dates

- [ ] **Step 1: Pass date-level task lookup to Today UI**

Today currently receives only tasks for the selected date. Add the smallest state shape needed so `TodayScreen` can know whether each visible week/month date has unfinished tasks. Prefer passing a `Map<LocalDate, Boolean>` or a callback from `AppShell`, rather than moving all app state into `TodayScreen`.

- [ ] **Step 2: Update week timeline dots**

In `WeekDayCell`, render one small Google-blue dot under the day number when `hasUnfinishedTask == true`.

- [ ] **Step 3: Update month calendar dots**

In `MonthGrid`, render the same one-dot marker below dates with unfinished tasks. Completed-only dates have no dot.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git commit -m "feat: show unfinished task date dots"
```

### Task 4: Fix summary card clipping

- [ ] **Step 1: Update summary card sizing**

Adjust `SummaryCard` so `PLANNED FOCUS`, `REVIEWED`, and values like `120 min`, `2h 10m`, `0/10` display fully. Keep the card compact and aligned with the reference.

Implementation constraints:

- No clipped label/value.
- No giant vertical growth.
- Use `maxLines = 1`, `softWrap = false`, and a slightly smaller fallback font where needed.
- Keep two cards in one row.

- [ ] **Step 2: Verify on emulator**

Install APK and inspect the main page:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: both summary cards show full text.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt
git commit -m "style: prevent main summary clipping"
```

### Task 5: Replace bottom nav text letters with Material-style icons

- [ ] **Step 1: Use project-owned vector resources or built-in Compose painters**

Do not add a new dependency unless absolutely necessary. Prefer local vector drawables for:

- tasks/check-circle;
- insights/bar-chart;
- settings/gear.

- [ ] **Step 2: Update `AttentionBottomBar`**

Replace `destination.iconText()` with icon rendering. Keep labels `Tasks`, `Insights`, `Settings`.

- [ ] **Step 3: Style selected nav like Google apps**

Use a pill behind the selected icon, blue selected color, neutral unselected color, and Material 3 navigation bar spacing.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/res/drawable
git commit -m "style: align bottom navigation icons"
```

## Chunk 3: Schedule Editor Scroll Wheel Behavior

**Files:**
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
- Modify only if required: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/domain/ScheduleOptions.kt`
- Existing tests: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/test/java/com/example/attentioncoach/domain/ScheduleOptionsTest.kt`

### Task 6: Make the selected hour/minute stay centered while scrolling

- [ ] **Step 1: Preserve current valid options**

Keep:

- hour options `0..23`;
- minute options every 5 minutes;
- initial position from saved `startTime`;
- default position from rounded current time when no start time exists.

- [ ] **Step 2: Implement centered wheel selection**

Change `WheelColumn` so:

- the selected/highlight band is fixed in the vertical center of the wheel card;
- scrolling updates `selectedHour` / `selectedMinute` based on the item nearest center;
- tapping an item animates that value into the center;
- reopening a task scrolls to the saved value centered, not merely visible.

Use existing Compose foundation APIs first, such as `LazyListState`, `LaunchedEffect`, `snapshotFlow`, and `animateScrollToItem`. Avoid a new snapper dependency.

- [ ] **Step 3: Move start-time wheel higher**

Reduce vertical waste above the wheel and use the freed space for duration. Keep the sheet height inside one phone screen.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 5: Emulator smoke**

Open a task, open schedule editor, set `19:40`, save, reopen, and verify `19` and `40` are centered/highlighted.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt
git commit -m "feat: center schedule wheel selection"
```

### Task 7: Polish duration grid and custom input

- [ ] **Step 1: Confirm 2x3 fixed grid**

Ensure the duration grid is exactly two rows by three cells:

First row: `15 min`, `30 min`, `45 min`.

Second row: `60 min`, `90 min`, custom input with fixed `min` suffix.

- [ ] **Step 2: Fix custom input interaction**

The custom cell should:

- accept digits only;
- keep `min` as fixed suffix, not part of the editable text;
- visually match selected duration styling when valid;
- not stretch or overlap.

- [ ] **Step 3: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt
git commit -m "style: refine schedule duration grid"
```

## Chunk 4: Plan Page Controls, Save Feedback, and Icon Consistency

**Files:**
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt`
- Modify: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt`

### Task 8: Make plan-page schedule and priority controls match the reference style

- [ ] **Step 1: Read source references**

Read:

```powershell
rg -n "field|duration-control|priority-field|priority-menu" ..\ui_prototype\styles.css
rg -n "toggle-priority|set-priority|renderTaskDetail" ..\ui_prototype\app.js
```

- [ ] **Step 2: Adjust only the visible controls**

Update:

- schedule field border, spacing, and typography;
- priority field chevron surface;
- `Targets` input card background/border;
- title area spacing only if required by screenshots.

Do not change task lifecycle logic in this chunk.

- [ ] **Step 3: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/ui/UiTokens.kt
git commit -m "style: polish plan page controls"
```

### Task 9: Replace raw arrows and close glyphs with consistent Material-style icon buttons

- [ ] **Step 1: Audit locations**

Audit:

- task detail back button;
- schedule editor back/close;
- schedule editor save check;
- date sheet close;
- date title chevrons;
- priority dropdown chevron;
- bottom navigation icons.

- [ ] **Step 2: Replace raw text glyphs where visible**

Use one consistent treatment:

- circular/pill icon button surface for close/save where appropriate;
- minimal plain icon button for back/chevrons;
- matching hit targets around 44-52dp.

- [ ] **Step 3: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt app/src/main/java/com/example/attentioncoach/ui/TaskScheduleEditor.kt app/src/main/res/drawable
git commit -m "style: unify material icon buttons"
```

### Task 10: Replace Android Toast with visible in-app schedule-saved feedback

- [ ] **Step 1: Implement transient in-sheet message**

When schedule is saved:

- persist `startTime` and `duration`;
- close the schedule editor;
- show a floating pill/snackbar inside the task detail sheet: `Schedule is saved`;
- auto-dismiss after a short delay.

Use Compose state rather than Android `Toast` so emulator screenshots can verify it.

- [ ] **Step 2: Verify**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build success.

- [ ] **Step 3: Emulator smoke**

Open task -> edit schedule -> save -> confirm message appears and reopened schedule keeps saved time.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt
git commit -m "feat: show schedule saved feedback"
```

## Chunk 5: Emulator Verification Notes

**Files:**
- Create: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/docs/main_plan_ui_followup_smoke_test.md`
- Optional screenshots: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/docs/emulator_screenshots/`

### Task 11: Record smoke coverage

- [ ] **Step 1: Run final verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
& 'C:\Users\11345\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: Gradle build success and APK installed.

- [ ] **Step 2: Interact with emulator**

Verify:

- main page summary labels/values are not clipped;
- bottom nav uses icons, not letters;
- week hint is gone;
- week arrows and swipe both change week;
- unfinished date shows one blue dot in week row;
- completed-only date hides the dot;
- calendar sheet shows the same unfinished blue dot rule;
- plan page schedule field is horizontal and readable;
- priority field chevron/icon is Google-style;
- schedule editor opens with saved time centered;
- wheel scroll changes selected hour/minute;
- duration grid is 2x3 and custom `min` is fixed;
- save check closes editor and shows `Schedule is saved`;
- reopening task keeps edited start time and duration.

- [ ] **Step 3: Write notes**

Record pass/fail, emulator/device used, and remaining visual differences.

- [ ] **Step 4: Commit**

```powershell
git add docs/main_plan_ui_followup_smoke_test.md
git commit -m "test: record main plan ui followup smoke coverage"
```

## Execution Rules

- Before each commit, run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- Commit after each subfeature, not only at the end.
- Do not mix visual polish with behavior commits unless the visual is inseparable from that behavior.
- Use emulator screenshots to validate UI claims.
- Do not touch existing untracked files unless the task explicitly uses them.
- Keep `docs/emulator_screenshots/` uncommitted unless the smoke-test documentation explicitly references selected screenshots.

## Open Alignment Points

- I will treat the latest `main_page/draft_plan.md` as overriding the older “remove all date dots” request. Final behavior: one blue dot means unfinished tasks remain.
- I will implement `Schedule is saved` as an in-app floating message, not Android system Toast, so the feedback is visually stable and screenshot-verifiable.
- Icons and controls should feel like Google official apps / Material 3. Prefer existing Material 3 components and project-owned vector drawables that follow Material icon geometry; do not leave raw text glyphs such as `<`, `>`, `X`, `T`, `I`, or `S` as visible controls.
