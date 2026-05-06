# Active Monitoring Smoke Test Notes

Date: 2026-05-06

Device: `Pixel_7` AVD, `emulator-5554`

## Verified

- `adb devices` recognized the emulator after restarting `Pixel_7` with conservative emulator flags.
- Debug APK installed successfully with `adb install -r`.
- `POST_NOTIFICATIONS` was granted through ADB for smoke testing.
- `GET_USAGE_STATS` app-op was set to `allow` for smoke testing.
- Starting a Work block launched `FocusMonitorService` as a foreground service:
  - `isForeground=true`
  - `foregroundId=4520`
  - channel `active_work_block`
- Pause opened the Short Pause screen and stopped the foreground service.
- Continue focus returned to Work and restarted the foreground service.
- Exit opened one confirmation dialog.
- Cancel returned to Work.
- Continue returned to Today and stopped the foreground service.
- ACTION_REENTRY deep link opened the Re-entry screen for the provided task id.
- Resume task returned from Re-entry to Work.
- With Work active, launching `com.google.android.youtube` produced a re-entry notification:
  - title `Still your focus block?`
  - text `You planned to work on Android project skeleton. Tap to return.`
  - channel `reentry_reminder`
- Exiting Work after a re-entry notification stopped the service and cleared the re-entry banner.

## UsageStats Limits

- The emulator test used ADB to set `GET_USAGE_STATS` to `allow`.
- On a real user device, this permission cannot be requested with a normal runtime permission dialog. The app must guide the user to Android Usage Access settings.
- The service relies on `UsageStatsManager.queryEvents()`, so detection depends on the user granting Usage Access and on Android returning recent foreground events.
- This is still a soft-lock. It detects and prompts; it does not hard-block Home, Recents, or external apps.

## UI Differences Not Fixed In This Subtask

Per `docs/prototype_reference_workflow.md`, these differences should be handled in a separate UI alignment subtask after reading `../ui_prototype` source files:

- Today native header and vertical spacing are visibly larger than `../app_design_freeze/snapshots/today_default.png`.
- Bottom navigation still uses temporary letter-like icon rendering rather than the prototype's icon treatment.
- Task Detail appears as a narrower centered dialog on the emulator instead of a near-full-width bottom sheet matching the frozen prototype.
- Work screen functionally matches the required flow, but timer sizing and vertical distribution should be rechecked against `../ui_prototype/styles.css` before UI polish.

## 2026-05-06 Next Lifecycle Smoke Addendum

Build under test: debug APK built after commits through `4ac68b2`.

Device state:

- `adb devices` showed the Pixel_7 emulator as `emulator-5554`.
- A physical device was also connected, so emulator commands were run with `-s emulator-5554`.
- Current APK installed successfully on `emulator-5554`.

Verified:

- App launches to Today after reinstall.
- Add opens create-mode task detail with title, targets, duration, priority, planning note, and `Save task`.
- Priority picker opens from the read-only priority field after adding the transparent click overlay.
- Priority picker shows all four fixed options:
  - `Urgent & important`
  - `Urgent`
  - `Important`
  - `Not urgent`
- Selecting `Not urgent` updates the draft; saving creates `Untitled task` on Today with `30 min` and `Not urgent`.
- Delete overflow menu is available on an existing task.
- Delete confirmation appears, and confirming delete removes the created task from Today.
- Starting a Work block from a task opens focus mode with a live countdown from the task duration, plus `Pause`, `Finish`, and `Exit`.
- Pause opens the short pause screen with a 3-minute countdown (`02:58` observed after tapping).
- Resuming from pause returns to Work.
- Finish opens the first confirmation dialog: `Finish this task?`.

Not completed in this smoke pass:

- The second Finish confirmation and final Today-state verification were not completed because the follow-up ADB interaction was not approved in this run.
- Exit two-step confirmation was covered by unit/domain behavior and prior smoke for the old one-step flow, but the new two-step UI still needs an emulator click-through.
- Date year/month wheel was covered by unit tests and build verification, but not clicked through on emulator in this pass.
