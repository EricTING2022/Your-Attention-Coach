# Attention Coach

Attention Coach is an Android task planning and focus-coaching application designed for students and self-directed learners who struggle with passive timers, poor task estimation, app distraction, and lack of post-task reflection.

The app helps users plan daily work, start timed focus blocks, receive start-time reminders, avoid distraction through soft-lock re-entry reminders, and review their actual performance after each task.

---

## Features

### Daily Planning

- View tasks by selected date.
- Browse the current week through a weekly timeline.
- Add, edit, and delete planned tasks.
- Configure task title, target, priority, optional start time, and planned duration.
- Sort tasks by priority and task identity for a stable daily task list.

### Task Scheduling

- Enable or disable a task start time.
- Select start time using 5-minute intervals.
- Choose duration presets such as 15, 30, 45, 60, and 90 minutes.
- Enter a custom duration when presets are not suitable.
- Avoid scheduling reminders for tasks whose start time has already passed.

### Focus Timer

- Start a focus block from a planned task.
- Display a countdown based on planned task duration.
- Show overtime using a `+MM:SS` format when the planned duration is exceeded.
- Pause and resume focus sessions.
- Finish a focus block and record actual focus time.
- Exit a focus block without mutating the task result.
- Keep the focus timer running even when the user leaves the app, opens a whitelisted app, opens a non-whitelisted app, goes home, or turns the screen off.

### Soft-lock Re-entry Reminder

Attention Coach implements a soft-lock re-entry mechanism during active focus sessions.

When a focus block is active, the app monitors whether the user remains inside Attention Coach or temporarily leaves to a whitelisted app. If the user leaves Attention Coach and remains outside the allowed scope after a short grace period, the app sends a re-entry reminder encouraging the user to return to the focus session.

Supported behavior:

- Returning to Attention Coach clears the re-entry reminder state.
- Opening a whitelisted app suppresses re-entry reminders.
- Opening a non-whitelisted app triggers re-entry reminders.
- Home, recents, unknown foreground state, and non-whitelisted packages are treated as outside the allowed scope.
- The first reminder after leaving the app bypasses the normal cooldown.
- Later reminders follow the notification interval configured in Settings.
- Visible re-entry notifications are cancelled when the user returns to Attention Coach.
- Screen-off behavior continues reminders when the user was already outside the allowed scope.

This design keeps the app non-blocking: it does not prevent the user from leaving the app, but it actively nudges the user back to the focus session.

### Start-time Reminders

- Schedule exact alarms for tasks with a configured start time.
- Show high-priority notifications when a task is due.
- Support lockscreen / full-screen reminder activity where available.
- Defer start-time reminders if another focus session is already active.
- Release deferred reminders after the active focus block ends.
- Mark due tasks in the task list until the user opens or starts the task.

### Review Workflow

- Unlock the Review tab after a task is finished.
- Record whether the task was completed as planned.
- Select a mismatch reason from presets or enter a custom reason.
- Add a next-step adjustment for future planning.
- Use review data to support weekly insight summaries.

### Weekly Insights

- Show planned versus actual focus minutes for the selected week.
- Display common review reasons.
- Help users compare estimated work with actual focus behavior.

### Settings

- Manage the apps whitelist used by the soft-lock mechanism.
- Add and remove launchable apps from the whitelist.
- Configure the re-entry notification interval.
- View current reminder interval and whitelist settings.

---

## Tech Stack

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Android Architecture Components / ViewModel
- Kotlin coroutines / Flow
- Room
- DataStore Preferences
- Android foreground service
- AlarmManager
- BroadcastReceiver
- NotificationManager
- AccessibilityService
- JUnit 4

---

## Implementation Updates After Initial Submission

The original README and project proposal described the first complete prototype. The current implementation adds two major engineering upgrades while preserving the original product direction: durable local persistence and the second-generation soft-lock re-entry system.

### Update 1: Room and DataStore Persistence

The app now uses a persistent local data layer instead of relying on in-memory demo state.

Implemented behavior:

