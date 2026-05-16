# Softlock Re-entry V2 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild softlock re-entry on a clean `main` baseline so Attention Coach can reliably distinguish Attention Coach, apps whitelist, launcher, and non-whitelist apps during an active focus timer.

**Architecture:** Implement the feature in validation layers. The first layer proves the foreground signal on a real device before any notification behavior depends on it. Accessibility is the primary foreground observer; UsageStats remains a diagnostic fallback only.

**Tech Stack:** Kotlin, Jetpack Compose, Android Foreground Service, AccessibilityService, UsageStatsManager fallback, AlarmManager, Notifications, DataStore, JUnit.

---

## Product Goal

During an active focus session, the timer must continue running even when the user leaves Attention Coach, opens an apps whitelist app, locks the screen, or turns the screen off.

Re-entry reminders should follow these rules:

| User state during active focus | Reminder behavior |
|---|---|
| Attention Coach focus timer page | Never remind. |
| Apps whitelist app | Never remind. |
| Launcher / home screen | Remind after grace and repeat by interval. |
| Non-whitelist app | Remind after grace and repeat by interval. |
| Screen off after Attention Coach | Do not remind. |
| Screen off after whitelist app | Do not remind. |
| Screen off after launcher / non-whitelist app | Continue reminders through alarm-based repeat. |

Returning to Attention Coach fully clears the re-entry violation state. Entering a whitelist app pauses/mutes reminders, but it is not a final acknowledgement. If the user later goes from whitelist to launcher or another non-whitelist app, reminders should resume.

The user already accepts enabling Accessibility permission. Therefore full softlock behavior may explicitly require the app's Accessibility service to be enabled.

## Why This Is V2

The previous branch, `feature/softlock-reentry-reminder`, should be kept as a research and failure-analysis worktree. It contains useful logs, manual tests, and partial code, but it also accumulated several corrective layers before the foreground source was proven on a real device.

Important lessons from V1:

- `UsageStatsManager` can return `null` while the user is in Chrome, launcher, or another app.
- A short whitelist handoff can protect a transition, but it cannot prove the user remains inside a whitelist app.
- `UNKNOWN` must not be treated as a reliable product state.
- Screen-off logic must preserve the last reliable presence; screen off itself must not create a violation.
- Emulator success is useful but insufficient. Real-device logs are the acceptance source for foreground detection and lockscreen behavior.

V2 starts from `main` and adds one validated layer at a time.

## Worktree Policy

Current worktrees:

- Keep: `.worktrees/softlock-reentry-reminder`
- New active worktree: `.worktrees/softlock-reentry-v2`
- New branch: `feature/softlock-reentry-v2`

Do not delete the V1 worktree. Use it only as a reference for:

- failure analyses under `docs/s06_test_results/`;
- manual test ideas;
- notification and AlarmManager examples.

Do not copy V1 state-machine code wholesale.

## Source Boundaries

Expected files to modify or create during implementation:

- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/xml/attention_coach_accessibility_service.xml`
- Create: `app/src/main/java/com/example/attentioncoach/platform/AttentionCoachAccessibilityService.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ForegroundObservationStore.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ForegroundPresenceResolver.kt` or keep pure rules in `domain/PlanningRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Create later: `app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt`
- Create later: `app/src/main/java/com/example/attentioncoach/platform/ReentryMonitorStateStore.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
- Docs: `docs/softlock_reentry_v2_manual_test.md`
- Docs: `docs/softlock_reentry_v2_real_device_results.md`

Keep files focused. If `FocusMonitorService.kt` starts growing past a readable boundary, split platform-specific pieces into small classes instead of adding more private state.

## Layer 0: Clean Baseline

**Goal:** Prove the new worktree starts from a stable main baseline before adding softlock code.

**Files:**

- Create: `docs/softlock_reentry_v2_manual_test.md`

- [ ] **Step 1: Record baseline**

Run:

```powershell
git status --short --branch
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected:

- branch is `feature/softlock-reentry-v2`;
- no code changes yet;
- tests and debug build pass.

- [ ] **Step 2: Smoke test current app**

Manual test:

- start a focus timer;
- verify focus timer keeps running;
- verify pause, finish, exit still work;
- verify settings whitelist still loads from DataStore.

- [ ] **Step 3: Commit baseline docs**

```powershell
git add docs/softlock_reentry_v2_manual_test.md
git commit -m "docs: add softlock reentry v2 baseline tests"
```

## Layer 1: Primary Foreground Observer

**Goal:** Prove on a real device that the app can observe the current foreground package for Attention Coach, whitelist apps, launcher, and non-whitelist apps.

This layer must not implement re-entry notification logic yet.

**Files:**

