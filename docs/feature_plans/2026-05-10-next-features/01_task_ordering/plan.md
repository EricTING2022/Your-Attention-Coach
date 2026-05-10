# Plan: Priority-Based Task Ordering

## Goal

Sort the main task list by priority. If two tasks have the same priority, the earlier-created task should stay above the later-created task.

## Proposed Behavior

Use this order:

1. Higher priority before lower priority.
2. Earlier-created task before later-created task.

Priority rank:

1. `Urgent & important`
2. `Urgent`
3. `Important`
4. `Not urgent`

Completed tasks remain in the same list, but their title is visually struck through. There is no divider section.

Completed tasks must not be pushed down. If a high-priority task is completed, it stays above lower-priority unfinished tasks.

`Paused` is not a task-list state. Pause only belongs to the active focus session.

## Implementation Notes

Files to inspect and likely edit:

- `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
- `app/src/main/java/com/example/attentioncoach/model/TaskPriority.kt`

`PlannedTask.id` is currently the best available creation-order proxy. If later persistence is added, a real `createdAt` field would be cleaner, but adding that now is not necessary.

## Tests

Add or update unit tests for:

- Urgent and important tasks sort above urgent tasks.
- Urgent tasks sort above important tasks.
- Same-priority tasks keep increasing `id` order.
- Finished/reviewed tasks do not move below unfinished tasks.
- Demo seed data does not contain `TaskStatus.PAUSED`.

## Commit

Suggested commit:

`feat: sort tasks by priority`

Before commit:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```
