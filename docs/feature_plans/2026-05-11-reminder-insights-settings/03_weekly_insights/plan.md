# Weekly Insights Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Insights dynamically shows the same Sunday-to-Saturday week selected in the Tasks timeline, with a daily planned-vs-actual bar chart and common review reasons for that week.

**Architecture:** Move Insights from a single aggregate summary to a seven-day model. Domain code computes `DailyInsight` rows and common reason counts; Compose renders two Material-style cards.

**Tech Stack:** Kotlin, Java Time, Jetpack Compose Canvas/Box layout, JUnit 4.

---

## References

- Current daily-looking Insights screenshot: `assets/now_insights_daily_summary.jpg`
- Target direction from user: planned bars in blue, actual bars in red, 7 days visible, no numeric labels on top of bars, common reasons below.
- Current code:
  - `app/src/main/java/com/example/attentioncoach/domain/InsightRules.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
  - `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`

## Required Behavior

- Week range is `WeekTimeline.weekFor(selectedDate)`.
- If selected date is May 6, 2026, the Insights week is May 3 to May 9, 2026.
- Planned minutes per day: sum of task duration for that date.
- Actual minutes per day: sum of actual focus minutes for finished/reviewed tasks for that date.
- Common reasons: count non-blank review reasons from tasks in that same week.
- The chart shows all seven days even if a day has no tasks.
- The chart uses blue for planned and red for actual.
- Do not hard-code chart values, day values, session counts, or reason counts.
- Do not show numeric labels on top of bars.
- Common reasons must include all default selectable review reasons, even when count is zero, so the user sees a stable reason vocabulary.

## Files

- Modify: `app/src/main/java/com/example/attentioncoach/domain/InsightRules.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/InsightRulesTest.kt`

## Tasks

### Task 1: Fix data flow

- [ ] Add test proving selected May 6 uses May 3 to May 9.
- [ ] Add test proving tasks outside that week are excluded.
- [ ] Add test proving no day values are hard-coded by using varied task data across different weeks.
- [ ] Add `DailyInsight(date, plannedMinutes, actualMinutes)`.
- [ ] Add `WeeklyInsight(days, commonReasons, sessionCount)`.
- [ ] Change `AppShell` to pass all tasks into `InsightsScreen`, not only selected-day tasks.
- [ ] Commit: `fix: sync insights to selected week`.

### Task 2: Render weekly chart

- [ ] Replace the current text aggregate cards with a `PlannedActualChartCard`.
- [ ] Render seven day columns.
- [ ] Use light blue background tracks and blue planned bars.
- [ ] Use red actual bars, offset or nested so both values are visible.
- [ ] Do not render numeric labels above bars.
- [ ] Keep day labels below bars.
- [ ] Keep typography and spacing close to Google/Material style.
- [ ] Commit: `feat: add weekly planned actual chart`.

### Task 3: Render common reasons card

- [ ] Render reason rows with colored bullets.
- [ ] Render counts on the right.
- [ ] Always render all default review reasons from `ReviewReasonOptions.presets`, excluding `Other` unless free-text "Other" reasons exist.
- [ ] Reasons with no matching reviews show count `0`.
- [ ] Sort default reasons in the fixed product order, not by count, unless the user later asks for count sorting.
- [ ] Commit: `feat: show weekly common reasons`.

## Emulator Smoke

- [ ] Select May 5 and open Insights.
- [ ] Select May 6 and open Insights.
- [ ] Confirm both dates show the same week if they are in the same Sunday-to-Saturday range.
- [ ] Confirm planned/actual chart has seven day columns.
- [ ] Confirm common reasons update after saving a review reason.
