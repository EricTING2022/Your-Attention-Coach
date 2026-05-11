# Softlock Re-entry Reminder Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make focus re-entry monitoring reliable while a focus block is active: Attention Coach clears the reminder state, whitelist apps pause reminders, and every non-whitelisted/unknown destination triggers repeat re-entry reminders until the user returns to Attention Coach.

**Architecture:** Keep the current foreground `FocusMonitorService` and `UsageStatsBoundary`, but stop treating UsageStats as the only source of truth. Add a small explicit re-entry state model, lifecycle signals from `MainActivity`, and a persisted single-shot alarm chain for screen-off repeat reminders. Reuse the start-time reminder pattern carefully: notification persistence and alarm chaining are reused, but full-screen re-entry is only used once for a screen-off violation chain.

**Tech Stack:** Kotlin, Android foreground service, `UsageStatsManager`, `AlarmManager`, `BroadcastReceiver`, `NotificationManager`, `SharedPreferences`, Jetpack Compose entry routing, JUnit 4 unit tests.

---

## Product Rules

Focus timer behavior:

- Focus timer continues while the user stays in Attention Coach, opens a whitelist app, opens a non-whitelisted app, goes home, opens recents, locks the screen, or turns the screen off.
- This plan does not change `ActiveWork`, `WorkSessionClock`, task completion, pause, finish, or review behavior.

Presence rules:

- `IN_ATTENTION_COACH`: no re-entry notification, no violation, cancel visible re-entry notification, cancel pending re-entry alarm, reset re-entry notification cooldown, and clear the uncleared re-entry state.
- `IN_WHITELIST_APP`: allowed temporary departure. Cancel visible re-entry notification and cancel/pause pending re-entry alarm, but do not clear the uncleared re-entry state if a violation already happened earlier in this focus session.
- `OUTSIDE_ALLOWED_SCOPE`: violation. Includes non-whitelisted apps, launcher/home, recents, unknown package, and `UsageStats` returning `null` while Attention Coach is known to be backgrounded.

Reminder timing:

- Leaving `MainActivity` during an active focus block starts a 3-second grace check.
- If the user is still outside Attention Coach and not in a whitelist app after grace, show the first re-entry notification immediately.
- The first re-entry notification is not blocked by the notification interval/cooldown.
- Later re-entry reminders respect `AppSettings.notificationIntervalSeconds`.

Screen-off behavior:

- Screen off is not itself a violation.
- If screen turns off while the current presence is `IN_ATTENTION_COACH` or `IN_WHITELIST_APP`, do not start re-entry reminders.
- If screen turns off while the current presence is `OUTSIDE_ALLOWED_SCOPE`, continue re-entry reminders with an `AlarmManager` single-shot chain.
- For screen-off violation chains, use option B: the first screen-off re-entry reminder may use a full-screen Activity route, and later repeats use high-priority notifications only.

Clear semantics:

- Only returning to `MainActivity` counts as fully clearing re-entry state.
- Tapping a re-entry notification routes to `MainActivity`; `MainActivity.onStart()` performs the same clear path as a manual app open.
- Notification dismiss, timeout, entering a whitelist app, and screen on/off do not fully clear re-entry state.

## Non-Goals

- Do not implement start-time reminders here.
- Do not change Settings UI or whitelist editor behavior except passing the current whitelist into monitoring.
- Do not redesign WorkScreen, ReentryScreen, task persistence, or review flows.
- Do not use emulator/ADB smoke testing as a required verification step in this task.
- Do not replace `UsageStatsBoundary` with AccessibilityService or a device admin style lock.
- Do not introduce Room/DataStore. Use `SharedPreferences` only for the minimal re-entry alarm receiver state.

## Current Problems To Clean Up

- `SoftLockPolicy.reentryDecision(...)` treats `foregroundPackage == null` as `SELF`, which suppresses reminders after the app is backgrounded.
- `FocusMonitorService` only polls UsageStats and does not receive `MainActivity.onStart/onStop` lifecycle signals.
- Current state is mostly `lastNotificationMillis`; it cannot represent Attention Coach clear vs whitelist pause vs outside violation.
- Returning to Attention Coach manually does not have a unified `clearReentryState(...)` path.
- `ReentryNotifier` uses `setTimeoutAfter(15_000L)`, so the notification object can disappear while the user still has not returned.
- There is no re-entry `AlarmManager` fallback chain for screen-off violation reminders.

