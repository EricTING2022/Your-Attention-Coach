package com.example.attentioncoach.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.ReminderRules
import java.time.ZoneId

enum class ReminderScheduleResult {
    SCHEDULED,
    NEEDS_EXACT_ALARM_PERMISSION,
    NO_START_TIME
}

class TaskReminderScheduler(
    private val context: Context,
    private val alarmPermissionHelper: AlarmPermissionHelper = AlarmPermissionHelper(context)
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: PlannedTask): ReminderScheduleResult {
        val startTime = task.startTime ?: return ReminderScheduleResult.NO_START_TIME
        if (!alarmPermissionHelper.canScheduleExactAlarms()) {
            return ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION
        }
        val triggerAtMillis = ReminderRules.triggerAtMillis(task.date, startTime, ZoneId.systemDefault())
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                reminderIntent(task)
            )
            ReminderScheduleResult.SCHEDULED
        } catch (_: SecurityException) {
            ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION
        }
    }

    private fun reminderIntent(task: PlannedTask): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
