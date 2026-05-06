package com.example.attentioncoach.domain

import java.time.LocalDate

data class PlannedTask(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val target: String,
    val durationMinutes: Int,
    val priority: Priority,
    val status: TaskStatus,
    val planningNote: String,
    val actualFocusMinutes: Int = 0,
    val actualCompletion: String = "",
    val mismatchReason: String = "Attention faded",
    val nextAdjustment: String = ""
)

enum class Priority {
    URGENT_IMPORTANT,
    URGENT,
    IMPORTANT,
    NOT_URGENT
}

enum class TaskStatus {
    PLANNED,
    PAUSED,
    REVIEWED,
    FINISHED,
    MISSED
}

data class TaskGroups(
    val open: List<PlannedTask>,
    val completed: List<PlannedTask>
)

data class DailySummary(
    val plannedMinutes: Int,
    val reviewedCount: Int,
    val totalCount: Int
)

data class ReentryDecision(
    val shouldNotify: Boolean,
    val reason: ReentryReason
)

enum class ReentryReason {
    INACTIVE,
    SELF,
    NEEDED_APP,
    NOT_LEISURE,
    LEISURE_APP,
    COOLDOWN
}