## File Structure

Modify:

- `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
  - Add small re-entry domain enums/data classes if needed.
- `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
  - Replace implicit self/null handling with explicit presence and notification decision helpers.
- `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
  - Add focused tests for presence, first reminder, cooldown, whitelist pause, and null handling.
- `app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt`
  - Add cadence/grace constants tests if new constants are introduced.
- `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
  - Notify `FocusMonitorService` when Attention Coach enters/leaves foreground.
- `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
  - Keep starting/stopping monitor from active focus state; pass current whitelist and interval as today.
- `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
  - Own lifecycle actions, re-entry state transitions, grace timer, screen receiver registration, persisted state writes, and alarm scheduling/canceling.
- `app/src/main/java/com/example/attentioncoach/platform/UsageStatsBoundary.kt`
  - Keep as a helper for latest foreground package; add launcher package helper if useful.
- `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
  - Remove notification timeout and add optional full-screen intent support for first screen-off violation reminder only.
- `app/src/main/AndroidManifest.xml`
  - Register the new re-entry alarm receiver and optional lockscreen Activity.

Create:

- `app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt`
  - Receives the persisted single-shot alarm and decides whether to notify/reschedule.
- `app/src/main/java/com/example/attentioncoach/platform/ReentryMonitorStateStore.kt`
  - Small SharedPreferences wrapper for active re-entry session state.
- `app/src/main/java/com/example/attentioncoach/platform/ReentryLockscreenActivity.kt`
  - Optional minimal full-screen route used only for the first screen-off violation reminder.

## Implementation Priority

1. Domain rules and tests first. The wrong `null == SELF` behavior must fail in tests before implementation changes.
2. Lifecycle clear/background signals next. The 3-second grace requirement depends on knowing that `MainActivity` left foreground.
3. Notification lifetime and unified clear path next. This prevents stale/vanishing notifications while state work lands.
4. Screen-off alarm chain last. It depends on the state model, clear semantics, and notifier API being stable.

## Chunk 1: Domain Re-entry Semantics

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
- Modify: `app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt`

- [ ] **Step 1: Add failing tests for explicit presence**

Add tests covering:

- Attention Coach foreground resolves to safe presence and no notification.
- Whitelist package resolves to whitelist presence and no notification.
- Non-whitelisted package resolves to outside presence and first notification.
- `foregroundPackage == null` with Attention Coach backgrounded resolves to outside presence.
- `foregroundPackage == "com.example.attentioncoach"` with Attention Coach backgrounded is not enough to count as safe, because UsageStats may be stale.

Target shape:

```kotlin
assertEquals(
    FocusPresence.OUTSIDE_ALLOWED_SCOPE,
    SoftLockPolicy.resolvePresence(
        attentionCoachInForeground = false,
        foregroundPackage = null,
        whitelistPackages = setOf("com.android.chrome"),
        appPackage = "com.example.attentioncoach"
    )
)
```

- [ ] **Step 2: Add failing tests for first reminder and cooldown**

Cover:

- first violation notification is allowed when `lastReentryNotificationAt == null`;
- repeated notification inside interval is suppressed;
- repeated notification after interval is allowed;
- whitelist presence suppresses notification even if `hasUnclearedReentryViolation == true`.

- [ ] **Step 3: Implement minimal domain model**

Add:

```kotlin
enum class FocusPresence {
    IN_ATTENTION_COACH,
    IN_WHITELIST_APP,
    OUTSIDE_ALLOWED_SCOPE
}
```

Keep the state split explicit:

```kotlin
data class ReentryMonitorSnapshot(
    val presence: FocusPresence,
    val hasUnclearedReentryViolation: Boolean,
    val lastReentryNotificationAt: Long?
)
```

Update `SoftLockPolicy` or add focused helpers in the same file:

```kotlin
fun resolvePresence(
    attentionCoachInForeground: Boolean,
    foregroundPackage: String?,
    whitelistPackages: Set<String>,
    appPackage: String = "com.example.attentioncoach"
): FocusPresence

