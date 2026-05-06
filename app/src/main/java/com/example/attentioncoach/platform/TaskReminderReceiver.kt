package com.example.attentioncoach.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.attentioncoach.MainActivity

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty().ifBlank { "your task" }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(notificationManager)
        notificationManager.notify(
            REMINDER_NOTIFICATION_ID_BASE + taskId.toInt().coerceAtLeast(0),
            buildNotification(context, taskTitle)
        )
    }

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun buildNotification(context: Context, taskTitle: String): Notification {
        return Notification.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Scheduled focus time")
            .setContentText(taskTitle)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        private const val REMINDER_CHANNEL = "task_reminders"
        private const val REMINDER_NOTIFICATION_ID_BASE = 5_000
    }
}
