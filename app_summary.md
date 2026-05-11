# Attention Coach Final-Report Inventory

Scope note: scanned current working tree under `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android`. Build verification passed with `.\gradlew.bat testDebugUnitTest assembleDebug`. Emulator behavior is marked **Needs verification** where source code alone is insufficient.

## 1. Project Overview

**App name:** Attention Coach  
Source: `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/res/values/strings.xml`

**Purpose:** A task planning and focus-coaching Android app. It helps users plan daily work, start timed focus blocks, review actual performance, receive start-time reminders, and get re-entry reminders when leaving the app during focus.

**Target users:** Students or self-directed learners who struggle with passive timers, poor task estimation, short-video/app distraction, and lack of post-task reflection.

**Main user flows:**

| Flow | Summary |
|---|---|
| Plan tasks | Select date → add task → set title, target, priority, start time, duration |
| Start work | Open task → start focus block → countdown runs → pause / finish / exit |
| Reflect | Finished task unlocks Review tab → record completion, reason, adjustment |
| Reminder response | Start-time alarm fires → notification / lockscreen activity → open task |
| Soft lock | During focus, leaving Attention Coach can trigger re-entry notification unless user is in whitelist app |
| Insights | View weekly planned vs actual minutes and common review reasons |
| Settings | Edit apps whitelist and notification interval |

## 2. Implemented Features

