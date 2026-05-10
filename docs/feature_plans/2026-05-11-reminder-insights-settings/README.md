# Reminder, Re-entry, Insights, and Settings Reliability Plan

Date: 2026-05-11

This folder freezes the next implementation batch after user testing found gaps in reminder reliability, focus re-entry, weekly insights, and settings editing.

## Product Decisions

1. Insights must use the same week as the Tasks timeline: Sunday through Saturday for the currently selected date.
2. "Needed apps" is renamed to "Apps whitelist" everywhere.
3. Apps whitelist must be editable: users can add launchable installed apps and remove apps from the whitelist.
4. The whitelist must be the single source of truth for:
   - Work screen app shortcuts.
   - FocusMonitorService soft-lock exceptions.
   - Settings display.
5. Settings display values must never be hard-coded from mockups. App count, app labels, package names, and interval labels are derived from current app state and PackageManager.
6. Start-time reminders must not fire for past task dates or past start times.
7. Start-time reminders must repeat after the configured notification interval until the user taps the notification and enters the task.
8. Start-time reminders should produce the lock-screen visible notification-card effect shown in the reference: high-importance alarm-style notification, public lock-screen visibility, sound/vibration, and RTC_WAKEUP alarms. Do not implement a full-screen modal for this phase.
9. Focus re-entry reminders must repeat while a focus block is active and the foreground app is neither Attention Coach nor a whitelisted app.
10. UI should follow official Google app / Material visual language: light surfaces, clear button-like rows, readable type, restrained color, and predictable navigation.

## Folder Map

- `01_start_time_reminders/plan.md`: reliable scheduled start-time reminders, lock-screen visibility, click acknowledgement, and past-date protection.
- `02_focus_reentry/plan.md`: repeated re-entry reminders during active focus and notification interval integration.
- `03_weekly_insights/plan.md`: dynamic weekly planned-vs-actual chart and common reason summary.
- `04_settings_whitelist/plan.md`: Settings home with only Apps whitelist and Notification interval, plus child editors.

## Reference Workflow

Before implementation, each task must use `workflow.md` in this folder.

Required verification before each commit:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Each implemented subtask gets its own commit. Do not bundle unrelated UI polish with reminder/service logic.
