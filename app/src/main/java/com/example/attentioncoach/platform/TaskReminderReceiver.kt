package com.example.attentioncoach.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.attentioncoach.MainActivity
import com.example.attentioncoach.domain.ReminderRules
import com.example.attentioncoach.domain.StartReminderAction

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val startReminderStore = StartReminderStore(context)
        when (
            ReminderRules.startReminderAction(
                isAcknowledged = isAcknowledged(context, taskId) || startReminderStore.isAcknowledged(taskId),
                focusActive = FocusSessionStore(context).isActive()
            )
        ) {
            StartReminderAction.IGNORE -> return
            StartReminderAction.DEFER -> {
                startReminderStore.defer(taskId)
                return
            }
            StartReminderAction.NOTIFY -> startReminderStore.markActiveDue(taskId)
        }
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty().ifBlank { "your task" }
        val repeatIntervalSeconds = intent.getIntExtra(EXTRA_REPEAT_INTERVAL_SECONDS, DEFAULT_REPEAT_INTERVAL_SECONDS)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(notificationManager)
        notificationManager.notify(
            REMINDER_NOTIFICATION_ID_BASE + taskId.toInt().coerceAtLeast(0),
            buildNotification(context, taskId, taskTitle)
        )
        scheduleRepeat(context, taskId, taskTitle, repeatIntervalSeconds)
    }

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            REMINDER_CHANNEL,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(
            channel
        )
    }

    private fun buildNotification(context: Context, taskId: Long, taskTitle: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, REMINDER_CHANNEL)
        } else {
            Notification.Builder(context)
        }
        val contentIntent = scheduledReminderIntent(context, taskId)
        val fullScreenIntent = lockscreenReminderIntent(context, taskId, taskTitle)
        builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled focus time")
            .setContentText(taskTitle)
            .setStyle(Notification.BigTextStyle().bigText(taskTitle))
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        return builder.build()
    }

    private fun scheduleRepeat(
        context: Context,
        taskId: Long,
        taskTitle: String,
        repeatIntervalSeconds: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = System.currentTimeMillis() + repeatIntervalSeconds.coerceAtLeast(1) * 1000L
        val pendingIntent = reminderPendingIntent(context, taskId, taskTitle, repeatIntervalSeconds)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_REPEAT_INTERVAL_SECONDS = "repeat_interval_seconds"
        const val ACTION_SCHEDULED_REMINDER = "com.example.attentioncoach.SCHEDULED_REMINDER"
        private const val ACTION_REMIND = "com.example.attentioncoach.REMIND_TASK"
        private const val REMINDER_CHANNEL = "task_reminders"
        private const val REMINDER_NOTIFICATION_ID_BASE = 5_000
        private const val CONTENT_REQUEST_CODE_BASE = 75_000
        private const val FULL_SCREEN_REQUEST_CODE_BASE = 95_000
        private const val DEFAULT_REPEAT_INTERVAL_SECONDS = 30
        private const val PREFS_NAME = "task_reminders"

        fun reminderPendingIntent(
            context: Context,
            taskId: Long,
            taskTitle: String,
            repeatIntervalSeconds: Int
        ): PendingIntent {
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                action = ACTION_REMIND
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_REPEAT_INTERVAL_SECONDS, repeatIntervalSeconds)
            }
            return PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun scheduledReminderIntent(context: Context, taskId: Long): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_SCHEDULED_REMINDER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getActivity(
                context,
                CONTENT_REQUEST_CODE_BASE + taskId.toInt().coerceAtLeast(0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun lockscreenReminderIntent(context: Context, taskId: Long, taskTitle: String): PendingIntent {
            val intent = Intent(context, ReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
            }
            return PendingIntent.getActivity(
                context,
                FULL_SCREEN_REQUEST_CODE_BASE + taskId.toInt().coerceAtLeast(0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun clearAcknowledged(context: Context, taskId: Long) {
            StartReminderStore(context).clearAcknowledged(taskId)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(ackKey(taskId))
                .apply()
        }

        fun acknowledgeReminder(context: Context, taskId: Long) {
            StartReminderStore(context).acknowledge(taskId)
            acknowledge(context, taskId)
            cancelReminder(context, taskId)
            context.getSystemService(NotificationManager::class.java)
                .cancel(REMINDER_NOTIFICATION_ID_BASE + taskId.toInt().coerceAtLeast(0))
        }

        fun acknowledgeReminders(context: Context, taskIds: Iterable<Long>) {
            taskIds.forEach { acknowledgeReminder(context, it) }
        }

        private fun acknowledge(context: Context, taskId: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ackKey(taskId), true)
                .apply()
        }

        private fun isAcknowledged(context: Context, taskId: Long): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(ackKey(taskId), false)
        }

        private fun cancelReminder(context: Context, taskId: Long) {
            val pendingIntent = reminderPendingIntent(
                context = context,
                taskId = taskId,
                taskTitle = "",
                repeatIntervalSeconds = DEFAULT_REPEAT_INTERVAL_SECONDS
            )
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }

        private fun ackKey(taskId: Long): String {
            return "acknowledged_$taskId"
        }
    }
}
