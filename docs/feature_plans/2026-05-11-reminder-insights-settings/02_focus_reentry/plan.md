# Focus Re-entry Reminder Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** While a focus block is active, Attention Coach repeatedly reminds the user to return whenever the foreground app is not Attention Coach and not in Apps whitelist.

**Architecture:** Keep UsageStats polling inside `FocusMonitorService`, but make the notification behavior reliably repeat by tracking last notification time per active session and issuing fresh re-entry notifications after the configured interval. The whitelist comes from `AppSettings` and is passed into the service when the focus block starts.

**Tech Stack:** Kotlin, Foreground Service, UsageStatsManager, NotificationManager, SharedPreferences-backed settings, JUnit 4.

---

## References

- Current notification behavior screenshot: `assets/now_reentry_notification.jpg`
- Current code:
  - `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
  - `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`

## Required Behavior

- If a focus block is active and not paused, monitoring runs.
- If the foreground app is Attention Coach, do not notify.
- If the foreground app is in Apps whitelist, do not notify.
- If the foreground app is any other app, notify after the configured interval.
- If the user remains outside Attention Coach/whitelist, notify repeatedly every interval.
- If the user taps the banner and returns, the current reminder cycle stops.
- If the user leaves again, the reminder cycle starts again.
- Re-entry reminders use the same notification interval setting as scheduled reminders.

## Files

- Modify: `app/src/main/java/com/example/attentioncoach/domain/PlanningRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusMonitorService.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/ReentryNotifier.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/PlanningRulesTest.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/FocusMonitorRulesTest.kt`

## Tasks

### Task 1: Clarify re-entry decision rules

- [ ] Add tests for repeated notifications after interval.
- [ ] Add tests for suppression before interval.
- [ ] Add tests for whitelist suppression.
- [ ] Add tests for leaving again after returning.
- [ ] Implement minimal domain helpers if current `SoftLockPolicy` is too implicit.
- [ ] Commit: `test: cover repeated focus reentry rules`.

### Task 2: Make notifications reappear as new reminders

- [ ] In `ReentryNotifier`, avoid relying on updating a single persistent notification as the only signal.
- [ ] Either cancel before notify or use a session-scoped incrementing notification id.
- [ ] Keep the active foreground service notification separate from re-entry reminder notifications.
- [ ] Ensure tapping the re-entry notification still routes through `ACTION_REENTRY`.
- [ ] Commit: `feat: make reentry reminders repeat visibly`.

### Task 3: Integrate Apps whitelist

- [ ] Rename user-facing `Needed apps` copy to `Apps whitelist`.
- [ ] Keep WorkScreen shortcut list behavior, but label it as whitelist apps.
- [ ] Ensure `FocusMonitorService.start()` receives the current whitelist package names.
- [ ] Ensure Settings edits affect the next focus session.
- [ ] Commit: `feat: use apps whitelist for focus monitoring`.

## Emulator Smoke

- [ ] Start a focus block.
- [ ] Open Chrome from the WorkScreen whitelist menu and confirm no re-entry reminder appears.
- [ ] Leave to a non-whitelisted app and confirm a re-entry reminder appears.
- [ ] Ignore it for one interval and confirm another reminder appears.
- [ ] Tap the reminder and confirm the app returns to the focus/re-entry flow.
- [ ] Leave again and confirm reminders resume.