fun shouldNotifyReentry(
    presence: FocusPresence,
    nowMillis: Long,
    lastReentryNotificationAt: Long?,
    intervalMillis: Long,
    bypassCooldown: Boolean
): Boolean
```

Preserve existing callers until `FocusMonitorService` is migrated, or update the old tests at the same time.

- [ ] **Step 4: Add grace constant**

In `FocusMonitorCadence`, add:

```kotlin
const val APP_EXIT_GRACE_MILLIS = 3_000L
```

Keep `POLL_INTERVAL_MILLIS` unchanged unless tests prove a change is required.

- [ ] **Step 5: Run focused verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/Models.kt app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt
git commit -m "test: cover focus reentry state rules"
```

## Chunk 2: Lifecycle Signals and Unified Clear

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`

- [ ] **Step 1: Add service actions for app foreground/background**

In `FocusMonitorService`, add actions:

```kotlin
ACTION_ATTENTION_COACH_FOREGROUND
ACTION_ATTENTION_COACH_BACKGROUND
ACTION_CLEAR_REENTRY
```

Add public helpers:

```kotlin
fun markAttentionCoachForeground(context: Context)
fun markAttentionCoachBackground(context: Context)
fun clearReentryState(context: Context)
```

- [ ] **Step 2: Wire `MainActivity` lifecycle**

In `MainActivity.onStart()`:

```kotlin
FocusMonitorService.markAttentionCoachForeground(this)
```

In `MainActivity.onStop()`:

```kotlin
FocusMonitorService.markAttentionCoachBackground(this)
```

Keep the existing `appEnteredAtMillis` start-time reminder hook.

- [ ] **Step 3: Implement unified clear path**

In `FocusMonitorService`, implement one private method:

```kotlin
private fun clearReentryState(reason: ReentryClearReason)
```

It must:

- set `attentionCoachInForeground = true`;
- set `lastPresence = FocusPresence.IN_ATTENTION_COACH`;
- set `hasUnclearedReentryViolation = false`;
- set `lastReentryNotificationAt = null`;
- cancel visible re-entry notification;
- cancel pending re-entry alarm;
- persist the cleared state.

Use this for `ACTION_ATTENTION_COACH_FOREGROUND`, `ACTION_CLEAR_REENTRY`, focus stop, and notification-click entry.

- [ ] **Step 4: Keep notification click routing simple**

`ReentryNotifier` should still route to `MainActivity` with `ACTION_REENTRY`.

`MainActivity.captureReentryIntent(...)` may continue setting `reentryTaskId`, but cooldown reset should be replaced by the foreground clear path. Do not keep a separate "notification click only" reset path.

- [ ] **Step 5: Implement background grace trigger**

When `ACTION_ATTENTION_COACH_BACKGROUND` arrives during an active focus session:

- set `attentionCoachInForeground = false`;
- cancel any existing grace runnable;
- schedule a handler runnable after `FocusMonitorCadence.APP_EXIT_GRACE_MILLIS`;
- the grace runnable resolves presence and, if outside, sends the first re-entry notification with `bypassCooldown = true`.

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/MainActivity.kt app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt
git commit -m "feat: clear reentry state on app return"
```

