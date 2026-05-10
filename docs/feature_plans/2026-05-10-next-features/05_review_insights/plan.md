# Plan: Review Reason Presets and Weekly Insights

## Goal

Make review reasons easier to collect and implement a first useful Insights screen.

## Review Reason UI

Provide preset reason options:

- Attention faded
- Task was unclear
- Duration was unrealistic
- Interrupted by another task
- Used entertainment app
- Other

If the user chooses Other, show a free-text field.

The user can still write a custom reason. The app should not force a default reason.

## Data Model

The current `PlannedTask.mismatchReason: String` can remain the stored value for the first implementation.

Optional later model:

```kotlin
enum class ReviewReasonType {
    ATTENTION_FADED,
    TASK_UNCLEAR,
    UNREALISTIC_DURATION,
    INTERRUPTED,
    ENTERTAINMENT_APP,
    OTHER
}
```

For this course project, keeping a string is simpler and sufficient unless analytics needs stricter categories.

## Insights Scope

Initial Insights should cover the most recent 7 days, including the selected date.

Show:

- Total planned focus minutes.
- Total actual focus minutes.
- Difference between planned and actual.
- Common review reasons ranked by count.

Suggested calculations:

```text
planned = sum(durationMinutes)
actual = sum(actualFocusMinutes for finished/reviewed tasks)
difference = actual - planned
```

Only tasks with a non-blank review reason should count toward common reasons.

## Implementation Notes

Files to inspect and likely edit:

- `app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt`
- `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
- `app/src/main/java/com/example/attentioncoach/domain/InsightRules.kt` or similar new file
- `app/src/test/java/com/example/attentioncoach/domain/InsightRulesTest.kt`

`InsightsScreen` currently needs access to the task list. If it only receives static display state, update the app shell so Insights can compute from tasks in the current in-memory store.

## Tests

Add or update tests for:

- Review reason starts blank.
- Selecting a preset saves that label.
- Selecting Other saves the custom text.
- Weekly planned minutes include tasks from the last 7 days.
- Weekly actual minutes include finished/reviewed actual focus.
- Common reasons are counted and sorted by frequency.

## Commit

Suggested commits:

1. `feat: add review reason presets`
2. `feat: add weekly insight summary`

Before each commit:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

