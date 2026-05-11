package com.example.attentioncoach.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderRules {
    fun isSchedulable(startTime: LocalTime?): Boolean {
        return startTime != null
    }

    fun triggerAtMillis(date: LocalDate, startTime: LocalTime, zoneId: ZoneId): Long {
        return ZonedDateTime.of(date, startTime, zoneId).toInstant().toEpochMilli()
    }

    fun futureTriggerAtMillisOrNull(
        date: LocalDate,
        startTime: LocalTime,
        zoneId: ZoneId,
        nowMillis: Long
    ): Long? {
        val triggerAtMillis = triggerAtMillis(date, startTime, zoneId)
        return triggerAtMillis.takeIf { it > nowMillis }
    }

    fun dueStartReminderTaskIds(
        tasks: List<PlannedTask>,
        nowMillis: Long,
        zoneId: ZoneId
    ): List<Long> {
        return tasks.filter { task ->
            task.startTime != null &&
                task.status != TaskStatus.FINISHED &&
                task.status != TaskStatus.REVIEWED &&
                triggerAtMillis(task.date, task.startTime, zoneId) <= nowMillis
        }.map { it.id }
    }

    fun highestPriorityDueTask(
        tasks: List<PlannedTask>,
        activeDueIds: Set<Long>
    ): PlannedTask? {
        return tasks.filter { task ->
            task.id in activeDueIds &&
                task.status != TaskStatus.FINISHED &&
                task.status != TaskStatus.REVIEWED
        }.minWithOrNull(
            compareBy<PlannedTask> { it.priority.reminderRank() }
                .thenBy { it.id }
        )
    }

    private fun Priority.reminderRank(): Int {
        return when (this) {
            Priority.URGENT_IMPORTANT -> 0
            Priority.URGENT -> 1
            Priority.IMPORTANT -> 2
            Priority.NOT_URGENT -> 3
        }
    }
}