## Chunk 3: Presence State Machine in `FocusMonitorService`

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/UsageStatsBoundary.kt`

- [ ] **Step 1: Expand service state minimally**

Add fields:

```kotlin
private var attentionCoachInForeground = false
private var lastPresence = FocusPresence.IN_ATTENTION_COACH
private var hasUnclearedReentryViolation = false
private var lastReentryNotificationAt: Long? = null
private var lastKnownForegroundPackage: String? = null
private var screenOn = true
private var screenOffFullScreenShownForViolation = false
```

Replace `lastNotificationMillis` with `lastReentryNotificationAt`.

- [ ] **Step 2: Update polling to resolve presence first**

`scanForegroundApp()` should:

1. return if there is no active session;
2. get `foregroundPackage` from `UsageStatsBoundary`;
3. resolve `presence` using `attentionCoachInForeground`, `foregroundPackage`, and session whitelist;
4. pass presence to a single transition method.

Suggested method:

```kotlin
private fun handlePresenceResolved(
    presence: FocusPresence,
    foregroundPackage: String?,
    nowMillis: Long,
    bypassCooldown: Boolean
)
```

- [ ] **Step 3: Implement transition behavior**

For `IN_ATTENTION_COACH`:

- call `clearReentryState(...)`.

For `IN_WHITELIST_APP`:

- set `lastPresence = FocusPresence.IN_WHITELIST_APP`;
- cancel visible re-entry notification;
- cancel pending re-entry alarm;
- do not reset `hasUnclearedReentryViolation`;
- do not reset `lastReentryNotificationAt` unless product testing shows immediate repeat after leaving whitelist feels better.

For `OUTSIDE_ALLOWED_SCOPE`:

- set `lastPresence = FocusPresence.OUTSIDE_ALLOWED_SCOPE`;
- set `hasUnclearedReentryViolation = true`;
- if `shouldNotifyReentry(...)` returns true, show notification and update `lastReentryNotificationAt`;
- if screen is off, ensure the single-shot alarm chain is scheduled.

- [ ] **Step 4: Keep launcher handling simple**

Add a helper to `UsageStatsBoundary` only if needed:

```kotlin
fun launcherPackages(): Set<String>
```

Use Android home intent resolution:

```kotlin
Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
```

Do not special-case launcher as safe. Launcher packages are outside allowed scope unless they are explicitly whitelisted, which they should normally not be.

- [ ] **Step 5: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt app/src/main/java/com/example/attentioncoach/platform/UsageStatsBoundary.kt
git commit -m "feat: track focus reentry presence"
```

## Chunk 4: Re-entry Notification Lifetime

**Files:**

- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`

- [ ] **Step 1: Remove re-entry notification auto-timeout**

Remove:

```kotlin
.setTimeoutAfter(REENTRY_TIMEOUT_MILLIS)
```

Delete `REENTRY_TIMEOUT_MILLIS` if unused.

- [ ] **Step 2: Keep replace-notification behavior**

Keep:

```kotlin
notificationManager.cancel(REENTRY_NOTIFICATION_ID)
notificationManager.notify(REENTRY_NOTIFICATION_ID, ...)
```

This makes repeat reminders visibly refresh without creating many notifications.

- [ ] **Step 3: Add optional full-screen parameter without using it yet**

Prepare a notifier API such as:

```kotlin
fun showReentryBanner(
    taskId: Long,
    taskTitle: String,
    useFullScreen: Boolean = false
)
```

If `useFullScreen == false`, behavior remains a normal high-priority re-entry notification.

Do not add full-screen behavior in this chunk unless the next chunk is implemented immediately.

- [ ] **Step 4: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt
git commit -m "fix: keep reentry notifications visible"
```

## Chunk 5: Persisted Re-entry Alarm Chain

**Files:**

