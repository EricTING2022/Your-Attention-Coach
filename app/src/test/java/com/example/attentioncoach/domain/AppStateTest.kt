package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun updatePlanFieldsChangesOnlyEditablePlanValues() {
        val store = AttentionCoachStore(DemoTaskRepository.seed())

        store.updatePlan(
            taskId = 1L,
            target = "New target",
            durationMinutes = 30,
            priority = Priority.URGENT,
            planningNote = "New note"
        )

        val task = store.taskById(1L)
        assertEquals("New target", task?.target)
        assertEquals(30, task?.durationMinutes)
        assertEquals(Priority.URGENT, task?.priority)
        assertEquals("New note", task?.planningNote)
        assertEquals(TaskStatus.PLANNED, task?.status)
    }
}