- Create: `app/src/main/res/xml/attention_coach_accessibility_service.xml`
- Create: `app/src/main/java/com/example/attentioncoach/platform/AttentionCoachAccessibilityService.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ForegroundObservationStore.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
- Create/modify tests where pure parsing is testable.

**Design rules:**

- Accessibility is the primary source.
- Prefer `rootInActiveWindow?.packageName` as the primary foreground app signal.
- Use `event.packageName` only when the root package is missing.
- If needed, add `windows` fallback with interactive-window retrieval.
- Store only package name, source, and timestamp.
- Do not inspect or store screen text.
- Add logs with tag `AC_ForegroundV2`.
- Log `eventType`, `eventPackage`, `rootPackage`, `windowPackages`, `chosenPackage`, and timestamp.

**Accessibility service config should enable enough official capability to observe windows:**

- `android:canRetrieveWindowContent="true"`
- include `flagRetrieveInteractiveWindows` if `windows` fallback is used;
- listen to `typeWindowStateChanged`, `typeWindowsChanged`, and possibly `typeWindowContentChanged` with throttling.

- [ ] **Step 1: Add failing resolver/store tests**

Cover:

- fresh observation is returned;
- stale observation is ignored;
- null package is ignored;
- source is recorded.

- [ ] **Step 2: Implement observer and store**

Keep the store tiny. Do not couple it to `FocusMonitorService` yet.

- [ ] **Step 3: Add Settings status**

Show whether Accessibility foreground detection is enabled. Do not redesign settings beyond a small status/action row if needed.

- [ ] **Step 4: Build**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Real-device foreground test**

Install the APK and enable the Accessibility service.

Capture:

```powershell
adb logcat -c
adb logcat -v time -s AC_ForegroundV2
```

Manual flow:

1. Open Attention Coach.
2. Open Chrome.
3. Press Home / launcher.
4. Open a non-whitelist app.
5. Return to Attention Coach.
6. Turn screen off from Attention Coach.
7. Turn screen off from Chrome.

Pass condition:

- Chrome emits Chrome package.
- Launcher emits launcher package.
- Non-whitelist app emits its package.
- Attention Coach emits app package or is otherwise identifiable through lifecycle.
- Screen-off does not erase the last reliable package.

If this layer fails on real device, stop. Do not implement later layers. Diagnose Accessibility service enablement/config first.

**Layer 1 real-device refinement:** Samsung real-device logs showed that `eventPackage` can be a stale or transient event source, for example launcher events while Attention Coach is still the active root window, or Attention Coach events while launcher is the active root window. Therefore the V2 design uses `rootPackage` first, `eventPackage` second, and `windowPackages` last.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main AndroidManifest.xml docs/softlock_reentry_v2_real_device_results.md
git commit -m "feat: add softlock foreground observer"
```

## Layer 2: Presence Classifier

**Goal:** Convert raw foreground package observations into stable product states.

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt`

**States:**

```kotlin
enum class FocusPresence {
    IN_ATTENTION_COACH,
    IN_WHITELIST_APP,
    IN_LAUNCHER,
    IN_OTHER_APP,
    UNKNOWN
}
```

**Rules:**

- Attention Coach lifecycle foreground wins.
- app package -> `IN_ATTENTION_COACH`
- whitelist package -> `IN_WHITELIST_APP`
- home package -> `IN_LAUNCHER`
- other non-null package -> `IN_OTHER_APP`
- null/stale package -> `UNKNOWN`

`UNKNOWN` is not a valid substitute for whitelist or launcher. It means detection is degraded.

- [ ] **Step 1: Write classifier tests**

Cover all five states, stale data, and launcher package lists.

- [ ] **Step 2: Implement classifier**

Keep the classifier pure and unit-testable.

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 4: Real-device classification log**

Add temporary or permanent diagnostic log in the monitor/viewmodel:

```text
rawPackage=<package> presence=<presence> source=<source>
```

Pass condition:

- Manual flows from Layer 1 now show correct classified presence.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain app/src/test/java/com/example/attentioncoach/domain docs/softlock_reentry_v2_real_device_results.md
git commit -m "feat: classify foreground presence"
```

## Layer 3: Screen-on Re-entry

**Goal:** While the screen is on, remind only when presence is launcher or non-whitelist app.

**Layer 2 adjustment before implementation:** real-device testing proved that `FocusPresence` is reliable while the screen is on, and System UI / lock-screen events should not be treated as user app switches. Therefore Layer 3 uses only the Layer 2 effective `FocusPresence` and skips screen-on policy when the device is not interactive. Screen-off reminders remain Layer 4 work. After Layer 3 smoke testing, the cadence was tuned to `POLL_INTERVAL_MILLIS = 3_000L` and `REENTRY_GRACE_MILLIS = 2_000L` so the first reminder usually appears within about 3-5 seconds while keeping polling moderate.

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Modify tests in `FocusMonitorRulesTest.kt`