| Feature | User-facing behavior | Key source files/classes | Implementation details | Edge cases handled | Suggested screenshots |
|---|---|---|---|---|---|
| Daily task list + weekly timeline | Main screen shows selected date, weekly row, planned focus summary, reviewed count, sorted task cards | `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui/TodayScreen.kt`; `PlanningRules.kt` | `WeekTimeline.weekFor()` starts week on Sunday; `TaskListSorter.sortForToday()` sorts by priority then task id | Completed tasks stay in priority position; week dots show unfinished days | Today screen with several tasks; week timeline |
| Calendar / date picker | Tap date title to open month grid; year/month wheel supports 2020–2035 and all 12 months | `TodayScreen.kt`; `DatePickerOptions.kt`; `CalendarRules.kt` | Uses `YearMonth.lengthOfMonth()` and Java time APIs | Actual month length; tested May 7, 2026 Thursday | Month picker; year/month wheel |
| Task create/edit/delete | Add opens create-mode task sheet; existing task opens bottom sheet; three-dot menu deletes task with confirmation | `AppShell.kt`; `TaskDetailSheet.kt`; `AttentionCoachStore.kt` | Create defaults: selected date, 30 min, Important, Planned; delete removes task and closes detail | Duplicate state reset on delete; deleting active task clears active work in UI path | Create sheet; task menu delete dialog |
| Priority picker | Priority field opens scrollable dropdown with four priorities | `TaskDetailSheet.kt`; `Models.kt`; `UiTokens.kt` | Priorities: Urgent & important, Urgent, Important, Not urgent | Selection updates local form state before save | Priority dropdown |
| Schedule editor | Duration field opens schedule editor; user can toggle start time on/off, choose hour/minute wheel, choose preset/custom duration | `TaskScheduleEditor.kt`; `ScheduleOptions.kt`; `ScheduleEditorRules.kt` | 5-minute start-time steps; duration presets 15/30/45/60/90; custom numeric duration | Disabled start time saves `null`; custom duration filters digits and max 3 digits | Schedule editor with toggle on/off |
| Completion toggle | Task card right side has circular toggle; completed task title is struck through; toggling again restores Planned | `TodayScreen.kt`; `AppShell.kt`; `AttentionCoachStore.kt` | Completion state maps to `FINISHED`; untoggle clears actual/review fields | Reviewed/finished both treated as completed | Task list with completed strikethrough |
| Focus timer | Work screen shows countdown from planned duration, overtime as `+MM:SS`, Needed apps menu, Pause/Finish/Exit | `FocusScreens.kt`; `WorkSessionClock.kt`; `AppShell.kt` | `LaunchedEffect` ticks every second; Finish writes `actualFocusMinutes`; Exit does not mutate task | Actual focus rounds up; overtime handled; Back opens exit confirm | Focus timer screen; finish/exit dialogs |
| Pause flow | Pause screen shows 3-minute countdown and Continue focus button | `FocusScreens.kt`; `WorkSessionClock.kt` | Pause time excluded from active focus calculation | Pause timer clamps at 00:00; Back disabled | Pause screen |
| Review workflow | Review tab appears only for Finished/Reviewed tasks; records actual completion, reason preset/custom, next adjustment | `TaskDetailSheet.kt`; `ReviewAvailability` in `PlanningRules.kt`; `InsightRules.kt` | Horizontal pager for Plan/Review when review is available | Planned tasks do not show Review tab; Other clears reason field for free input | Finished task detail with Review tab |
| Weekly insights | Insights page shows planned vs actual daily bars for selected week and common reasons | `TopLevelInfoScreens.kt`; `InsightRules.kt` | Week is selected-date week; common reasons includes default reasons even when count is 0 | Custom reasons appended and sorted by count/name | Insights screen |
| Settings: apps whitelist | Settings home has Apps whitelist button; whitelist screen shows current apps, package names, remove button, add-app dialog | `TopLevelInfoScreens.kt`; `AppSettings.kt`; `InstalledAppsProvider.kt` | Queries launchable apps via package manager; filters already-selected apps | Duplicate apps ignored; empty whitelist message | Settings home; whitelist list; add-app dialog |
| Settings: notification interval | Settings home shows current interval; interval screen provides scrollable radio list and Confirm | `TopLevelInfoScreens.kt`; `AppSettings.kt` | Options: 30s, 1 min, 2 min, 5 min | Unsupported interval ignored in domain rules | Interval picker |
| Start-time reminders | Saving scheduled task uses exact alarm; alarm triggers high-priority notification and optional lockscreen full-screen activity | `TaskReminderScheduler.kt`; `TaskReminderReceiver.kt`; `ReminderActivity.kt`; `AlarmPermissionHelper.kt` | Uses `AlarmManager.setExactAndAllowWhileIdle`; notification channel high importance; `ReminderActivity` uses `setShowWhenLocked(true)` / `setTurnScreenOn(true)` | Past start time is not scheduled; missing exact-alarm permission prompts user; repeat alarm chains until acknowledged | Notification shade; lockscreen reminder activity |
| Deferred reminders during focus | If start-time reminder fires while focus timer is active, it is deferred; when focus ends, due reminders are released and highest-priority task is notified | `TaskReminderReceiver.kt`; `StartReminderStore.kt`; `FocusSessionStore.kt`; `ReminderRules.kt`; `AppShell.kt` | SharedPreferences stores deferred/active due/acknowledged ids; highest priority uses priority rank then id | Completed/reviewed tasks ignored for highest-priority selection | Focus ends, then reminder banner; task list due alarm icon |
| Due task marker | Overdue/due tasks show alarm marker in task list until user opens or starts that task | `TodayScreen.kt`; `ic_start_due_alarm_24.xml`; `AppShell.kt` | Opening task or starting work calls `acknowledgeReminder()` | Main page entry may cancel visible notification but does not clear due state | Task list with alarm marker |
| Soft-lock / re-entry reminder | During active work, foreground service monitors foreground app and sends re-entry notification when user leaves allowed scope | `FocusMonitorService.kt`; `UsageStatsBoundary.kt`; `ReentryNotifier.kt`; `SoftLockPolicy` in `PlanningRules.kt`; `MainActivity.kt` | Polls every 5s using UsageStats; whitelist apps suppress notification; cooldown configurable via settings interval | Missing UsageStats returns null and currently suppresses notification; screen-off alarm fallback is **Needs verification / likely incomplete** | Home screen after leaving app; re-entry notification |

## 3. Architecture

