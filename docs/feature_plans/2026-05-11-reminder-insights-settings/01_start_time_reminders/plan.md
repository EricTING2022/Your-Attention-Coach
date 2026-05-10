# Start-Time Reminder Reliability Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scheduled start-time reminders fire only for valid future task times, create a lock-screen visible notification-card effect, repeat at the configured interval, and stop only after the user enters the task from the notification.

**Architecture:** Add a small reminder state layer around AlarmManager. Domain rules decide whether a task reminder is schedulable and when the next reminder should fire; platform code stores acknowledgement and schedules/cancels alarms. Notification click routing opens the target task and acknowledges the reminder.

**Tech Stack:** Kotlin, Java Time, AlarmManager, BroadcastReceiver, NotificationManager, SharedPreferences, Jetpack Compose, JUnit 4.

---

## References

- Current lock-screen notification screenshot: `assets/now_lockscreen_notifications.jpg`
- Current code:
  - `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
  - `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
  - `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`

## Required Behavior

- If `task.date + task.startTime <= now`, do not schedule a start-time reminder.
- If the task date is in the past, never schedule a reminder even if the time-of-day is in the future.
- When the reminder fires, show a high-priority, alarm-category notification.
- The notification must be visible on the lock screen as public content, matching the reference notification-card effect rather than a full-screen modal.
- If the user does not tap the notification, schedule the next reminder after `settings.notificationIntervalSeconds`.
- If the user taps the notification, open the task's Plan page and mark the reminder acknowledged.
- Once acknowledged, do not remind again for that exact task date and start time.
- If the task start time changes, clear the old acknowledgement and schedule the new reminder if it is in the future.

## Files

- Modify: `app/src/main/java/com/example/attentioncoach/domain/ReminderRules.kt`
- Create: `app/src/main/java/com/example/attentioncoach/domain/ReminderState.kt`
- Create: `app/src/main/java/com/example/attentioncoach/platform/ReminderStateStore.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/MainActivity.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
- Test: `app/src/test/java/com/example/attentioncoach/domain/ReminderRulesTest.kt`

## Tasks

### Task 1: Add schedulability rules

- [ ] Add tests proving past dates and past date-times are not schedulable.
- [ ] Add tests proving future date-times are schedulable.
- [ ] Add tests for `nextRepeatAt(now, intervalSeconds)`.
- [ ] Implement `ReminderRules.scheduleDecision(taskDate, startTime, now, zoneId)`.
- [ ] Verify with `.\gradlew.bat testDebugUnitTest assembleDebug`.
- [ ] Commit: `fix: prevent past task reminders`.

### Task 2: Persist reminder acknowledgement

- [ ] Create `ReminderKey(taskId, date, startTime)`.
- [ ] Create `ReminderStateStore` backed by SharedPreferences.
- [ ] Store acknowledged keys as stable strings: `taskId|yyyy-MM-dd|HH:mm`.
- [ ] Add unit tests for key formatting in domain where possible.
- [ ] Commit: `feat: persist task reminder acknowledgement`.

### Task 3: Repeat unacknowledged reminders

- [ ] Add receiver extras: task id, task title, task date, start time, interval seconds.
- [ ] Receiver shows notification, then schedules the next alarm if not acknowledged.
- [ ] Scheduler cancels old PendingIntent before scheduling an updated reminder.
- [ ] Click PendingIntent routes to MainActivity with action `ACTION_TASK_REMINDER`.
- [ ] MainActivity captures task id and passes it to `AttentionCoachApp`.
- [ ] AppShell opens the selected task detail Plan page and acknowledges the reminder.
- [ ] Commit: `feat: repeat scheduled task reminders`.

### Task 4: Lock-screen visible notification-card behavior

- [ ] Notification channel uses `IMPORTANCE_HIGH`.
- [ ] Notification uses `CATEGORY_ALARM`.
- [ ] Notification uses `VISIBILITY_PUBLIC`.
- [ ] Notification uses default sound/vibration.
- [ ] Notification is posted from an `RTC_WAKEUP` alarm path so a sleeping device gets the reminder event.
- [ ] Do not add full-screen intent in this phase.
- [ ] Notification content title remains concise: `Scheduled focus time`.
- [ ] Notification text uses task title.
- [ ] Commit: `feat: make task reminders lockscreen visible`.

## Emulator Smoke

- [ ] Set a task on the selected date five minutes in the future.
- [ ] Confirm notification appears.
- [ ] Tap notification and confirm the correct task Plan page opens.
- [ ] Confirm no second reminder after tapping.
- [ ] Set a past date task time and confirm no reminder fires.
- [ ] Lock screen or turn screen off if possible, wait for reminder, and confirm it is visible on lock screen/notification shade.
