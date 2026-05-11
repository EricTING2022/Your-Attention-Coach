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
- Android foreground service
- AlarmManager
- BroadcastReceiver
- NotificationManager
- UsageStatsManager
- SharedPreferences
- JUnit 4

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

This keeps important behavior testable without depending directly on Android framework APIs.

### Platform Layer

The platform layer wraps Android-specific APIs:

* Exact alarm scheduling
* Start-time reminder receiver
* Foreground focus monitor service
* UsageStats foreground app detection
* Re-entry notification delivery
* Installed app querying
* Needed app launching
* SharedPreferences-backed reminder state

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
| Foreground service       | Keep focus monitoring active while the user is outside Attention Coach  |
| Package queries          | Display launchable apps for whitelist configuration                     |

Some permissions, such as Usage Access and exact alarm access, may require manual approval in Android system settings depending on device and Android version.

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

---

## Current Limitations

* Task data is currently stored in app memory and seeded demo data; user-created tasks are not persisted through process death.
* There is no Room, Firebase, or backend integration.
* Settings and whitelist state are lightweight and should be moved to a durable persistence layer in a production version.
* Usage Access permission flow depends on Android system settings.
* Full-screen reminder behavior may vary by Android version and device policy.
* The project currently focuses on unit tests and manual verification rather than full Compose UI instrumentation tests.

---

## Future Improvements

* Add Room database persistence for tasks, reviews, settings, and reminder state.
* Introduce a ViewModel and Repository layer for lifecycle-safe state management.
* Add DataStore for settings persistence.
* Add a dedicated Usage Access permission education screen.
* Add Compose UI tests for key user flows.
* Add exportable weekly insight reports.
* Improve long-term analytics for planning accuracy and distraction patterns.
* Add cloud sync or account-based backup if multi-device support is required.

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
