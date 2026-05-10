# Attention Coach Next Feature Feasibility Review

Date: 2026-05-10

This folder records the next feature batch before implementation. Each feature has its own folder so the work can be implemented, verified, and committed independently.

## Current Findings

The current task list order is defined in `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt` by `TaskListSorter.sortForToday()`.

Current behavior:

1. Paused tasks are shown first.
2. Finished or reviewed tasks are pushed lower.
3. Tasks with the same status group are ordered by `id`, which roughly follows seed/create order.

It does not currently sort by priority.

## Feasibility Summary

All requested features are feasible, with one Android-system caveat around notification banners:

- Android does not let an app precisely force a heads-up notification to stay visible for exactly 15 seconds, use the full screen width, or use a custom height on every device.
- We can make the reminder much more reliable by using a high-importance notification channel, alarm-style notifications, repeat scheduling, task-specific click routing, and optional full-screen intent for screen-off or lock-screen cases.
- Screen-off delivery also depends on notification permission, exact alarm permission, device battery policy, and OEM behavior. The app should request the required permissions and show clear setup status in Settings.

Recommended implementation order:

1. Task priority ordering.
2. Optional start time toggle in schedule editing.
3. Settings data model for needed apps and notification interval.
4. Persistent start-time reminders and focus re-entry reminders.
5. Review reason presets and weekly Insights.

## Folder Map

- `01_task_ordering/plan.md`: priority-based task ordering.
- `02_optional_start_time/plan.md`: start-time on/off switch and collapsed time picker.
- `03_persistent_reminders/plan.md`: repeated start-time reminder notification behavior.
- `04_soft_lock_needed_apps/plan.md`: re-entry reminders, needed app launch, and Settings redesign.
- `05_review_insights/plan.md`: review reason presets and weekly Insights.

## Open Product Decisions

These are safe to decide before implementation:

1. Completed tasks should probably stay visually lower only through strikethrough styling, not a separate section. The proposed sort keeps unfinished tasks above completed tasks, then sorts each group by priority.
2. "Jump to the task's pre-start page" is interpreted as opening the task detail Plan page, not starting the timer immediately.
3. Needed apps should list launchable installed apps only, not every system package, to keep Settings understandable and privacy-friendly.

