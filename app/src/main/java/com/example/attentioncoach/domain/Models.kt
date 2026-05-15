package com.example.attentioncoach.domain

import java.time.LocalDate
import java.time.LocalTime

data class PlannedTask(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val target: String,
    val startTime: LocalTime? = null,
    val durationMinutes: Int,
    val priority: Priority,
    val status: TaskStatus,
    val actualFocusMinutes: Int = 0,
    val actualCompletion: String = "",
    val mismatchReason: String = "",
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
    REVIEWED,
    FINISHED,
    MISSED
}

data class DailySummary(
    val plannedMinutes: Int,
    val reviewedCount: Int,
    val totalCount: Int
)

data class ReentryDecision(
    val shouldNotify: Boolean,
    val reason: ReentryReason
)

data class ForegroundObservation(
    val packageName: String,
    val source: ForegroundSource,
    val observedAtMillis: Long
)

enum class ForegroundSource {
    ACCESSIBILITY,
    USAGE_STATS
}

enum class FocusPresence {
    IN_ATTENTION_COACH,
    IN_WHITELIST_APP,
    IN_LAUNCHER,
    IN_OTHER_APP,
    UNKNOWN
}

enum class ReentryReason {
    INACTIVE,
    SELF,
    NEEDED_APP,
    NOT_LEISURE,
    LEISURE_APP,
    NON_NEEDED_APP,
    COOLDOWN
}

data class ActiveWork(
    val taskId: Long,
    val isActive: Boolean,
    val plannedDurationMinutes: Int = 0,
    val startedAtMillis: Long = 0L,
    val accumulatedActiveMillis: Long = 0L,
    val pauseStartedAtMillis: Long? = null,
    val isPaused: Boolean = false
)
