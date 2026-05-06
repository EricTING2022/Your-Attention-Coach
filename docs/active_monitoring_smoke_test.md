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