* Tasks created or edited by the user are saved locally and remain available after app restart.
* Task reviews are stored separately from the task plan and can be overwritten when the user saves a new review.
* Settings, including the apps whitelist and notification interval, are stored durably.
* Active focus session recovery state is stored so the app can resume the focus timer after process recreation instead of treating process death as an implicit exit.
* The `Seed demo day` action remains available for grading/demo preparation. It replaces the May 5 demo data idempotently, so repeated taps do not create duplicate demo tasks.

Implementation details:

| Area | Implementation | Key Files |
| --- | --- | --- |
| Task persistence | Room database with task and review tables | `data/local/AttentionCoachDatabase.kt`, `TaskEntity.kt`, `TaskReviewEntity.kt`, `AttentionCoachDao.kt` |
| Repository boundary | UI talks to repositories instead of direct storage classes | `data/TaskRepository.kt`, `data/RoomTaskRepository.kt`, `ui/AttentionCoachViewModel.kt` |
| Domain mapping | Room entities are mapped to domain `PlannedTask` objects | `data/TaskMappers.kt` |
| Settings persistence | DataStore Preferences store whitelist apps and notification interval | `data/DataStoreSettingsRepository.kt`, `data/DataStoreKeys.kt` |
| Focus recovery | DataStore persists the active work session | `platform/FocusSessionStore.kt` |
| Dependency wiring | Application container owns database and repositories | `AppContainer.kt`, `AttentionCoachApplication.kt` |

Design rationale:

* Room is used for structured domain data because tasks and reviews have clear fields, relationships, and query needs.
* DataStore is used for preferences because settings are small key-value style data and should not be modeled as relational entities.
* Repository interfaces keep UI code independent from the storage implementation and leave room for future cloud sync or backup support.

### Update 2: Soft-lock Re-entry V2

The soft-lock feature has been rebuilt around a clearer presence state machine and real-device foreground detection.

Implemented behavior:

* During an active focus timer, Attention Coach classifies the user into one of four stable states:
  * `IN_ATTENTION_COACH`
  * `IN_WHITELIST_APP`
  * `IN_LAUNCHER`
  * `IN_OTHER_APP`
* `IN_ATTENTION_COACH` and `IN_WHITELIST_APP` are treated as safe states.
* `IN_LAUNCHER` and `IN_OTHER_APP` are treated as unsafe states and trigger re-entry reminders after a short grace period.
* The apps whitelist is configurable in Settings and is shared by the focus screen and monitoring service.
* Screen-off behavior uses the last reliable foreground presence instead of treating Android System UI as a user app.
* Unsafe screen-off reminders are driven by an `AlarmManager` single-shot chain so reminders can continue while the screen is locked.
* Safe screen-off states pause foreground polling and resume polling when the screen turns on.
* The first unsafe screen-off reminder can use a lockscreen/full-screen route when Android allows it.
* Notification taps and lockscreen `Return to focus` return directly to the active focus timer without an intermediate re-entry page.

Implementation details:

| Area | Implementation | Key Files |
| --- | --- | --- |
| Foreground detection | Accessibility observer records the current foreground package | `platform/AttentionCoachAccessibilityService.kt`, `ForegroundObservationStore.kt` |
| Presence classification | Pure domain rules classify foreground state and preserve last stable presence across System UI / lockscreen events | `domain/PlanningRules.kt` |
| Screen-on reminders | Foreground service applies presence policy and notification cooldown while the device is interactive | `platform/FocusMonitorService.kt` |
| Screen-off reminders | Persisted monitor state and broadcast receiver run the AlarmManager reminder chain | `platform/ReentryMonitorStateStore.kt`, `platform/ReentryReminderReceiver.kt` |
| Notification delivery | High-priority re-entry notifications and lockscreen route | `platform/ReentryNotifier.kt`, `platform/ReentryLockscreenActivity.kt` |
| Routing | Re-entry intents route directly back to the active focus timer | `MainActivity.kt`, `ui/AppShell.kt` |
| Verification docs | Layered real-device test plan and results | `docs/softlock_reentry_v2_manual_test.md`, `docs/softlock_reentry_v2_real_device_results.md` |

Design rationale:

