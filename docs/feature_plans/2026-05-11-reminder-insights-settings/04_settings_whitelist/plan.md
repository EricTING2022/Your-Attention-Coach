# Settings and Apps Whitelist Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Settings becomes a Material-style menu with only two button rows: Apps whitelist and Notification interval. Apps whitelist is editable from installed launchable apps, and notification interval is edited through a scrollable picker with confirmation.

**Architecture:** Keep settings state small and explicit. Add a platform boundary that lists launchable installed apps, then map selected apps into `AppSettings`. Settings UI becomes a parent menu with child screens/sheets for whitelist and interval editing.

**Tech Stack:** Kotlin, PackageManager, Jetpack Compose Material3, SharedPreferences-ready settings model, JUnit 4.

---

## References

- Reference menu style: `assets/settings_reference.png`
- Reference layout within app whitelist setting: `apps_whitelist.png`
- reference layout within notification interval: `notification_interval.png`
- Latest user-uploaded Settings target screenshots in chat:
  - Settings home uses one large rounded white card.
  - The card contains exactly two full-width rows.
  - Row 1: pale-blue circular grid icon, `Apps whitelist`, trailing summary like `4 apps`, trailing chevron.
  - Row 2: pale-blue circular bell icon, `Notification interval`, trailing summary like `30s`, trailing chevron.
  - A thin divider separates the two rows inside the same card.
  - Large `Preferences` title sits above the card.
  - Bottom navigation remains visible with Settings selected.
- Screenshot values are examples only. `4 apps`, `Chrome`, `Docs`, package names, and `30s` must be derived from current state.
- Current current UI screenshot was provided in chat: large exposed cards for Needed apps and Notification interval.
- Current code:
  - `app/src/main/java/com/example/attentioncoach/domain/AppSettings.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/NeededAppLauncher.kt`

## Required Behavior

- Rename all user-facing `Needed apps` copy to `Apps whitelist`.
- Settings home shows only one card with two button rows, not exposed editable content:
  - `Apps whitelist`
  - `Notification interval`
- Settings home dynamic display rules:
  - Apps whitelist summary uses `settings.whitelistedApps.size`.
  - `0` renders as `No apps`.
  - `1` renders as `1 app`.
  - `n > 1` renders as `${n} apps`.
  - Notification interval summary is formatted from `settings.notificationIntervalSeconds`.
  - Examples: `30s`, `1 min`, `2 min`, `5 min`.
  - Do not hard-code `4 apps` or `30s` in the composable.
- Settings home row visual design:
  - Use a single white rounded card around both rows.
  - Each row height should feel tappable and spacious, close to the uploaded reference.
  - Left icon sits inside a pale-blue circular container.
  - Main label is bold, dark, and left aligned.
  - Current value is gray and right aligned before the chevron.
  - Chevron indicates row navigation.
  - The row itself is the button target; do not add separate text buttons on the home screen.
- Apps whitelist screen:
  - Shows currently selected apps.
  - Allows removing apps from the whitelist.
  - Allows adding from installed launchable apps.
  - Does not show every system package.
  - Uses package name as stable identity and app label as display text.
  - Shows a back/close control in the top row.
  - Shows each selected app as a rounded row with app label, package name in smaller gray text, and a remove icon/button.
  - Shows `Add app` as a primary blue button below the current list.
  - Tapping `Add app` opens a searchable or scrollable installed-app picker sheet.
  - Selecting an app adds it to the whitelist and returns to the whitelist editor.
  - Duplicate package names cannot be added.
  - Every visible app row is derived from `settings.whitelistedApps`.
  - App icon, label, and package name come from `InstalledAppsProvider` / PackageManager when available.
  - If PackageManager cannot resolve a saved package, show the saved label/package with a generic fallback icon instead of dropping the row.
  - If the whitelist is empty, show an empty state and the `Add app` button; do not show placeholder app rows.
- Notification interval screen:
  - Shows scrollable list of interval options.
  - User selects one option, then taps `Confirm`.
  - The selected interval feeds both start-time reminders and focus re-entry reminders.
  - The selected option is derived from `settings.notificationIntervalSeconds`; no option is selected by default unless it matches current settings.

## Files

- Modify: `app/src/main/java/com/example/attentioncoach/domain/AppSettings.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/InstalledAppsProvider.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/NeededAppLauncher.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/AppSettingsRulesTest.kt`

## Tasks

