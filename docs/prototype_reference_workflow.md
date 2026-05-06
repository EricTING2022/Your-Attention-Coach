# Prototype Reference Workflow

Use `../ui_prototype` as the source of truth for UI implementation, not only as a screenshot generator. The frozen snapshots remain the visual acceptance targets, but Android Compose work should begin from the prototype source files so spacing, colors, hierarchy, and interaction details are not guessed from images.

## Source Order

1. Read `../ui_prototype/index.html` to identify the screen structure, component nesting, and which UI belongs to top-level screens, sheets, focus mode, or overlays.
2. Read `../ui_prototype/styles.css` to map design tokens and layout measurements into Compose:
   - CSS variables in `:root` -> `UiTokens`;
   - `week-planner`, `week-row`, and `week-day` -> Today timeline;
   - `daily-brief` -> summary cards;
   - `task-card`, `task-title`, `task-meta`, and `chip` -> task entry cards and chips;
   - `segmented-control`, `field`, `duration-control`, and `priority-menu` -> Plan/Review form surfaces;
   - `focus-layout`, `timer-orbit`, `focus-actions`, and `confirm-dialog` -> Work, Pause, and Exit flows;
   - `bottom-nav` plus `phone.focus-mode` / `phone.sheet-mode` rules -> navigation visibility.
3. Read `../ui_prototype/app.js` to map interaction and state behavior:
   - seed tasks and priority/status values;
   - task grouping and completed divider logic;
   - week swipe and selected date updates;
   - month picker and year/month picker behavior;
   - Plan/Review tab switching;
   - duration numeric parsing with fixed `min`;
   - priority dropdown updates;
   - Work, Pause, Exit, and Re-entry transitions.
4. Compare the resulting Android screen against `../app_design_freeze/snapshots/*.png`.

## Compose Mapping Rules

- Preserve the prototype hierarchy before tuning visuals. For example, Today should remain: sticky week timeline -> summary cards -> Tasks title/Add -> grouped task list -> bottom navigation.
- Use prototype CSS measurements as the first Compose draft. Convert px to dp approximately one-to-one, then adjust only when the emulator screenshot proves Android density or system UI requires it.
- Keep chip colors and label colors tied to prototype class names. Do not invent a new priority palette unless the prototype changes first.
- Match the prototype's visibility modes:
  - top-level screens show bottom navigation;
  - task detail and re-entry behave like sheet mode and hide bottom navigation;
  - work and pause behave like focus mode and hide bottom navigation.
- When a Compose implementation visually diverges from a snapshot, inspect the CSS rule first before changing Compose by eye.

## Verification Loop

For every UI subtask:

1. Name the prototype source sections used, including CSS selectors and JS functions.
2. Implement the smallest Compose change that maps those sections.
3. Run `testDebugUnitTest` for behavior changes and `assembleDebug` for all UI changes.
4. Install on emulator/device when available and capture screenshots.
5. Compare screenshots against `../app_design_freeze/snapshots`.
6. Record material differences in the work notes before committing.

This keeps the native app aligned with the design that was already reviewed in the local browser prototype.