**Pattern:** Lightweight layered Compose architecture, not full MVVM. It uses:
- Compose UI with hoisted state in `AttentionCoachApp`
- Pure domain rule objects for testable logic
- Platform adapter classes for Android services, alarms, notifications, UsageStats, package manager
- Demo/in-memory task state rather than Room/Repository persistence

**Major packages/modules:**

| Package | Role |
|---|---|
| `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/domain` | Models, pure rules, seed data, testable business logic |
| `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/ui` | Jetpack Compose screens/components |
| `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/main/java/com/example/attentioncoach/platform` | Android APIs: alarms, notification, foreground service, usage stats, installed apps |
| `D:/Desktop/HKUST/25-26spring/comp4521/project/attention_coach_android/app/src/test/java/com/example/attentioncoach` | JUnit tests for domain/platform state stores |

**Data flow:**
UI event → `AppShell.kt` callback → mutate Compose state or call domain rule → optional platform adapter call.  
Example: Save plan → update `tasks` list in `AppShell.kt` → call `TaskReminderScheduler.schedule()` → `TaskReminderReceiver` handles future alarm.

**State management:**
- Main task list, selected date, selected task, active work, settings: `remember { mutableStateOf(...) }` in `AppShell.kt`
- Reminder/focus cross-process state: SharedPreferences via `FocusSessionStore.kt` and `StartReminderStore.kt`
- Task data is seeded from `DemoTaskRepository.kt`; task edits are not persisted across process death. **Important limitation.**

**Navigation:**
- No Navigation Compose graph.
- Top-level tabs use `TopLevelDestination` enum: Tasks, Insights, Settings.
- Task detail, focus, pause, re-entry are controlled by local state such as `selectedTaskId`, `draftTask`, `activeWork`, `reentryOpen`.

## 4. Data and Backend

**Local database schema:** None. No Room/SQLite schema found.

**Firebase/backend/API:** None. Gradle dependencies show Compose, Material3, lifecycle runtime, JUnit only. No Firebase/Retrofit/network dependency.

**Main models/entities:**
- `PlannedTask`: id, date, title, target, startTime, durationMinutes, priority, status, actualFocusMinutes, actualCompletion, mismatchReason, nextAdjustment
- `Priority`: `URGENT_IMPORTANT`, `URGENT`, `IMPORTANT`, `NOT_URGENT`
- `TaskStatus`: `PLANNED`, `REVIEWED`, `FINISHED`, `MISSED`
- `ActiveWork`: focus session timing state
- `AppSettings`, `NeededApp`
- `WeeklyInsight`, `DailyInsight`, `ReasonCount`

**Persistence behavior:**
- Tasks/settings: in-memory Compose state only. **Not durable.**
- Seed tasks: `DemoTaskRepository.seed()`
- Reminder state: SharedPreferences in `StartReminderStore.kt`
- Focus-active marker: SharedPreferences in `FocusSessionStore.kt`
- Notification acknowledgement also uses SharedPreferences in `TaskReminderReceiver.kt`

**Synchronization:** No cloud sync or account sync.

## 5. Android / Platform Features

| Platform feature | Implementation | Failure handling |
|---|---|---|
| Runtime notifications | `POST_NOTIFICATIONS` in manifest; requested in `MainActivity.kt` | Permission denial handling is minimal; notifications may not appear. Needs manual verification |
| Exact alarms | `SCHEDULE_EXACT_ALARM`; `AlarmPermissionHelper.kt`; `TaskReminderScheduler.kt` | If cannot schedule exact alarms, UI shows `AlarmPermissionPrompt` |
| Full-screen/lockscreen reminder | `USE_FULL_SCREEN_INTENT`; `ReminderActivity.kt`; full-screen pending intent in `TaskReminderReceiver.kt` | Android 14+ may restrict full-screen intent. Needs device verification |
| Foreground service | `FocusMonitorService.kt`, foreground notification via `ReentryNotifier.kt` | Stops when focus paused/ended; no screen-off AlarmManager fallback visible in current code |
| UsageStats monitoring | `PACKAGE_USAGE_STATS`; `UsageStatsBoundary.kt` | If no usage access or no foreground package, policy treats as self/no notification. This can hide re-entry reminders |
| Package queries / launch apps | Manifest `queries`; `InstalledAppsProvider.kt`; `NeededAppLauncher.kt` | Missing app shows Toast |
| Notifications | Channels: task reminders, active work block, re-entry reminder | Re-entry notification has `setTimeoutAfter(15000)`; repeated screen-off behavior needs verification |
| Camera/location/sensors/storage | Not used | N/A |