### Task 1: Add dynamic settings display rules

- [ ] Add `SettingsDisplayRules.whitelistSummary(count: Int)`.
- [ ] Test `0 -> No apps`.
- [ ] Test `1 -> 1 app`.
- [ ] Test `2 -> 2 apps`.
- [ ] Add `SettingsDisplayRules.intervalLabel(seconds: Int)`.
- [ ] Test `30 -> 30s`, `60 -> 1 min`, `120 -> 2 min`, `300 -> 5 min`.
- [ ] Commit: `test: cover dynamic settings labels`.

### Task 2: Rename model language

- [ ] Rename user-facing labels from `Needed apps` to `Apps whitelist`.
- [ ] Keep Kotlin class names only if renaming would create churn; otherwise introduce `WhitelistedApp` as a focused model.
- [ ] Add tests that add/remove uses package name identity.
- [ ] Commit: `feat: rename needed apps to apps whitelist`.

### Task 3: List launchable installed apps

- [ ] Add `InstalledAppsProvider`.
- [ ] Query apps with `Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER)`.
- [ ] Return label and package name.
- [ ] Return app icon or icon loader handle for Compose UI.
- [ ] Sort apps by label.
- [ ] Exclude Attention Coach itself from add candidates.
- [ ] Keep Chrome launch path working.
- [ ] Commit: `feat: list installed apps for whitelist`.

### Task 4: Build settings home UI with dynamic summaries

- [ ] Settings home uses the uploaded reference layout: one rounded white card, two rows, icon circle, value text, chevron.
- [ ] Apps whitelist row summary calls `SettingsDisplayRules.whitelistSummary(settings.whitelistedApps.size)`.
- [ ] Notification interval row summary calls `SettingsDisplayRules.intervalLabel(settings.notificationIntervalSeconds)`.
- [ ] No hard-coded counts, app names, package names, or interval labels in the home composable.
- [ ] Add Compose-friendly preview/test data only in previews, never in runtime UI.
- [ ] Commit: `feat: add dynamic settings menu`.

### Task 5: Build whitelist editor UI

- [ ] Settings home row opens Apps whitelist editor.
- [ ] Settings home must not show the app list directly.
- [ ] Settings home must not show add/remove controls directly.
- [ ] Current whitelist apps show delete/remove affordance.
- [ ] Current whitelist apps are read from `settings.whitelistedApps`, not from defaults.
- [ ] Each row shows actual app icon, actual label, actual package name, and `Remove`.
- [ ] Row layout follows the uploaded reference: icon left, label and package in the center, remove action right, dividers between rows.
- [ ] If only 2 apps are whitelisted, exactly 2 app rows appear and Settings home says `2 apps`.
- [ ] If 0 apps are whitelisted, no app rows appear and Settings home says `No apps`.
- [ ] Add button opens installed app picker.
- [ ] Selecting an app adds it and closes picker or returns to the whitelist editor.
- [ ] Removing an app immediately updates settings.
- [ ] The editor uses Material-style white cards on a light gray background, rounded rows, Google blue primary action, and clear secondary text for package names.
- [ ] Commit: `feat: edit apps whitelist from settings`.

### Task 6: Build notification interval picker

- [ ] Settings home row opens interval picker.
- [ ] Settings home must show only the current interval summary, for example `30s`, before the chevron.
- [ ] Use a scrollable list with options such as 30s, 1m, 2m, 5m.
- [ ] Selected row is highlighted.
- [ ] Confirm button saves the interval and returns to Settings home.
- [ ] Settings home only shows the current interval summary, not all interval options.
- [ ] Interval picker selected state is derived from current settings.
- [ ] After confirming `2 min`, Settings home immediately shows `2 min`.
- [ ] Commit: `feat: add notification interval picker`.

## Emulator Smoke

- [ ] Open Settings and confirm only `Apps whitelist` and `Notification interval` button-style menu rows are visible.
- [ ] Open Apps whitelist.
- [ ] Confirm Settings home count matches the actual whitelist size before and after add/remove.
- [ ] Add Chrome from installed apps if missing.
- [ ] Remove an app and confirm it disappears.
- [ ] Confirm only actual whitelisted apps appear; no placeholder `Chrome`/`Docs` rows are shown unless those apps are currently whitelisted.
- [ ] Start a focus block and confirm whitelist menu matches Settings.
- [ ] Open Notification interval, select another value, confirm, and verify the displayed summary updates.
