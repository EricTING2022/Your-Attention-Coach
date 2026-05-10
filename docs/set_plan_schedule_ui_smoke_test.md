# Set Plan Schedule UI Smoke Test Notes

Date: 2026-05-10

Build under test: debug APK after commit `0b99b10`.

## Reference Assets

- Plan page reference: `docs/design_references/set_plan_schedule_ui/generated_plan_page_reference_source.png`
- Schedule editor reference: `docs/design_references/set_plan_schedule_ui/generated_schedule_editor_reference.png`
- Notes: `docs/design_references/set_plan_schedule_ui/README.md`

## Verified By Tests And Build

- `testDebugUnitTest` passed.
- `assembleDebug` passed.
- Schedule persistence regression passed:
  - a created task can update `startTime` after creation;
  - a created task can update `durationMinutes` after creation.
- Schedule option tests passed:
  - hour options cover `0..23`;
  - minute options use 5-minute steps from `00` to `55`;
  - duration presets remain `15`, `30`, `45`, `60`, and `90`;
  - custom duration input keeps positive minute digits only.

## Implemented

- `Save schedule` in the editor now persists schedule edits immediately for existing tasks.
- `Save schedule` feedback is shown through a toast with text `Schedule is saved`.
- Schedule editor save control moved to the top-right green check.
- The bottom `Save schedule` button was removed.
- Start time selection changed from a single time list to two columns:
  - hour column;
  - minute column.
- The editor initializes the wheels near the existing task start time.
- Selected hour/minute values are highlighted.
- Duration controls changed from a horizontal scrolling row to a fixed two-row grid:
  - `15 min`, `30 min`, `45 min`;
  - `60 min`, `90 min`, custom input.
- Custom duration input has a fixed `min` suffix.
- Plan page schedule summary now uses a horizontal layout:
  - duration on the left;
  - start time on the right.
- Plan page field surfaces were softened to match the reference direction:
  - white rounded cards;
  - softer outlined text fields;
  - pill primary action;
  - Priority rendered as a chip-style card instead of a raw outlined input.

## Device / Emulator Status

- `adb devices` returned no connected devices in this pass.
- Because no emulator/device was available, the following remain build-verified but not click-through verified:
  - reopening a task after `Save schedule` visually shows the updated values;
  - the toast appears on-device;
  - the wheel scroll positions feel correct by touch;
  - the fixed duration grid fits the target viewport without visual crowding.

## Next Manual Checklist

- Install the debug APK on the Pixel_7 AVD.
- Create a task.
- Reopen the task.
- Open schedule editor from Duration.
- Change hour, minute, and duration.
- Tap the top-right green check.
- Confirm `Schedule is saved` appears.
- Close and reopen the same task.
- Confirm duration and start time persist visually.
- Reopen schedule editor.
- Confirm the hour/minute columns begin near the saved values and highlight them.
- Confirm there is no bottom save button.
- Confirm Plan page schedule summary is unclipped and close to the generated reference.
