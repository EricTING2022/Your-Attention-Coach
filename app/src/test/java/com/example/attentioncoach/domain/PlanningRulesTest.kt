package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanningRulesTest {
    @Test
    fun weekTimelineStartsOnSundayAndKeepsSelectedWeek() {
        val week = WeekTimeline.weekFor(LocalDate.of(2026, 5, 6))

        assertEquals(LocalDate.of(2026, 5, 3), week.first())
        assertEquals(LocalDate.of(2026, 5, 9), week.last())
        assertEquals(7, week.size)
    }

    @Test
    fun taskListSortsByPriorityThenCreationOrder() {
        val tasks = listOf(
            task(id = 1, title = "Design", priority = Priority.IMPORTANT),
            task(id = 2, title = "Reviewed notes", priority = Priority.NOT_URGENT),
            task(id = 3, title = "Skeleton", priority = Priority.URGENT),
            task(id = 4, title = "Finished report", priority = Priority.URGENT_IMPORTANT)
        )

        val sorted = TaskListSorter.sortForToday(tasks)

        assertEquals(listOf(4L, 3L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun taskListKeepsCompletedTasksInPriorityPosition() {
        val tasks = listOf(
            task(id = 1, priority = Priority.IMPORTANT, status = TaskStatus.PLANNED),
            task(id = 2, priority = Priority.URGENT_IMPORTANT, status = TaskStatus.FINISHED),
            task(id = 3, priority = Priority.URGENT, status = TaskStatus.REVIEWED),
            task(id = 4, priority = Priority.NOT_URGENT, status = TaskStatus.PLANNED)
        )

        val sorted = TaskListSorter.sortForToday(tasks)

        assertEquals(listOf(2L, 3L, 1L, 4L), sorted.map { it.id })
    }

    @Test
    fun taskListKeepsCreationOrderWithinSamePriority() {
        val tasks = listOf(
            task(id = 3, priority = Priority.IMPORTANT),
            task(id = 1, priority = Priority.IMPORTANT),
            task(id = 2, priority = Priority.IMPORTANT)
        )

        val sorted = TaskListSorter.sortForToday(tasks)

        assertEquals(listOf(1L, 2L, 3L), sorted.map { it.id })
    }

    @Test
    fun summaryCountsPlannedMinutesAndReviewedTasksOnly() {
        val summary = SummaryCalculator.forTasks(
            listOf(
                task(durationMinutes = 40, status = TaskStatus.PLANNED),
                task(durationMinutes = 30, status = TaskStatus.REVIEWED),
                task(durationMinutes = 25, status = TaskStatus.FINISHED)
            )
        )

        assertEquals(95, summary.plannedMinutes)
        assertEquals(1, summary.reviewedCount)
        assertEquals(3, summary.totalCount)
    }

    @Test
    fun dateIndicatorShowsBlueDotWhenAnyTaskIsUnfinished() {
        val tasks = listOf(
            task(id = 1, status = TaskStatus.FINISHED),
            task(id = 2, status = TaskStatus.PLANNED)
        )

        assertTrue(DateIndicatorRules.hasUnfinishedTaskDot(tasks))
    }

    @Test
    fun dateIndicatorHidesDotWhenAllTasksAreComplete() {
        val tasks = listOf(
            task(id = 1, status = TaskStatus.FINISHED),
            task(id = 2, status = TaskStatus.REVIEWED)
        )

        assertFalse(DateIndicatorRules.hasUnfinishedTaskDot(tasks))
    }

    @Test
    fun softLockAllowsNeededAppsWithoutReentryNotification() {
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = "com.android.chrome",
            neededPackages = setOf("com.android.chrome"),
            leisurePackages = setOf("com.shortvideo.app"),
            nowMillis = 60_000,
            lastNotificationMillis = null
        )

        assertFalse(decision.shouldNotify)
        assertEquals(ReentryReason.NEEDED_APP, decision.reason)
    }

    @Test
    fun softLockTriggersLeisureAppAfterThirtySecondCooldown() {
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = "com.shortvideo.app",
            neededPackages = setOf("com.android.chrome"),
            leisurePackages = setOf("com.shortvideo.app"),
            nowMillis = 60_000,
            lastNotificationMillis = 29_000
        )

        assertTrue(decision.shouldNotify)
        assertEquals(ReentryReason.LEISURE_APP, decision.reason)
    }

    @Test
    fun softLockTriggersAnyNonNeededAppAfterCooldown() {
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = "com.random.reader",
            neededPackages = setOf("com.android.chrome"),
            leisurePackages = emptySet(),
            nowMillis = 60_000,
            lastNotificationMillis = 20_000
        )

        assertTrue(decision.shouldNotify)
        assertEquals(ReentryReason.NON_NEEDED_APP, decision.reason)
    }

    @Test
    fun softLockSuppressesRepeatedLeisureNotificationInsideCooldown() {
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = "com.shortvideo.app",
            neededPackages = emptySet(),
            leisurePackages = setOf("com.shortvideo.app"),
            nowMillis = 60_000,
            lastNotificationMillis = 45_000
        )

        assertFalse(decision.shouldNotify)
        assertEquals(ReentryReason.COOLDOWN, decision.reason)
    }

    @Test
    fun softLockUsesConfiguredReentryCooldown() {
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = "com.shortvideo.app",
            neededPackages = emptySet(),
            leisurePackages = setOf("com.shortvideo.app"),
            nowMillis = 60_000,
            lastNotificationMillis = 20_000,
            reentryCooldownMillis = 30_000
        )

        assertTrue(decision.shouldNotify)
        assertEquals(ReentryReason.LEISURE_APP, decision.reason)
    }

    private fun task(
        id: Long = 1,
        title: String = "Task",
        durationMinutes: Int = 30,
        priority: Priority = Priority.IMPORTANT,
        status: TaskStatus = TaskStatus.PLANNED
    ): PlannedTask {
        return PlannedTask(
            id = id,
            date = LocalDate.of(2026, 5, 5),
            title = title,
            target = "Target",
            durationMinutes = durationMinutes,
            priority = priority,
            status = status
        )
    }
}
