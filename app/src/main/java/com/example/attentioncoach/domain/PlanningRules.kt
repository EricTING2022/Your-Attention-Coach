package com.example.attentioncoach.domain

import java.time.LocalDate

object WeekTimeline {
    fun weekFor(selectedDate: LocalDate): List<LocalDate> {
        val sunday = selectedDate.minusDays(selectedDate.dayOfWeek.value % 7L)
        return (0L..6L).map { sunday.plusDays(it) }
    }
}

object TaskGrouper {
    fun group(tasks: List<PlannedTask>): TaskGroups {
        val open = tasks
            .filterNot { it.isCompleted() }
            .sortedWith(compareByDescending<PlannedTask> { it.status == TaskStatus.PAUSED })
        val completed = tasks.filter { it.isCompleted() }
        return TaskGroups(open = open, completed = completed)
    }

    private fun PlannedTask.isCompleted(): Boolean {
        return status == TaskStatus.REVIEWED || status == TaskStatus.FINISHED
    }
}

object TaskListSorter {
    fun sortForToday(tasks: List<PlannedTask>): List<PlannedTask> {
        return tasks.sortedWith(
            compareByDescending<PlannedTask> { it.status == TaskStatus.PAUSED }
                .thenBy { it.status == TaskStatus.FINISHED || it.status == TaskStatus.REVIEWED }
                .thenBy { it.id }
        )
    }
}

object ReviewAvailability {
    fun canReview(status: TaskStatus): Boolean {
        return status == TaskStatus.FINISHED || status == TaskStatus.REVIEWED
    }
}

object SummaryCalculator {
    fun forTasks(tasks: List<PlannedTask>): DailySummary {
        return DailySummary(
            plannedMinutes = tasks.sumOf { it.durationMinutes },
            reviewedCount = tasks.count { it.status == TaskStatus.REVIEWED },
            totalCount = tasks.size
        )
    }
}

object DateIndicatorRules {
    fun hasUnfinishedTaskDot(tasks: List<PlannedTask>): Boolean {
        return tasks.any { it.status != TaskStatus.FINISHED && it.status != TaskStatus.REVIEWED }
    }
}

object FocusMonitorCadence {
    const val POLL_INTERVAL_MILLIS = 5_000L
    const val USAGE_LOOKBACK_MILLIS = 15_000L
    const val REENTRY_COOLDOWN_MILLIS = 30_000L
}

object SoftLockPolicy {
    private const val APP_PACKAGE = "com.example.attentioncoach"

    fun reentryDecision(
        activeWorkBlock: Boolean,
        foregroundPackage: String?,
        neededPackages: Set<String>,
        leisurePackages: Set<String>,
        nowMillis: Long,
        lastNotificationMillis: Long?
    ): ReentryDecision {
        if (!activeWorkBlock) return ReentryDecision(false, ReentryReason.INACTIVE)
        if (foregroundPackage == null || foregroundPackage == APP_PACKAGE) {
            return ReentryDecision(false, ReentryReason.SELF)
        }
        if (foregroundPackage in neededPackages) {
            return ReentryDecision(false, ReentryReason.NEEDED_APP)
        }
        if (foregroundPackage !in leisurePackages) {
            return ReentryDecision(false, ReentryReason.NOT_LEISURE)
        }
        if (lastNotificationMillis != null && nowMillis - lastNotificationMillis < FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS) {
            return ReentryDecision(false, ReentryReason.COOLDOWN)
        }
        return ReentryDecision(true, ReentryReason.LEISURE_APP)
    }
}
