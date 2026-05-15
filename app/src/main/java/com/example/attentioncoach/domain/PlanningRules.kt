package com.example.attentioncoach.domain

import java.time.LocalDate

object WeekTimeline {
    fun weekFor(selectedDate: LocalDate): List<LocalDate> {
        val sunday = selectedDate.minusDays(selectedDate.dayOfWeek.value % 7L)
        return (0L..6L).map { sunday.plusDays(it) }
    }
}

object TaskListSorter {
    fun sortForToday(tasks: List<PlannedTask>): List<PlannedTask> {
        return tasks.sortedWith(
            compareBy<PlannedTask> { it.priority.sortRank() }
                .thenBy { it.id }
        )
    }
}

private fun Priority.sortRank(): Int {
    return when (this) {
        Priority.URGENT_IMPORTANT -> 0
        Priority.URGENT -> 1
        Priority.IMPORTANT -> 2
        Priority.NOT_URGENT -> 3
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
    const val FOREGROUND_OBSERVATION_MAX_AGE_MILLIS = 10_000L
    const val REENTRY_COOLDOWN_MILLIS = 30_000L
}

object ForegroundObservationRules {
    const val DUPLICATE_RECORD_THROTTLE_MILLIS = 5_000L

    fun choosePackage(
        eventPackage: String?,
        rootPackage: String?,
        windowPackages: List<String>
    ): String? {
        val cleanRootPackage = rootPackage?.trim()?.takeIf(String::isNotBlank)
        val cleanEventPackage = eventPackage?.trim()?.takeIf(String::isNotBlank)
        return sequenceOf(cleanRootPackage, cleanEventPackage)
            .plus(windowPackages.asSequence())
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    fun freshOrNull(
        observation: ForegroundObservation?,
        nowMillis: Long,
        maxAgeMillis: Long
    ): ForegroundObservation? {
        if (observation == null) return null
        return observation.takeIf { nowMillis - it.observedAtMillis <= maxAgeMillis }
    }

    fun shouldRecord(
        lastPackageName: String?,
        lastRecordedAtMillis: Long,
        newPackageName: String,
        nowMillis: Long,
        duplicateThrottleMillis: Long = DUPLICATE_RECORD_THROTTLE_MILLIS
    ): Boolean {
        return lastPackageName != newPackageName ||
            nowMillis - lastRecordedAtMillis >= duplicateThrottleMillis
    }
}

object ForegroundPresenceClassifier {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    fun classify(
        attentionCoachInForeground: Boolean,
        observation: ForegroundObservation?,
        nowMillis: Long,
        appPackage: String,
        whitelistPackages: Set<String>,
        launcherPackages: Set<String>,
        maxAgeMillis: Long = FocusMonitorCadence.FOREGROUND_OBSERVATION_MAX_AGE_MILLIS
    ): FocusPresence {
        if (attentionCoachInForeground) return FocusPresence.IN_ATTENTION_COACH
        val foregroundPackage = ForegroundObservationRules.freshOrNull(
            observation = observation,
            nowMillis = nowMillis,
            maxAgeMillis = maxAgeMillis
        )?.packageName ?: return FocusPresence.UNKNOWN

        return when (foregroundPackage) {
            appPackage -> FocusPresence.IN_ATTENTION_COACH
            SYSTEM_UI_PACKAGE -> FocusPresence.UNKNOWN
            in whitelistPackages -> FocusPresence.IN_WHITELIST_APP
            in launcherPackages -> FocusPresence.IN_LAUNCHER
            else -> FocusPresence.IN_OTHER_APP
        }
    }
}

object ForegroundPresenceMemory {
    fun resolve(
        classifiedPresence: FocusPresence,
        lastStablePresence: FocusPresence?
    ): FocusPresence {
        return if (classifiedPresence == FocusPresence.UNKNOWN) {
            lastStablePresence ?: FocusPresence.UNKNOWN
        } else {
            classifiedPresence
        }
    }
}

object SoftLockPolicy {
    private const val APP_PACKAGE = "com.example.attentioncoach"

    fun reentryDecision(
        activeWorkBlock: Boolean,
        foregroundPackage: String?,
        neededPackages: Set<String>,
        leisurePackages: Set<String>,
        nowMillis: Long,
        lastNotificationMillis: Long?,
        reentryCooldownMillis: Long = FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS
    ): ReentryDecision {
        if (!activeWorkBlock) return ReentryDecision(false, ReentryReason.INACTIVE)
        if (foregroundPackage == null || foregroundPackage == APP_PACKAGE) {
            return ReentryDecision(false, ReentryReason.SELF)
        }
        if (foregroundPackage in neededPackages) {
            return ReentryDecision(false, ReentryReason.NEEDED_APP)
        }
        if (lastNotificationMillis != null && nowMillis - lastNotificationMillis < reentryCooldownMillis) {
            return ReentryDecision(false, ReentryReason.COOLDOWN)
        }
        return ReentryDecision(
            shouldNotify = true,
            reason = if (foregroundPackage in leisurePackages) ReentryReason.LEISURE_APP else ReentryReason.NON_NEEDED_APP
        )
    }
}