## 6. Testing Checklist

| Test ID | Feature | Scenario | Expected result | Current evidence in code | Recommended manual test |
|---|---|---|---|---|---|
| T01 | Build | Run unit tests and debug build | Build succeeds | `testDebugUnitTest assembleDebug` passed | Re-run before submission |
| T02 | Calendar | May 7, 2026 weekday | Thursday | `CalendarRulesTest.kt` | Open May 2026 picker |
| T03 | Task ordering | Mixed priorities | Urgent & important first, then Urgent, Important, Not urgent; same priority by id | `PlanningRulesTest.kt` | Create 4 tasks with priorities |
| T04 | Task CRUD | Add and delete task | Task appears/disappears on selected date | `AppStateTest.kt` | Add task; delete via three-dot menu |
| T05 | Schedule editor | Disable start time | Saved task has no start reminder | `ScheduleOptionsTest.kt` | Toggle off start time and save |
| T06 | Priority dropdown | Select each priority | Chip updates and sorting changes | `PlanningRulesTest.kt` partly | Manual UI check |
| T07 | Work timer | Start 30-min focus | Countdown decreases; overtime shows `+MM:SS` | `WorkSessionClockTest.kt` | Start short/custom duration |
| T08 | Pause | Pause focus | 3-min pause countdown; pause time excluded | `WorkSessionClockTest.kt`; `AppStateTest.kt` | Pause, resume, finish |
| T09 | Finish | Finish focus | Task becomes FINISHED; actual focus saved | `AppStateTest.kt` | Finish after 1–2 min |
| T10 | Exit | Exit focus | Plan/status/actual focus unchanged | `AppStateTest.kt` | Start then exit |
| T11 | Review gating | Planned task | Review tab hidden | `AppStateTest.kt`; `ReviewAvailability` | Open planned vs finished task |
| T12 | Insights | Selected week | Weekly planned/actual bars and default reasons | `InsightRulesTest.kt` | Switch selected date/week |
| T13 | Settings whitelist | Add/remove app | Count and list update dynamically | `AppSettingsRulesTest.kt`; UI in `TopLevelInfoScreens.kt` | Add Chrome/remove app |
| T14 | Start-time alarm | Future scheduled task | Notification repeats until app/task acknowledged | `ReminderRulesTest.kt`; `TaskReminderReceiver.kt` | Set task 1–2 min ahead; lock screen |
| T15 | Deferred reminder | Start time occurs during focus | Reminder deferred until focus ends; highest priority shown | `ReminderRulesTest.kt`; `ReminderStoresTest.kt` | Run focus over another task’s start time |
| T16 | Due marker | Due task not opened | Alarm icon appears in task list | Source only; no UI test | Wait for due reminder; return to Tasks |
| T17 | Re-entry | Leave app during focus | Re-entry notification appears after cooldown unless whitelist app | `PlanningRulesTest.kt`; `FocusMonitorService.kt` | Requires Usage Access; test home/non-whitelist/whitelist |
| T18 | Needed app launch | Select whitelisted Chrome | Chrome opens; timer continues | `NeededAppLauncher.kt` | Test on emulator/device with Chrome installed |

## 7. Code Quality / Software Engineering

