# Image Generation Prompts

Use built-in image generation only for visual alignment previews. These images are references, not implementation artifacts.

## Preview 1: Weekly Insights

Use case: ui-mockup
Asset type: Android mobile app screen mockup
Primary request: Create a polished Attention Coach Insights screen following official Google app / Material design.
Scene/backdrop: portrait phone screen, light gray app background.
Subject: weekly insights page with two cards.
Style/medium: clean high-fidelity UI mockup.
Composition/framing: full mobile screen, bottom navigation visible.
Color palette: Google blue, soft red, white cards, gray page background.
Text: "This week", "Planned vs actual", "Common reasons", "Attention faded", "Entertainment app distraction", "Task too large", "Duration was unrealistic", "Interrupted by another task".
Constraints: 7 day chart from Sunday to Saturday; planned bars blue; actual bars red; no numeric labels on top of bars; reason list includes all default selectable reasons with counts, including zero-count rows; no hard-coded-looking single-day summary; no dark theme; no marketing copy.

## Preview 2: Settings Apps Whitelist

Use case: ui-mockup
Asset type: Android mobile app screen mockup
Primary request: Create a polished Attention Coach Settings flow preview following official Google app / Material design.
Scene/backdrop: portrait phone screen, light gray app background.
Subject: Settings home with exactly one rounded white card containing two button rows, plus an Apps whitelist editor.
Style/medium: clean high-fidelity UI mockup.
Composition/framing: show either split stacked states or one screen focused on Apps whitelist.
Color palette: white cards, Google blue selected accents, gray secondary text.
Text: "Preferences", "Apps whitelist", "2 apps", "Notification interval", "30s", "Chrome", "Docs", "com.android.chrome", "com.google.android.apps.docs.editors.docs", "Add app", "Remove".
Constraints: Settings home shows exactly one rounded white card with exactly two rows; each row has a pale-blue circular icon on the left, bold label, gray right-side dynamic summary, and chevron; the visible count must match the shown whitelist rows, for example if two apps are shown the summary says \"2 apps\"; no exposed app list or interval chips on home; whitelist supports add and remove in its child editor; app rows show actual label and package name; notification interval is edited in a scrollable list child screen; bottom navigation visible with Settings selected.

## Preview 3: Reminder Notification Behavior

Use case: ui-mockup
Asset type: Android lock-screen notification mockup
Primary request: Create a lock-screen style Attention Coach reminder preview.
Scene/backdrop: soft blurred Android lock screen.
Subject: Attention Coach high-priority reminder notification and focus re-entry notification.
Style/medium: realistic Android notification UI mockup.
Composition/framing: portrait lock-screen notification shade, two Attention Coach notifications visible.
Color palette: soft Material notification cards, Google blue icon accents.
Text: "Scheduled focus time", "Room schema implementation", "Focus block running", "Tap to return".
Constraints: show lock-screen visible notifications; do not show full-screen modal; no exact phone brand UI copying.
