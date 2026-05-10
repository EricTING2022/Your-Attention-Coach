# Reference and Implementation Workflow

Use this workflow for every task in this feature batch.

## 1. Read Current Implementation

Before editing code, read the exact current implementation path:

- Start-time reminders:
  - `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
  - `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
- Focus re-entry:
  - `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
  - `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Insights:
  - `app/src/main/java/com/example/attentioncoach/domain/InsightRules.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Settings / Apps whitelist:
  - `app/src/main/java/com/example/attentioncoach/domain/AppSettings.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/NeededAppLauncher.kt`

## 2. Check References

Reference assets in this plan:

- Start-time reminder lock-screen behavior:
  - `01_start_time_reminders/assets/now_lockscreen_notifications.jpg`
- Focus re-entry notification behavior:
  - `02_focus_reentry/assets/now_reentry_notification.jpg`
- Current insights gap:
  - `03_weekly_insights/assets/now_insights_daily_summary.jpg`
- Settings target menu style:
  - `04_settings_whitelist/assets/reference_settings_menu.png`
- Latest user-uploaded Settings screenshots in chat supersede older settings layout notes. When implementing Settings home, match:
  - one rounded white card,
  - exactly two rows,
  - pale-blue circular icons,
  - right-side summary values,
  - chevrons,
  - Settings bottom navigation selected.
- Treat all screenshot values as examples only. `4 apps`, `Chrome`, `Docs`, package names, and `30s` must be rendered from current state, not hard-coded.
- Add tests for formatting dynamic Settings summaries before implementing UI.

Also consult frozen prototype references when touching visual hierarchy:

- `../app_design_freeze/snapshots/settings.png`
- `docs/prototype_reference_workflow.md`
- `../ui_prototype/index.html`
- `../ui_prototype/styles.css`
- `../ui_prototype/app.js`

## 3. TDD Before Implementation

For each subtask:

1. Write a focused failing unit test for the rule.
2. Run `.\gradlew.bat testDebugUnitTest` and confirm the expected failure.
3. Implement the minimum production change.
4. Run `.\gradlew.bat testDebugUnitTest assembleDebug`.
5. Run emulator smoke where the behavior is platform/UI dependent.
6. Commit only the files for that subtask.

## 4. Platform Caveats

Android does not let the app precisely force a heads-up banner to stay visible for exactly 15 seconds or use a custom banner height on every device.

The implementable target is the lock-screen notification card effect shown in the user reference:

- High-priority notification channel.
- Alarm category for scheduled focus reminders.
- Public lock-screen visibility.
- Sound/vibration/default alert behavior.
- RTC_WAKEUP exact alarm when permission allows.
- Repeated reminders until the user enters the task.
- No full-screen modal in this phase.

Do not claim full-screen or custom-duration banner behavior unless a full-screen intent task is explicitly approved and verified.
