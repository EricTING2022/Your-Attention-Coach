package com.example.attentioncoach.data

import com.example.attentioncoach.data.local.TaskWithReview
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.TaskStatus
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskMappersTest {
    @Test
    fun plannedTaskRoundTripsThroughRoomEntities() {
        val task = PlannedTask(
            id = 42L,
            date = LocalDate.of(2026, 5, 5),
            title = "Report outline",
            target = "Draft the final report structure.",
            startTime = LocalTime.of(14, 30),
            durationMinutes = 45,
            priority = Priority.URGENT_IMPORTANT,
            status = TaskStatus.REVIEWED,
            actualFocusMinutes = 51,
            actualCompletion = "Finished the introduction and feature list.",
            mismatchReason = "Task too large",
            nextAdjustment = "Split testing evidence into its own block."
        )

        val entities = task.toEntities(isDemo = true, nowMillis = 1234L)
        val restored = TaskWithReview(
            task = entities.task,
            review = entities.review
        ).toDomain()

        assertEquals(task, restored)
        assertTrue(entities.task.isDemo)
        assertEquals(1234L, entities.task.createdAtMillis)
        assertEquals(1234L, entities.task.updatedAtMillis)
        assertEquals(1234L, entities.review?.updatedAtMillis)
    }
}
