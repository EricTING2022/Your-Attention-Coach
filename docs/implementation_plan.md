# Attention Coach Android Implementation Plan

> Implementation contract: follow `../app_design_freeze` for UI, interaction logic, state rules, and acceptance criteria. This Android app folder is its own git repository. Every completed, verified subtask must be committed before starting the next substantial subtask.

**Goal:** Build the native Android MVP for the plan-aware attention coach course project.

**Architecture:** Single-activity Kotlin Android app using Jetpack Compose for UI, local demo/domain state first, and platform boundaries for UsageStats, foreground work timing, notifications, and app launching. UI must mirror the frozen HTML prototype closely enough for screenshots and final demo.

**Tech Stack:** Kotlin, Gradle Android Plugin, Jetpack Compose, Material 3, JUnit for pure logic tests, Android SDK platform APIs for UsageStats/notifications/app launch intents.

---

## Git Discipline

- Initialize `project/attention_coach_android` as a standalone git repository.
- Commit the plan and scaffold first.
- After each implementation subtask:
  - run the relevant verification command;
  - inspect the result;
  - `git add` only relevant files;
  - `git commit -m "<type>: <short task summary>"`.
- If verification fails, do not commit that subtask until the failure is understood or explicitly documented.

## Task 1: Scaffold Android App

**Files:**

- Create Gradle settings/build files.
- Create `app/` Android application module.
- Create Android manifest and `MainActivity`.

**Verification:**

- `gradle test` or `gradlew test` once wrapper/project is ready.
- `gradle assembleDebug` or `gradlew assembleDebug`.

**Commit:** `chore: scaffold android project`

## Task 2: Core Domain Tests And Models

**Files:**

- Create task/session/intervention models.
- Create pure logic helpers for:
  - date/week timeline;
  - task grouping;
  - planned/reviewed summary;
  - soft-lock whitelist decision;
  - 30-second re-entry notification cooldown.

**Verification:**

- Write tests before implementation.
- Run unit tests and confirm red/green where practical.

**Commit:** `test: cover core planning and soft-lock rules`

## Task 3: App State And Demo Repository

**Files:**

- Create local demo repository seeded with the frozen prototype tasks.
- Create mutable app state for selected date, selected task, active tab, work session, settings, and seeded apps.

**Verification:**

- Unit tests for state transitions:
  - save review moves task to reviewed section;
  - start work preserves original plan;
  - exit returns to today without changing task plan.

**Commit:** `feat: add demo repository and app state`

## Task 4: Top-Level Compose Navigation

**Files:**

- Build single-activity Compose shell.
- Add top-level screens: Tasks, Insights, Settings.
- Add bottom navigation visible only on top-level screens.

**Verification:**

- Unit tests where possible for route state.
- `assembleDebug`.

**Commit:** `feat: add top-level compose shell`

## Task 5: Today Screen And Date Picker

**Files:**

- Implement fixed week timeline.
- Implement date picker sheet with month grid and year/month mode.
- Implement task list grouping and chips.

**Verification:**

- Compare against `app_design_freeze/snapshots/today_default.png`, `date_month_picker.png`, and `date_year_month_picker.png`.
- `assembleDebug`.

**Commit:** `feat: implement today timeline and date picker`

## Task 6: Task Detail Bottom Sheet

**Files:**

- Implement near-full-screen task detail sheet.
- Implement Plan and Review tabs.
- Implement duration field with fixed `min`.
- Implement priority picker with four fixed colored options.
- Implement review save behavior.

**Verification:**

- Compare against Plan/Review snapshots.
- Unit test review transition.
- `assembleDebug`.

**Commit:** `feat: implement task detail plan review flow`

## Task 7: Work, Pause, Exit, And Soft-Lock State

**Files:**

- Implement Work screen matching frozen timer layout.
- Hide bottom navigation and use full-screen focus mode.
- Handle Android back with Exit confirmation behavior.
- Implement Pause screen.
- Add Needed apps data model and launcher boundary.

**Verification:**

- Compare against work/pause/exit snapshots.
- Unit test soft-lock whitelist and cooldown rules.
- `assembleDebug`.

**Commit:** `feat: implement focus work block soft lock`

## Task 8: Re-entry Platform Boundary

**Files:**

- Add notification/deep-link route for Re-entry.
- Add platform interfaces/stubs for UsageStats monitoring and notification banner sending.
- Add cooldown behavior.

**Verification:**

- Unit tests for notification trigger decisions.
- Manual smoke path inside app for Re-entry route if platform permission is unavailable.
- `assembleDebug`.

**Commit:** `feat: add re-entry monitoring boundary`

## Task 9: Polish, Permissions, And Demo Readiness

**Files:**

- Add usage access / notification permission settings copy.
- Add demo seed/reset action.
- Polish UI tokens, colors, spacing, and typography.

**Verification:**

- Full unit test run.
- `assembleDebug`.
- Manual screenshot pass on emulator/device if available.

**Commit:** `feat: polish demo-ready app`
