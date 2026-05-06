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
        )
        notificationManager.createNotificationChannel(activeChannel)
        notificationManager.createNotificationChannel(reentryChannel)
    }

    fun showReentryBanner(taskTitle: String) {
        ensureChannels()
        notificationManager.notify(REENTRY_NOTIFICATION_ID, buildReentryNotification(taskTitle))
    }

    private fun buildReentryNotification(taskTitle: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REENTRY
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(context, REENTRY_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Still your focus block?")
            .setContentText("You planned to work on $taskTitle. Tap to return.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        const val ACTIVE_WORK_CHANNEL = "active_work_block"
        const val REENTRY_CHANNEL = "reentry_reminder"
        const val ACTION_REENTRY = "com.example.attentioncoach.REENTRY"
        private const val REENTRY_NOTIFICATION_ID = 4521
    }
}

