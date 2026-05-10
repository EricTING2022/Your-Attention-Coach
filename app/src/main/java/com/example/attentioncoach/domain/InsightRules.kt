package com.example.attentioncoach.domain

import java.time.LocalDate

data class WeeklyInsight(
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val actualMinusPlannedMinutes: Int,
    val daily: List<DailyInsight>,
    val commonReasons: List<ReasonCount>
)

data class DailyInsight(
    val date: LocalDate,
    val plannedMinutes: Int,
    val actualMinutes: Int
)

data class ReasonCount(
    val reason: String,
    val count: Int
)

object InsightRules {
    fun weeklySummary(tasks: List<PlannedTask>, selectedDate: LocalDate): WeeklyInsight {
        val weekDates = WeekTimeline.weekFor(selectedDate)
        val weeklyTasks = tasks.filter { it.date in weekDates }
        val tasksByDate = weeklyTasks.groupBy { it.date }
        val plannedMinutes = weeklyTasks.sumOf { it.durationMinutes }
        val actualMinutes = weeklyTasks
            .filter { it.status == TaskStatus.FINISHED || it.status == TaskStatus.REVIEWED }
            .sumOf { it.actualFocusMinutes }
        val daily = weekDates.map { date ->
            val dayTasks = tasksByDate[date].orEmpty()
            DailyInsight(
                date = date,
                plannedMinutes = dayTasks.sumOf { it.durationMinutes },
                actualMinutes = dayTasks
                    .filter { it.status == TaskStatus.FINISHED || it.status == TaskStatus.REVIEWED }
                    .sumOf { it.actualFocusMinutes }
            )
        }
        val recordedReasonCounts = weeklyTasks
            .map { it.mismatchReason.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
        val defaultReasons = ReviewReasonOptions.defaultReasons
        val commonReasons = defaultReasons.map {
            ReasonCount(reason = it, count = recordedReasonCounts[it] ?: 0)
        } + recordedReasonCounts
            .filterKeys { it !in defaultReasons }
            .map { ReasonCount(reason = it.key, count = it.value) }
            .sortedWith(compareByDescending<ReasonCount> { it.count }.thenBy { it.reason })

        return WeeklyInsight(
            plannedMinutes = plannedMinutes,
            actualMinutes = actualMinutes,
            actualMinusPlannedMinutes = actualMinutes - plannedMinutes,
            daily = daily,
            commonReasons = commonReasons
        )
    }
}

object ReviewReasonOptions {
    const val other = "Other"
    val defaultReasons = listOf(
        "Attention faded",
        "Entertainment app distraction",
        "Task too large",
        "Task was unclear",
        "Duration was unrealistic",
        "Interrupted by another task"
    )
    val presets = defaultReasons + other
}
