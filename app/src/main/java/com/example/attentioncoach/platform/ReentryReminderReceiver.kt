package com.example.attentioncoach.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.attentioncoach.domain.ScreenOffReentryPolicy

class ReentryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REENTRY_ALARM) return
        val store = ReentryMonitorStateStore(context)
        val state = store.read() ?: return
        val nowMillis = System.currentTimeMillis()
        val decision = ScreenOffReentryPolicy.alarmDecision(
            activeWorkBlock = state.active,
            presence = state.presence,
            nowMillis = nowMillis,
            violationStartedAtMillis = state.violationStartedAtMillis,
            lastNotificationMillis = state.lastNotificationMillis,
            reentryCooldownMillis = state.reentryCooldownMillis
        )
        Log.d(
            TAG,
            "alarm presence=${state.presence} reason=${decision.reason} " +
                "delay=${decision.delayMillis} task=${state.taskId}"
        )
        if (decision.shouldClearAlarm) {
            cancel(context)
            ReentryNotifier(context).clearReentryBanner()
            store.save(state.copy(violationStartedAtMillis = null, lastNotificationMillis = null))
            return
        }
        if (!decision.shouldScheduleAlarm) {
            store.save(
                state.copy(
                    violationStartedAtMillis = decision.nextViolationStartedAtMillis,
                    lastNotificationMillis = decision.nextLastNotificationMillis
                )
            )
            return
        }
        if (decision.delayMillis <= 0L) {
            ReentryNotifier(context).showReentryBanner(state.taskId, state.taskTitle)
            val updated = state.copy(
                violationStartedAtMillis = decision.nextViolationStartedAtMillis,
                lastNotificationMillis = decision.nextLastNotificationMillis ?: nowMillis
            )
            store.save(updated)
            schedule(
                context = context,
                delayMillis = state.reentryCooldownMillis
            )
        } else {
            store.save(
                state.copy(
                    violationStartedAtMillis = decision.nextViolationStartedAtMillis,
                    lastNotificationMillis = decision.nextLastNotificationMillis
                )
            )
            schedule(context, decision.delayMillis)
        }
    }

    companion object {
        private const val ACTION_REENTRY_ALARM = "com.example.attentioncoach.reentry.ALARM"
        private const val REQUEST_CODE = 4522
        private const val TAG = "AC_ReentryAlarmV2"

        fun schedule(context: Context, delayMillis: Long) {
            val triggerAtMillis = System.currentTimeMillis() + delayMillis.coerceAtLeast(0L)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = pendingIntent(context)
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

        fun cancel(context: Context) {
            val pendingIntent = pendingIntent(context)
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReentryReminderReceiver::class.java).apply {
                action = ACTION_REENTRY_ALARM
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