**Behavior:**

- On focus start, service starts as foreground service.
- Service polls latest effective `FocusPresence`.
- If presence is `IN_ATTENTION_COACH`, clear re-entry violation.
- If presence is `IN_WHITELIST_APP`, clear visible notification but do not count as final acknowledgement.
- If presence is `IN_LAUNCHER` or `IN_OTHER_APP`, start a 2-second grace and then notify.
- Repeated reminders follow the settings notification interval.
- If presence is `UNKNOWN`, show degraded detection status in logs. Do not hardcode Chrome or assume whitelist.

- [ ] **Step 1: Write rule tests**

Cover:

- first launcher violation notifies after grace;
- whitelist suppresses reminders;
- returning to Attention Coach clears;
- unknown does not masquerade as whitelist.

- [ ] **Step 2: Implement minimal service logic**

Avoid carrying over V1's handoff-heavy logic. Use presence as the input.

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 4: Real-device screen-on test**

Manual flow:

1. Focus -> launcher: reminder after grace.
2. Focus -> Chrome whitelist: no reminder.
3. Launcher -> Chrome whitelist: reminder mutes/stops.
4. Chrome -> launcher: reminder resumes.
5. Non-whitelist app: reminder.
6. Return to Attention Coach: reminder clears.

Pass condition:

- Behavior matches product goal without using Needed-app handoff as proof.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform app/src/test/java/com/example/attentioncoach/domain docs/softlock_reentry_v2_manual_test.md docs/softlock_reentry_v2_real_device_results.md
git commit -m "feat: add screen-on reentry policy"
```

## Layer 4: Screen-off Re-entry

**Goal:** Screen-off behavior follows the last reliable presence.

**Layer 4 adjustment before implementation:** Layer 2/3 testing showed that screen-off turns the raw foreground source into System UI / lock-screen events. Therefore Layer 4 never derives a new violation from `com.android.systemui`. It persists the last effective `FocusPresence` from screen-on monitoring and lets the alarm receiver make decisions from that state.

**Files:**

- Create: `app/src/main/java/com/example/attentioncoach/platform/ReentryMonitorStateStore.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Behavior:**

- Screen off from `IN_ATTENTION_COACH`: no reminder.
- Screen off from `IN_WHITELIST_APP`: no reminder.
- Screen off from `IN_LAUNCHER`: schedule alarm reminders.
- Screen off from `IN_OTHER_APP`: schedule alarm reminders.
- Screen off from `UNKNOWN`: preserve last reliable state and log degraded status. Do not invent a new violation from screen off alone.
- Returning to screen-on monitoring cancels the pending screen-off alarm.
- Stopping focus monitoring clears persisted re-entry state and pending screen-off alarms.

- [ ] **Step 1: Add state-store tests where practical**

Verify state serialization is narrow and does not duplicate task/settings persistence.

- [ ] **Step 2: Implement persisted re-entry state**

Persist only:

- focus active;
- task id/title;
- whitelist package set;
- notification interval;
- last reliable presence;
- last notification time;
- screen-off reminder state.

- [ ] **Step 3: Implement alarm receiver**

Use single-shot alarm chaining. Do not rely on background polling while screen is off.

- [ ] **Step 4: Build**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Real-device screen-off test**

Manual flow:

1. Focus page -> screen off: no reminder.
2. Chrome whitelist -> screen off: no reminder.
3. Launcher -> screen off: reminder repeats by interval.
4. Non-whitelist app -> screen off: reminder repeats by interval.
5. Return to Attention Coach: alarm and notification cancel.

Pass condition:

- No reminder from allowed states.
- Repeat reminder from disallowed states.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform app/src/main/AndroidManifest.xml app/src/test docs/softlock_reentry_v2_real_device_results.md
git commit -m "feat: add screen-off reentry alarms"
```

## Layer 5: Notification and Lockscreen UX

**Goal:** Re-entry reminders route directly back to the focus timer without a separate re-entry page.

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Optional create: `app/src/main/java/com/example/attentioncoach/platform/ReentryLockscreenActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`

**Behavior:**

- Notification tap opens Attention Coach and resumes the active focus timer.
- Full-screen/lockscreen reminder button opens the focus timer with one tap.
- The old Reentry page should not be routed to during normal use.
- Notification uses high importance and lockscreen-visible channel.

- [ ] **Step 1: Add navigation tests where possible**

Verify re-entry intent maps to focus timer route.

- [ ] **Step 2: Implement notification routing**

Do not add extra UI unless needed for lockscreen.

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 4: Real-device UX test**

Manual flow:

1. Trigger screen-on re-entry reminder.
2. Tap banner.
3. Confirm one tap returns to focus timer.
4. Trigger screen-off/full-screen reminder.
5. Tap resume/open.
6. Confirm one tap returns to focus timer.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach app/src/test docs/softlock_reentry_v2_real_device_results.md
git commit -m "feat: route reentry reminders to focus timer"
```

