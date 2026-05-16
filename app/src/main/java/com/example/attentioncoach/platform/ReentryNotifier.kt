package com.example.attentioncoach.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.attentioncoach.MainActivity

class ReentryNotifier(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activeChannel = NotificationChannel(
            ACTIVE_WORK_CHANNEL,
            "Active Work Block",
            NotificationManager.IMPORTANCE_LOW
        )
        val reentryChannel = NotificationChannel(
            REENTRY_CHANNEL,
            "Re-entry Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(activeChannel)
        notificationManager.createNotificationChannel(reentryChannel)
    }

    fun buildActiveWorkNotification(taskId: Long, taskTitle: String): Notification {
        ensureChannels()
        return Notification.Builder(context, ACTIVE_WORK_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Focus block running")
            .setContentText(taskTitle)
            .setContentIntent(reentryPendingIntent(taskId))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    fun showReentryBanner(taskId: Long, taskTitle: String, useFullScreen: Boolean = false) {
        ensureChannels()
        notificationManager.cancel(REENTRY_NOTIFICATION_ID)
        notificationManager.notify(REENTRY_NOTIFICATION_ID, buildReentryNotification(taskId, taskTitle, useFullScreen))
    }

    fun clearReentryBanner() {
        notificationManager.cancel(REENTRY_NOTIFICATION_ID)
    }

    private fun buildReentryNotification(taskId: Long, taskTitle: String, useFullScreen: Boolean): Notification {
        val builder = Notification.Builder(context, REENTRY_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Still your focus block?")
            .setContentText("You planned to work on $taskTitle. Tap to return.")
            .setStyle(Notification.BigTextStyle().bigText("You planned to work on $taskTitle. Tap to return."))
            .setContentIntent(reentryPendingIntent(taskId))
            .setAutoCancel(true)
            .setTimeoutAfter(REENTRY_TIMEOUT_MILLIS)
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        if (useFullScreen) {
            builder.setFullScreenIntent(reentryLockscreenPendingIntent(taskId, taskTitle), true)
        }
        return builder.build()
    }

    private fun reentryPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REENTRY
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reentryLockscreenPendingIntent(taskId: Long, taskTitle: String): PendingIntent {
        val intent = Intent(context, ReentryLockscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getActivity(
            context,
            LOCKSCREEN_REQUEST_CODE_BASE + taskId.toInt().coerceAtLeast(0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTIVE_WORK_CHANNEL = "active_work_block"
        const val REENTRY_CHANNEL = "reentry_reminder"
        const val ACTION_REENTRY = "com.example.attentioncoach.REENTRY"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        private const val REENTRY_NOTIFICATION_ID = 4521
        private const val REENTRY_TIMEOUT_MILLIS = 15_000L
        private const val LOCKSCREEN_REQUEST_CODE_BASE = 125_000
    }
}
