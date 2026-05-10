package com.example.attentioncoach.platform

import android.app.AlarmManager
import android.content.Context
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.ReminderRules
import java.time.ZoneId

enum class ReminderScheduleResult {
    SCHEDULED,
    NEEDS_EXACT_ALARM_PERMISSION,
    NO_START_TIME,
    PAST_START_TIME
}

class TaskReminderScheduler(
    private val context: Context,
    private val alarmPermissionHelper: AlarmPermissionHelper = AlarmPermissionHelper(context)
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: PlannedTask, repeatIntervalSeconds: Int): ReminderScheduleResult {
        val startTime = task.startTime ?: return ReminderScheduleResult.NO_START_TIME
        val triggerAtMillis = ReminderRules.futureTriggerAtMillisOrNull(
            date = task.date,
            startTime = startTime,
            zoneId = ZoneId.systemDefault(),
            nowMillis = System.currentTimeMillis()
        ) ?: return ReminderScheduleResult.PAST_START_TIME
        if (!alarmPermissionHelper.canScheduleExactAlarms()) {
            return ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION
        }
        TaskReminderReceiver.clearAcknowledged(context, task.id)
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                TaskReminderReceiver.reminderPendingIntent(context, task.id, task.title, repeatIntervalSeconds)
            )
            ReminderScheduleResult.SCHEDULED
        } catch (_: SecurityException) {
            ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION
        }
    }
}