- Create: `app/src/main/java/com/example/attentioncoach/platform/ReentryMonitorStateStore.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add minimal persisted state store**

Create `ReentryMonitorStateStore` using `SharedPreferences`.

Persist only:

```kotlin
focusActive: Boolean
taskId: Long
taskTitle: String
whitelistPackages: Set<String>
notificationIntervalMillis: Long
attentionCoachInForeground: Boolean
presenceName: String
hasUnclearedReentryViolation: Boolean
lastReentryNotificationAt: Long
screenOffFullScreenShownForViolation: Boolean
```

Use a session/task id guard so stale alarms for old tasks do nothing.

- [ ] **Step 2: Write state from service transitions**

`FocusMonitorService` should persist state when:

- monitoring starts;
- monitoring stops;
- Attention Coach foreground/background changes;
- presence changes;
- a re-entry notification is shown;
- screen-off full-screen usage is recorded;
- re-entry state is cleared.

- [ ] **Step 3: Add receiver**

Create `ReentryReminderReceiver : BroadcastReceiver`.

`onReceive(...)` should:

1. load persisted state;
2. return and cancel if `focusActive == false`;
3. return and cancel if `attentionCoachInForeground == true`;
4. return and cancel if `presence != OUTSIDE_ALLOWED_SCOPE`;
5. show a re-entry notification;
6. update `lastReentryNotificationAt`;
7. schedule the next single-shot alarm after `notificationIntervalMillis`.

Do not try to infer a new foreground app inside the receiver. The receiver uses persisted state; the service updates state while alive.

- [ ] **Step 4: Add schedule/cancel helpers**

Add focused helpers either in `ReentryReminderReceiver.Companion` or a small private section of `FocusMonitorService`:

```kotlin
fun scheduleNext(context: Context, triggerAtMillis: Long, taskId: Long)
fun cancel(context: Context, taskId: Long)
```

Use:

```kotlin
alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
```

Fallback to `alarmManager.set(...)` on `SecurityException`, matching the start-time reminder style.

- [ ] **Step 5: Register receiver**

In `AndroidManifest.xml`:

```xml
<receiver
    android:name=".platform.ReentryReminderReceiver"
    android:exported="false" />
```

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/ReentryMonitorStateStore.kt app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt app/src/main/AndroidManifest.xml
git commit -m "feat: repeat reentry reminders while screen off"
```

## Chunk 6: Screen Events and One-Time Full-Screen Re-entry

**Files:**

- Create: `app/src/main/java/com/example/attentioncoach/platform/ReentryLockscreenActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Register screen receiver dynamically in service**

In `FocusMonitorService.onCreate()`, dynamically register a `BroadcastReceiver` for:

```kotlin
Intent.ACTION_SCREEN_OFF
Intent.ACTION_SCREEN_ON
Intent.ACTION_USER_PRESENT
```

Unregister in `onDestroy()`.

Do not declare screen on/off in manifest; Android only delivers these reliably to dynamically registered receivers.

- [ ] **Step 2: Screen-off handling**

On `ACTION_SCREEN_OFF`:

- set `screenOn = false`;
- persist `screenOn`-derived state if stored;
- if active session and `lastPresence == OUTSIDE_ALLOWED_SCOPE`, schedule the next re-entry alarm;
- do not create a new violation if last presence was safe or whitelist.

- [ ] **Step 3: Screen-on handling**

On `ACTION_SCREEN_ON` or `ACTION_USER_PRESENT`:

- set `screenOn = true`;
- let the normal service loop resolve presence;
- do not clear re-entry unless `MainActivity.onStart()` arrives.

- [ ] **Step 4: Create minimal lockscreen activity**

Create `ReentryLockscreenActivity` similar to `ReminderActivity`, but copy should be re-entry specific:

- title: `Return to focus`
- body: task title
- button: `Open Attention Coach`

Button launches `MainActivity` with `ReentryNotifier.ACTION_REENTRY` and the task id, then finishes.

- [ ] **Step 5: Register lockscreen activity**

In `AndroidManifest.xml`:

```xml
<activity
    android:name=".platform.ReentryLockscreenActivity"
    android:excludeFromRecents="true"
    android:exported="false"
    android:showWhenLocked="true"
    android:theme="@style/Theme.AttentionCoach"
    android:turnScreenOn="true" />
```

- [ ] **Step 6: Use full-screen only once per screen-off violation chain**

In `ReentryNotifier`, add a full-screen pending intent to `ReentryLockscreenActivity` only when `useFullScreen == true`.

In `FocusMonitorService` or `ReentryReminderReceiver`, pass `useFullScreen = true` only when:

- screen is off;
- current persisted presence is `OUTSIDE_ALLOWED_SCOPE`;
- `screenOffFullScreenShownForViolation == false`.

After showing it once, persist `screenOffFullScreenShownForViolation = true`.

All later repeats use high-priority notifications with `useFullScreen = false`.

- [ ] **Step 7: Reset full-screen once safe**

Reset `screenOffFullScreenShownForViolation = false` when:

- `clearReentryState(...)` runs;
- presence becomes `IN_WHITELIST_APP`;
- a new focus session starts.

- [ ] **Step 8: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/ReentryLockscreenActivity.kt app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add one-time lockscreen reentry prompt"
```