## Layer 6: Cleanup and Report Evidence

**Goal:** Remove temporary diagnostics that are too noisy, keep useful diagnostic logs, and update report/test docs.

**Files:**

- Modify: `docs/softlock_reentry_v2_manual_test.md`
- Modify: `docs/softlock_reentry_v2_real_device_results.md`
- Optional modify: final report material if needed.

- [ ] **Step 1: Audit logs**

Keep concise logs:

- `AC_ForegroundV2` for foreground observer;
- `AC_ReentryV2` for presence transitions and notifications.

Remove excessive per-frame/event logs after verification.

- [ ] **Step 2: Final verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 3: Full manual matrix**

Run the final matrix:

| ID | Flow | Expected |
|---|---|---|
| V2-S01 | Focus -> launcher | reminder after grace |
| V2-S02 | Focus -> whitelist | no reminder |
| V2-S03 | Whitelist -> launcher | reminder resumes |
| V2-S04 | Launcher -> whitelist | reminder mutes |
| V2-S05 | Focus -> non-whitelist | reminder |
| V2-S06 | Focus -> screen off | no reminder |
| V2-S07 | Whitelist -> screen off | no reminder |
| V2-S08 | Launcher -> screen off | repeat reminder |
| V2-S09 | Non-whitelist -> screen off | repeat reminder |
| V2-S10 | Notification tap | returns directly to focus timer |

- [ ] **Step 4: Commit**

```powershell
git add docs app/src/main/java app/src/test
git commit -m "test: record softlock reentry v2 verification"
```

## Success Criteria

The feature is considered ready only when:

- Layer 1 foreground observation passes on the user's real device.
- Launcher and non-whitelist are distinguishable from whitelist in logs.
- No code path depends on hardcoded Chrome behavior.
- `UsageStatsManager` is documented and implemented as fallback only.
- Screen-off behavior depends on last reliable presence, not on screen-off itself.
- All unit tests pass.
- `assembleDebug` passes.
- Manual real-device matrix is documented.

## Non-goals

- Do not implement hard blocking or kiosk mode.
- Do not use Device Owner / MDM APIs.
- Do not inspect user content through Accessibility.
- Do not infer whitelist state from a long handoff timer.
- Do not merge V1 softlock code wholesale.
- Do not redesign task, review, or settings UI except for minimal Accessibility status/help text.

## Open Risks

- Some OEMs may restrict Accessibility events or lockscreen full-screen notifications.
- Android permission dialogs and settings screens cannot be fully automated in unit tests.
- Exact alarm behavior can vary if the user denies exact alarm capability.
- If Accessibility cannot produce package names on the real device even with window-content retrieval, full softlock accuracy is not feasible for a normal app without stronger device-management privileges.

## Layer 4.1 Adjustment: Lockscreen Route And Screen-off Polling Pause

Layer 4 passed the state-machine tests, but real-device testing showed two platform-level follow-ups:

- unsafe screen-off reminders were delivered as notifications, but did not surface as a lockscreen full-screen route;
- safe screen-off still allowed the service polling loop to wake every poll interval and clear the safe state repeatedly.

Minimal implementation direction:

- keep the V2 `FocusPresence` state machine unchanged;
- reuse the V1-style `ReentryLockscreenActivity` only as a lockscreen route;
- add `setFullScreenIntent(...)` only for the first unsafe screen-off reminder in a violation chain;
- keep later repeats as normal high-priority notifications;
- pause the `FocusMonitorService` polling loop while screen-off;
- use the existing AlarmManager chain for unsafe screen-off repeats;
- resume polling on `ACTION_SCREEN_ON`.

Verification:

- `FocusMonitorLoopPolicy.shouldPausePolling(false)` is unit-tested;
- `ReentryFullscreenPolicy.shouldUseFullScreen(...)` is unit-tested;
- full behavior still requires real-device lockscreen testing because Android/OEM full-screen notification policy is device-dependent.

## Implementation Discipline

- One layer per implementation round.
- One commit per completed layer.
- Run `testDebugUnitTest` and `assembleDebug` before each commit.
- After each layer, stop and wait for real-device test feedback before moving to the next layer.
- Prefer small pure functions for state rules and unit-test them before wiring platform code.