- **Separation of concerns:** Good split between UI (`ui`), pure logic (`domain`), and Android APIs (`platform`). Business rules such as task sorting, calendar, reminders, insights, and timer calculations are unit-testable.
- **Reusable components:** Compose components are reusable within app: `TaskCard`, `TaskDetailSheet`, `TaskScheduleEditor`, `WeeklyBarChart`, settings rows, chips, dialogs.
- **Error handling:** Handles missing exact-alarm permission, missing needed app, past start time, duplicate whitelist apps, invalid interval, numeric custom duration filtering.
- **Input validation:** Custom duration strips non-digits and requires positive value; blank create title becomes `"Untitled task"`; unsupported notification interval ignored.
- **Asynchronous programming:** Uses Compose `LaunchedEffect` for timer ticks and UI side effects; Android `Handler` loop for foreground app polling; `AlarmManager` for scheduled reminders.
- **Design patterns:** Rule objects / policy objects (`ReminderRules`, `SoftLockPolicy`, `InsightRules`), platform adapter boundary (`UsageStatsBoundary`, `InstalledAppsProvider`), state hoisting in Compose.
- **Main engineering weakness:** No persistent task repository or ViewModel layer; most app state is stored in Composable memory.

## 8. Known Limitations and Future Improvements

**Current limitations:**
- Tasks and settings are not persisted in a database; app restart may lose user-created task data.
- No Room/Firebase/backend integration.
- No UI automation tests; most evidence is unit tests plus manual smoke screenshots.
- UsageStats permission flow is not fully guided in UI; if permission is missing, re-entry reminders may silently fail.
- Re-entry screen-off repeated reminders are **Needs verification / likely incomplete**, because current `FocusMonitorService` uses a `Handler` polling loop and no AlarmManager screen-off fallback.
- Full-screen reminder behavior depends on Android/device policy and must be manually verified.
- Notification permission denial has limited user-facing recovery.
- AppSettings are in-memory; whitelist/interval may not persist after process death.

**Reasonable future work:**
- Add Room database for `Task`, `Review`, `Settings`, `ReminderState`, `FocusSession`.
- Add ViewModel + Repository layer for lifecycle-safe state.
- Add Usage Access permission education screen.
- Add AlarmManager fallback receiver for re-entry reminders during screen-off.
- Add instrumentation/UI tests for Compose screens.
- Persist settings and whitelist.
- Add analytics/export for final project report data.

**Improvements compared with original proposal/design direction:**
- More active than a passive Pomodoro timer: planning, review, actual focus, re-entry nudges.
- Soft-lock concept with whitelist apps.
- Reflection reasons feed weekly insights.
- Start-time reminders support lockscreen/full-screen path and deferred reminder handling during active focus.
- UI follows Google/Material-style colors and components through `UiTokens.kt` and Material3 Compose.

## 9. Report Assets to Prepare

**Screenshots to take:**
1. Today screen with weekly timeline and sorted task cards.
2. Today screen with a completed strikethrough task.
3. Today screen with due alarm marker.
4. Date picker month grid.
5. Year/month wheel.
6. Create task sheet.
7. Plan page with priority dropdown.
8. Schedule editor with start-time toggle enabled.
9. Schedule editor with start-time disabled.
10. Focus timer screen.
11. Pause screen.
12. Finish confirmation dialog.
13. Exit confirmation dialog.
14. Needed apps menu in focus screen.
15. Re-entry notification after leaving app. **Needs verification**
16. Start-time reminder notification.
17. Lockscreen/full-screen reminder activity. **Needs verification**
18. Review tab with reason presets.
19. Insights weekly chart and common reasons.
20. Settings home.
21. Apps whitelist screen.
22. Add app dialog.
23. Notification interval screen.

**Diagrams/tables to include:**
- Architecture diagram: Compose UI → Domain Rules → Platform Adapters.
- Data model table for `PlannedTask`, `ActiveWork`, `AppSettings`.
- Main user-flow diagram: Plan → Start → Pause/Finish/Exit → Review → Insights.
- Reminder state machine: Scheduled → Active Due / Deferred → Acknowledged.
- Soft-lock flow: Focus active → foreground app check → whitelist/self/non-whitelist → notification.
- Testing matrix based on Section 6.