## Chunk 7: Final Cleanup and Basic Debug

**Files:**

- Modify only files touched above if cleanup is needed.
- Optional doc update: `docs/active_monitoring_smoke_test.md`

- [ ] **Step 1: Remove obsolete cooldown reset API**

If no longer used, remove:

```kotlin
FocusMonitorService.resetReentryCooldown(...)
ACTION_RESET_REENTRY_COOLDOWN
```

Do not keep two paths for the same state reset.

- [ ] **Step 2: Search for old wrong logic**

Run:

```powershell
rg -n "foregroundPackage == null|ReentryReason.SELF|setTimeoutAfter|RESET_REENTRY_COOLDOWN|lastNotificationMillis" app\src\main app\src\test
```

Expected:

- no re-entry notification timeout remains;
- no `null` foreground package is treated as self while Attention Coach is backgrounded;
- old reset cooldown API is gone or intentionally unused only in tests being migrated;
- `lastNotificationMillis` is replaced with `lastReentryNotificationAt`.

- [ ] **Step 3: Add debug logs behind focused tags**

Add small `Log.d(...)` statements in `FocusMonitorService` and `ReentryReminderReceiver` for:

- focus monitor start/stop;
- app foreground/background;
- presence transition;
- first notification vs cooldown repeat;
- alarm schedule/cancel;
- receiver ignored reason.

Keep logs concise and remove noisy per-field dumps.

- [ ] **Step 4: Basic debug verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: PASS.

Then do manual basic debug only, no emulator/ADB requirement:

- Start a focus block.
- Press Home or switch to a non-whitelisted app.
- Wait 3 seconds plus a small scheduling margin.
- Confirm a re-entry notification appears.
- Return to Attention Coach manually.
- Confirm re-entry notification disappears and does not repeat.
- Start focus again, open a whitelist app, and confirm no re-entry notification appears.
- From whitelist, switch to a non-whitelisted app and confirm a re-entry notification appears.
- While already outside allowed scope, turn the screen off and confirm repeat notification behavior continues; the first screen-off repeat may use full-screen, later repeats should be notification-only.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt app/src/main/java/com/example/attentioncoach/platform/ReentryReminderReceiver.kt app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt docs/active_monitoring_smoke_test.md
git commit -m "chore: clean up reentry monitor state"
```

## Acceptance Criteria

Unit/debug acceptance:

- `.\gradlew.bat testDebugUnitTest assembleDebug` passes.
- Tests prove `foregroundPackage == null` is outside allowed scope when Attention Coach is backgrounded.
- Tests prove first outside reminder bypasses cooldown.
- Tests prove repeated reminders obey notification interval.
- Tests prove whitelist presence suppresses notification but does not necessarily clear uncleared re-entry state.

Behavior acceptance:

- Focus timer continues while the app is backgrounded, while a whitelist app is open, while a non-whitelisted app is open, and while the screen is off.
- Returning to `MainActivity` by notification click or manual app open fully clears re-entry state.
- Opening a whitelist app cancels visible re-entry notification and pauses pending reminders, but does not count as final re-entry acknowledgement.
- Leaving Attention Coach and remaining outside whitelist after 3 seconds sends the first re-entry reminder immediately.
- Home/launcher/recents/unknown/null foreground state is treated as outside allowed scope.
- Re-entry notification no longer auto-removes itself via `setTimeoutAfter`.
- Screen-off does not create a new violation from a safe/whitelist state.
- Screen-off after an existing violation continues reminders through the alarm chain.
- Only the first screen-off violation repeat may use full-screen; later repeats are high-priority notifications only.

Scope acceptance:

- No start-time reminder behavior is changed except shared patterns being reused conceptually.
- No Settings redesign, task model migration, Room/DataStore, or AccessibilityService is introduced.
- Existing focus start/finish/pause flows remain intact.
