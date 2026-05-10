package com.example.attentioncoach.domain

import java.time.LocalDate

data class WeeklyInsight(
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val actualMinusPlannedMinutes: Int,
    val commonReasons: List<ReasonCount>
)

data class ReasonCount(
    val reason: String,
    val count: Int
)

object InsightRules {
    fun weeklySummary(tasks: List<PlannedTask>, selectedDate: LocalDate): WeeklyInsight {
        val startDate = selectedDate.minusDays(6)
        val weeklyTasks = tasks.filter { it.date in startDate..selectedDate }
        val plannedMinutes = weeklyTasks.sumOf { it.durationMinutes }
        val actualMinutes = weeklyTasks
            .filter { it.status == TaskStatus.FINISHED || it.status == TaskStatus.REVIEWED }
            .sumOf { it.actualFocusMinutes }
        val commonReasons = weeklyTasks
            .map { it.mismatchReason.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .map { ReasonCount(reason = it.key, count = it.value) }
            .sortedWith(compareByDescending<ReasonCount> { it.count }.thenBy { it.reason })

        return WeeklyInsight(
            plannedMinutes = plannedMinutes,
            actualMinutes = actualMinutes,
            actualMinusPlannedMinutes = actualMinutes - plannedMinutes,
            commonReasons = commonReasons
        )
    }
}

object ReviewReasonOptions {
    val presets = listOf(
        "Attention faded",
        "Task was unclear",
        "Duration was unrealistic",
        "Interrupted by another task",
        "Used entertainment app",
        "Other"
    )
}
