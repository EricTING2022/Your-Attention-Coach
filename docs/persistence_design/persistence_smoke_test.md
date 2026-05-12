# Persistence Refactor Smoke Notes

Date: 2026-05-12

Branch/worktree: `feature/room-datastore-persistence`

## Automated Verification

- `testDebugUnitTest`: pass
- `assembleDebug`: pass
- `compileDebugAndroidTestKotlin`: pass for Room DAO instrumentation tests

## Implemented Persistence Paths

- Tasks are stored in Room through `tasks`.
- Reviews are stored in Room through one-to-one `task_reviews`.
- Settings are stored in DataStore Preferences.
- Active focus recovery state is stored in DataStore Preferences.
- Demo data is no longer seeded automatically on first launch.
- Settings includes `Seed demo day`, which refreshes demo-owned May 5 tasks and switches the Tasks screen to May 5.

## Manual Smoke Tests To Run On Device

1. Create a task, kill/reopen the app, and confirm the task remains.
2. Edit a task plan, kill/reopen the app, and confirm duration, start time, priority, and target remain.
3. Finish a focus block, save review text, kill/reopen the app, and confirm actual focus and review remain.
4. Change apps whitelist and notification interval, kill/reopen the app, and confirm settings remain.
5. Start a focus block, kill/reopen the app, and confirm the focus screen is restored.
6. Tap `Seed demo day`, return to Tasks, and confirm May 5 demo tasks are visible.
7. Tap `Seed demo day` repeatedly and confirm it does not duplicate demo tasks.

## Notes

- Room schema export is enabled under `app/schemas`.
- Future cloud sync metadata is intentionally limited to `createdAtMillis` and `updatedAtMillis` in this refactor.
- `deletedAtMillis`, `remoteId`, and `syncStatus` remain future TODOs.