* UsageStats alone was not reliable enough on the target real device, so V2 uses Accessibility foreground observation as the primary signal.
* The state machine is expressed in domain code and unit-tested, while Android services only perform platform integration.
* Screen-off reminders are handled by alarms rather than continuous polling to reduce unnecessary wakeups.
* The feature remains a soft lock: it nudges the user back to focus but does not use kiosk mode, device-owner APIs, or hard blocking.

---

## Architecture

Attention Coach uses a lightweight layered Compose architecture.

The project is organized around three main layers:

```text
Compose UI
    ↓
Domain Rules
    ↓
Android Platform Adapters
````

### UI Layer

The UI layer is built with Jetpack Compose. It contains screens and reusable components for:

* Today task list
* Task detail sheet
* Schedule editor
* Focus timer
* Pause screen
* Review tab
* Insights screen
* Settings screens
* Whitelist editor
* Notification interval picker

Main UI state is hoisted in the app shell and passed down through composable functions.

### Domain Layer

The domain layer contains testable business logic, including:

* Task sorting
* Calendar rules
* Schedule editor rules
* Review availability
* Insight calculation
* Reminder decision logic
* Soft-lock re-entry policy
* Focus timer calculations
* Foreground presence classification

This keeps important behavior testable without depending directly on Android framework APIs.

### Platform Layer

The platform layer wraps Android-specific APIs:

* Exact alarm scheduling
* Start-time reminder receiver
* Foreground focus monitor service
* Accessibility-based foreground observation
* Re-entry notification delivery
* Screen-off re-entry alarm receiver
* Installed app querying
* Needed app launching
* Room database wiring
* DataStore-backed settings and active focus state

This separation keeps Android framework interaction outside the core domain logic.

---

## Project Structure

```text
app/src/main/java/com/example/attentioncoach/
├── domain/
│   ├── Models.kt
│   ├── PlanningRules.kt
│   ├── InsightRules.kt
│   ├── ScheduleEditorRules.kt
│   └── ...
├── platform/
│   ├── TaskReminderScheduler.kt
│   ├── TaskReminderReceiver.kt
│   ├── FocusMonitorService.kt
│   ├── UsageStatsBoundary.kt
│   ├── ReentryNotifier.kt
│   ├── ReentryReminderReceiver.kt
│   ├── ReentryMonitorStateStore.kt
│   └── ...
├── ui/
│   ├── AppShell.kt
│   ├── TodayScreen.kt
│   ├── TaskDetailSheet.kt
│   ├── TaskScheduleEditor.kt
│   ├── FocusScreens.kt
│   ├── TopLevelInfoScreens.kt
│   └── ...
└── MainActivity.kt
```

Unit tests are located under:

```text
app/src/test/java/com/example/attentioncoach/
```

---

## Android Permissions

Attention Coach uses several Android platform permissions to support reminders and focus monitoring.

| Permission / Capability  | Purpose                                                                 |
| ------------------------ | ----------------------------------------------------------------------- |
| `POST_NOTIFICATIONS`     | Show task reminders, active focus notifications, and re-entry reminders |
| `SCHEDULE_EXACT_ALARM`   | Schedule accurate start-time reminders                                  |
| `USE_FULL_SCREEN_INTENT` | Show lockscreen / full-screen reminder routes where supported           |
| `PACKAGE_USAGE_STATS`    | Detect the current foreground app during an active focus block          |
| Accessibility service    | Observe the foreground app for soft-lock re-entry decisions             |
| Foreground service       | Keep focus monitoring active while the user is outside Attention Coach  |
| Package queries          | Display launchable apps for whitelist configuration                     |

Some permissions and capabilities, such as Accessibility foreground detection, Usage Access fallback, notification permission, exact alarm access, and full-screen notification display, may require manual approval in Android system settings depending on device and Android version.

---

## Build and Run

### Prerequisites

* Android Studio
* Android SDK
* JDK compatible with the Android Gradle Plugin used by the project
* Android device or emulator

### Clone the repository

```bash
git clone https://github.com/EricTING2022/Your-Attention-Coach.git
cd Your-Attention-Coach
```

### Build debug APK

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS / Linux:

```bash
./gradlew assembleDebug
```

### Run unit tests

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

On macOS / Linux:

```bash
./gradlew testDebugUnitTest
```

### Recommended verification command

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

---

## Manual Testing Checklist

The following flows are recommended for manual verification:

| Area                | Scenario                                                             | Expected Result                                         |
| ------------------- | -------------------------------------------------------------------- | ------------------------------------------------------- |
| Task planning       | Create a task with title, target, priority, start time, and duration | Task appears on the selected date                       |
| Task editing        | Modify priority or duration                                          | Task card updates correctly                             |
| Task deletion       | Delete an existing task                                              | Task disappears from the list                           |
| Completion          | Toggle task completion                                               | Task title becomes struck through                       |
| Focus timer         | Start a focus block                                                  | Countdown begins                                        |
| Pause               | Pause and resume focus                                               | Pause time is excluded from active focus                |
| Finish              | Finish a focus block                                                 | Task becomes finished and actual focus time is recorded |
| Exit                | Exit a focus block                                                   | Task result remains unchanged                           |
| Review              | Open a finished task                                                 | Review tab is available                                 |
| Insights            | Finish and review tasks                                              | Weekly planned vs actual data updates                   |
| Start-time reminder | Schedule a task shortly in the future                                | Reminder notification appears                           |
| Deferred reminder   | Let another task become due during active focus                      | Reminder is deferred until focus ends                   |
| Whitelist           | Open a whitelisted app during focus                                  | Re-entry reminder is suppressed                         |
| Re-entry            | Open a non-whitelisted app during focus                              | Re-entry notification appears after the grace period    |
| Return to app       | Open Attention Coach after re-entry reminder                         | Re-entry state is cleared and notification disappears   |
| Settings            | Change notification interval                                         | Later re-entry reminders follow the selected interval   |
| Persistence         | Restart the app after editing tasks/settings                         | Tasks, reviews, whitelist, interval, and active focus state persist |

---

## Current Limitations

* There is no Firebase, remote API, account login, or cloud sync layer.
* Accessibility, exact-alarm, notification, and full-screen reminder behavior depend on Android system settings and device policy.
* Full-screen reminder behavior may vary by Android version and device policy.
* The project currently focuses on unit tests and manual verification rather than full Compose UI instrumentation tests.

---

## Design Notes

Attention Coach is intentionally more active than a passive Pomodoro timer. Instead of only counting down time, it combines:

* planning,
* timed focus,
* start-time reminders,
* soft-lock re-entry nudges,
* task review,
* and weekly reflection.

The soft-lock design is intentionally non-invasive. It does not block other apps or enforce device-level restrictions. Instead, it uses Android notifications and foreground monitoring to encourage the user to return to the intended focus task while still allowing legitimate temporary app switching through the whitelist.

---

# Future Improvements

The following items are intentionally placed at the end of the README as the project roadmap. Completed items are kept with strikethrough text to show how the implementation has advanced beyond the first submitted version.

* ~~Add Room database persistence for tasks and reviews.~~ Implemented with `AttentionCoachDatabase`, `TaskEntity`, `TaskReviewEntity`, and `RoomTaskRepository`.
* ~~Add DataStore for settings persistence.~~ Implemented for apps whitelist and notification interval.
* ~~Persist active focus session recovery state.~~ Implemented with `FocusSessionStore`.
* ~~Improve soft-lock re-entry reliability.~~ Implemented in Soft-lock Re-entry V2 using Accessibility foreground observation, presence classification, screen-off alarm reminders, and direct focus routing.
* ~~Route re-entry reminders directly back to the focus timer.~~ Implemented for normal notifications and lockscreen/full-screen reminders.
* Add a dedicated permission education flow for Accessibility, notification, exact alarm, and full-screen reminder capabilities.
* Add Compose UI instrumentation tests for key user flows.
* Add exportable weekly insight reports.
* Improve long-term analytics for planning accuracy and distraction patterns.
* Add cloud sync or account-based backup if multi-device support is required.
