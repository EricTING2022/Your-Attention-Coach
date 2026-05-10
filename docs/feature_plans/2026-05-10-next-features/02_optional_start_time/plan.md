# Plan: Optional Start Time Toggle

## Goal

Let users create or edit a task without a start time. The schedule editing page should show a binary sliding toggle next to Start time.

Reference image:

![Sliding switch](assets/sliding_button.jpg)

## Proposed Behavior

- Default state: start time enabled, switch on the right, green.
- Tap once: switch moves left, becomes gray, and the start-time wheel collapses.
- Tap again: switch moves right, becomes green, and the start-time wheel expands.
- When disabled, save `startTime = null`.
- When enabled, save the selected `LocalTime`.
- Duration remains editable even when start time is disabled.

## Data Model

No new task field is needed. `PlannedTask.startTime: LocalTime?` already supports this:

- `null`: no scheduled start reminder.
- non-null: scheduled start reminder.

## Scheduler Behavior

When start time is disabled:

- Cancel any existing start-time alarm for that task.
- Do not schedule a new reminder.

When start time is re-enabled or changed:

- Schedule a new alarm for the selected time.
- Clear reminder acknowledgement state for that task and start time.

## Implementation Notes

Files to inspect and likely edit:

- `app/src/main/java/com/example/attentioncoach/ui/ScheduleEditScreen.kt`
- `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- `app/src/main/java/com/example/attentioncoach/notifications/TaskReminderScheduler.kt`
- `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`

The toggle should be a custom Compose control rather than the default Android `Switch`, because the visual target is closer to the reference image and current app style.

## Tests

Add or update tests for:

- Disabling start time stores `null`.
- Disabled start time does not schedule a reminder.
- Re-enabling start time schedules a reminder.
- Changing start time resets reminder acknowledgement.

## Commit

Suggested commit:

`feat: make task start time optional`

Before commit:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

