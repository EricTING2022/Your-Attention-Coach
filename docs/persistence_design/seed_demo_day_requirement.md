# Seed Demo Day Requirement

## Confirmed Decision

The app should not automatically seed demo tasks on first launch.

Instead, Settings should provide a `Seed demo day` action for demonstration and testing. This keeps the normal first-run experience clean while still allowing the project demo to quickly show a populated day.

## Expected User Flow

1. User opens `Settings`.
2. User taps `Seed demo day`.
3. The app inserts or refreshes the demo task set for May 5, 2026.
4. When the user returns to `Tasks`, the selected date should immediately switch to May 5, 2026.
5. The May 5 task list should be visible without requiring the user to manually open the calendar.

## Product Rationale

- Keeps the app from feeling like a toy on first launch.
- Preserves a fast demo path for grading and presentation.
- Makes the demo behavior explicit and user-controlled.

## Implementation Notes To Consider Later

- The action should be idempotent or clearly defined:
  - either replace the existing May 5 demo data;
  - or insert only missing demo tasks.
- The app should avoid creating duplicate demo tasks after repeated taps.
- Reminder scheduling should be consistent with whatever demo tasks are inserted.
