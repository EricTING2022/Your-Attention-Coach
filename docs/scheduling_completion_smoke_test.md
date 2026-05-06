# Scheduling And Completion Smoke Test Notes

Date: 2026-05-06

Build under test: debug APK after commits through `a561288`.

## Verified By Unit Tests And Build

- `testDebugUnitTest` passed.
- `assembleDebug` passed.
- New tasks default to a blank review reason instead of `Attention faded`.
- Work Exit and Finish still preserve the domain distinction:
  - Exit does not mutate task status or actual focus.
  - Finish records actual active focus.
- Calendar helpers use `java.time`:
  - `today(clock)` follows the provided/system clock.
  - May 7, 2026 is Thursday.
  - May 2026 has 31 days.
- Task completion toggle rules:
  - Planned task toggles to `FINISHED`.
  - Manual completion does not invent actual focus time.
  - Toggling again returns to `PLANNED` and clears review fields.
- Today list sorter keeps paused tasks first and completed tasks last without a completed-section data model.
- Schedule options:
  - 5-minute start-time options cover the full day.
  - Duration options are `15`, `30`, `45`, `60`, and `90` minutes.
- Reminder rules:
  - Trigger time is calculated from task date, start time, and zone.
  - Tasks without start time are not schedulable.

## Implemented UI / Platform Paths

- Week timeline indicator dots were removed.
- Today task list no longer renders a divider between unfinished and finished tasks.
- Task entry right arrow was replaced by a hollow completion circle.
- Completed task titles render with strikethrough.
- Plan page no longer exposes Planning note.
- Plan page order is now Duration, Priority, Targets.
- Duration opens a schedule editor with start time and duration options.
- `SCHEDULE_EXACT_ALARM` was added to the manifest.
- A reminder scheduler, exact alarm permission helper, and reminder receiver were added.
- Saving a scheduled task asks the user to open exact alarm settings when exact alarm access is unavailable.

## Not Fully Device-Smoked In This Pass

- The emulator was not relaunched for this final pass; verification here is unit/build level.
- Exact alarm permission behavior still needs device confirmation because Android exposes it through special app access settings, not a normal runtime permission dialog.
- Actual reminder delivery also needs device/emulator confirmation with notification permission granted and exact alarm access enabled.
- The hollow-circle interaction, one-step dialogs, schedule editor scrolling, and strikethrough rendering should be checked visually in the next emulator UI pass.

## Next Emulator Checklist

- Launch Today and confirm it opens on the system date.
- Confirm the week timeline has no blue/pink dots.
- Tap a task's hollow circle; title becomes struck through.
- Tap the circle again; title returns to normal and Review is hidden.
- Tap task body; detail sheet opens.
- Confirm Plan page has Duration, Priority, and Targets only.
- Tap Duration; schedule editor opens and scrolls through start times.
- Save a task with start time; confirm exact alarm prompt appears if access is unavailable.
- Start work and confirm Finish and Exit each show one confirmation dialog.
