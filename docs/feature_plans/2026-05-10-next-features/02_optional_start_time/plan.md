# Plan: Optional Start Time Toggle

## Goal

Let users create or edit a task without a start time. The schedule editing page should show a binary sliding toggle inline with the Start Time control.

Reference image:

![Sliding switch](assets/sliding_button.jpg)

## Proposed Behavior

### Start time

- Default state: start time enabled, switch on the right, green.
- Place the switch in the same rounded row as `START TIME`, aligned to the right. Do not create a separate card only for the toggle.
- Show the currently selected start time in the same row when enabled.
- Tap once: switch moves left, becomes gray, and the start-time wheel collapses.
- Tap again: switch moves right, becomes green, and the start-time wheel expands.
- When disabled, save `startTime = null`.
- When enabled, save the selected `LocalTime`.
- Duration remains editable even when start time is disabled.

### Custom duration

- Keep preset duration chips visible: `15 min`, `30 min`, `45 min`, `60 min`, `90 min`, and `Custom`.
- Tapping `Custom` opens a bottom sheet instead of expanding controls inside the main schedule sheet.
- The bottom sheet contains two scrollable wheels: `Hour` and `Minute`.
- Initial custom duration value is `0 hr : 00 min`.
- At the minimum value, do not show negative or previous values above `0`; the wheel should start at `0`.
- The selected row uses the existing pale-green highlight.
- Unit labels `hr` and `min` stay inside the highlighted selected row, next to the current number, and do not scroll independently.
- The bottom sheet has a close button and a single `Apply` button. Applying updates the selected duration and returns to the schedule sheet.

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

Use a modal bottom sheet for custom duration. Implement the hour and minute pickers as bounded scrollable lists with `0` as the first item. Render the unit labels as fixed text inside the selected-row highlight, not as list items.

## Tests

Add or update tests for:

- Disabling start time stores `null`.
- Disabled start time does not schedule a reminder.
- Re-enabling start time schedules a reminder.
- Changing start time resets reminder acknowledgement.
- Custom duration cannot scroll below `0 hr : 00 min`.
- Applying custom duration updates the selected duration value.

## Commit

Suggested commit:

`feat: make task start time optional`

Before commit:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

