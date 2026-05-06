package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AppStateTest {
    @Test
    fun demoRepositorySeedsFrozenPrototypeTasksForMayFive() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        val mayFiveTasks = store.tasksForDate(LocalDate.of(2026, 5, 5))

        assertEquals(4, mayFiveTasks.size)
        assertEquals("COMP4521 design spec", mayFiveTasks.first { it.id == 1L }.title)
        assertEquals(TaskStatus.PAUSED, mayFiveTasks.first { it.id == 3L }.status)
    }

    @Test
    fun saveReviewMarksTaskReviewedAndMovesItToCompletedGroup() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.saveReview(
            taskId = 1L,
            actualCompletion = "Reviewed implementation plan.",
            mismatchReason = "Attention faded",
            nextAdjustment = "Use 30 minutes first."
        )

        val groups = TaskGrouper.group(store.tasksForDate(LocalDate.of(2026, 5, 5)))
        assertTrue(groups.completed.any { it.id == 1L })
        assertEquals(TaskStatus.REVIEWED, store.taskById(1L)?.status)
    }

    @Test
    fun startAndExitWorkKeepsOriginalPlanUnchanged() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())
        val before = store.taskById(1L)

        store.startWork(taskId = 1L)
        assertNotNull(store.activeWork)
        store.exitWork()

        val after = store.taskById(1L)
        assertEquals(before?.target, after?.target)
        assertEquals(before?.durationMinutes, after?.durationMinutes)
        assertEquals(before?.status, after?.status)
        assertFalse(store.activeWork?.isActive == true)
    }

    @Test
    fun updatePlanStoresScheduleAndEditablePlanValues() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.updatePlan(
            taskId = 1L,
            target = "New target",
            startTime = LocalTime.of(9, 30),
            durationMinutes = 30,
            priority = Priority.URGENT
        )

        val task = store.taskById(1L)
        assertEquals("New target", task?.target)
        assertEquals(LocalTime.of(9, 30), task?.startTime)
        assertEquals(30, task?.durationMinutes)
        assertEquals(Priority.URGENT, task?.priority)
        assertEquals(TaskStatus.PLANNED, task?.status)
    }

    @Test
    fun createTaskUsesSelectedDateAndDefaultPlanningValues() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        val created = store.createTask(
            date = LocalDate.of(2026, 5, 7),
            title = "New task"
        )

        assertEquals(LocalDate.of(2026, 5, 7), created.date)
        assertEquals("New task", created.title)
        assertEquals("", created.target)
        assertEquals(30, created.durationMinutes)
        assertEquals(Priority.IMPORTANT, created.priority)
        assertEquals(TaskStatus.PLANNED, created.status)
        assertTrue(store.tasksForDate(LocalDate.of(2026, 5, 7)).any { it.id == created.id })
    }

    @Test
    fun newTasksUseBlankReviewReasonByDefault() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        val created = store.createTask(
            date = LocalDate.of(2026, 5, 7),
            title = "Blank reason task"
        )

        assertEquals("", created.mismatchReason)
    }

    @Test
    fun deleteTaskRemovesItFromDateGroups() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.deleteTask(taskId = 1L)

        assertFalse(store.tasksForDate(LocalDate.of(2026, 5, 5)).any { it.id == 1L })
        assertEquals(null, store.taskById(1L))
    }

    @Test
    fun finishWorkStoresActualFocusAndMovesTaskToCompletedGroup() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.startWork(taskId = 1L, nowMillis = 0L)
        store.finishWork(nowMillis = 61_000L)

        val task = store.taskById(1L)
        assertEquals(TaskStatus.FINISHED, task?.status)
        assertEquals(2, task?.actualFocusMinutes)
        assertTrue(TaskGrouper.group(store.tasksForDate(LocalDate.of(2026, 5, 5))).completed.any { it.id == 1L })
    }

    @Test
    fun toggleCompletionMarksPlannedTaskFinishedWithoutActualFocus() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.toggleTaskCompletion(1L)

        val task = store.taskById(1L)
        assertEquals(TaskStatus.FINISHED, task?.status)
        assertEquals(0, task?.actualFocusMinutes)
    }

    @Test
    fun toggleCompletionAgainReturnsTaskToPlannedAndClearsReviewFields() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.toggleTaskCompletion(1L)
        store.saveReview(
            taskId = 1L,
            actualCompletion = "Done",
            mismatchReason = "Reason",
            nextAdjustment = "Next"
        )
        store.toggleTaskCompletion(1L)

        val task = store.taskById(1L)
        assertEquals(TaskStatus.PLANNED, task?.status)
        assertEquals(0, task?.actualFocusMinutes)
        assertEquals("", task?.actualCompletion)
        assertEquals("", task?.mismatchReason)
        assertEquals("", task?.nextAdjustment)
    }

    @Test
    fun pausedTimeIsExcludedFromFinishedActualFocus() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.startWork(taskId = 1L, nowMillis = 0L)
        store.pauseWork(nowMillis = 20_000L)
        store.resumeWork(nowMillis = 200_000L)
        store.finishWork(nowMillis = 250_000L)

        assertEquals(2, store.taskById(1L)?.actualFocusMinutes)
    }

    @Test
    fun exitWorkDoesNotMutateActualFocusOrStatus() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.startWork(taskId = 1L, nowMillis = 0L)
        store.exitWork()

        val task = store.taskById(1L)
        assertEquals(TaskStatus.PLANNED, task?.status)
        assertEquals(28, task?.actualFocusMinutes)
    }

    @Test
    fun reviewIsAvailableOnlyAfterFinishOrReview() {
        assertFalse(ReviewAvailability.canReview(TaskStatus.PLANNED))
        assertFalse(ReviewAvailability.canReview(TaskStatus.PAUSED))
        assertFalse(ReviewAvailability.canReview(TaskStatus.MISSED))
        assertTrue(ReviewAvailability.canReview(TaskStatus.FINISHED))
        assertTrue(ReviewAvailability.canReview(TaskStatus.REVIEWED))
    }

    @Test
    fun saveReviewChangesFinishedTaskToReviewed() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.startWork(taskId = 1L, nowMillis = 0L)
        store.finishWork(nowMillis = 60_000L)
        store.saveReview(
            taskId = 1L,
            actualCompletion = "Done.",
            mismatchReason = "Clear enough",
            nextAdjustment = "Keep block size."
        )

        assertEquals(TaskStatus.REVIEWED, store.taskById(1L)?.status)
    }
}
