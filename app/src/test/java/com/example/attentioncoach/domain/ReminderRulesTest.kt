package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderRulesTest {
    @Test
    fun triggerMillisUsesTaskDateAndStartTime() {
        val zone = ZoneId.of("America/Los_Angeles")

        val triggerMillis = ReminderRules.triggerAtMillis(
            date = LocalDate.of(2026, 5, 7),
            startTime = LocalTime.of(7, 50),
            zoneId = zone
        )

        assertEquals(
            ZonedDateTime.of(2026, 5, 7, 7, 50, 0, 0, zone).toInstant().toEpochMilli(),
            triggerMillis
        )
    }

    @Test
    fun taskWithoutStartTimeIsNotSchedulable() {
        assertFalse(ReminderRules.isSchedulable(null))
        assertTrue(ReminderRules.isSchedulable(LocalTime.of(9, 0)))
    }

    @Test
    fun pastStartTimeDoesNotCreateTriggerMillis() {
        val zone = ZoneId.of("America/Los_Angeles")
        val nowMillis = ZonedDateTime.of(2026, 5, 11, 2, 30, 0, 0, zone).toInstant().toEpochMilli()

        val triggerMillis = ReminderRules.futureTriggerAtMillisOrNull(
            date = LocalDate.of(2026, 5, 6),
            startTime = LocalTime.of(2, 35),
            zoneId = zone,
            nowMillis = nowMillis
        )

        assertEquals(null, triggerMillis)
    }

    @Test
    fun dueStartReminderIdsUseTaskDateAndStartTime() {
        val zone = ZoneId.of("America/Los_Angeles")
        val nowMillis = ZonedDateTime.of(2026, 5, 11, 2, 30, 0, 0, zone).toInstant().toEpochMilli()
        val tasks = listOf(
            task(id = 1, date = LocalDate.of(2026, 5, 11), startTime = LocalTime.of(2, 30)),
            task(id = 2, date = LocalDate.of(2026, 5, 11), startTime = LocalTime.of(2, 31)),
            task(id = 3, date = LocalDate.of(2026, 5, 10), startTime = LocalTime.of(23, 59)),
            task(id = 4, date = LocalDate.of(2026, 5, 11), startTime = null)
        )

        val dueIds = ReminderRules.dueStartReminderTaskIds(tasks, nowMillis, zone)

        assertEquals(listOf(1L, 3L), dueIds)
    }

    @Test
    fun dueStartReminderIdsIgnoreCompletedTasks() {
        val zone = ZoneId.of("America/Los_Angeles")
        val nowMillis = ZonedDateTime.of(2026, 5, 11, 2, 30, 0, 0, zone).toInstant().toEpochMilli()
        val tasks = listOf(
            task(id = 1, status = TaskStatus.PLANNED),
            task(id = 2, status = TaskStatus.FINISHED),
            task(id = 3, status = TaskStatus.REVIEWED)
        )

        val dueIds = ReminderRules.dueStartReminderTaskIds(tasks, nowMillis, zone)

        assertEquals(listOf(1L), dueIds)
    }

    @Test
    fun highestPriorityDueTaskSelectsTopPriority() {
        val tasks = listOf(
            task(id = 1, priority = Priority.IMPORTANT),
            task(id = 2, priority = Priority.URGENT),
            task(id = 3, priority = Priority.URGENT_IMPORTANT),
            task(id = 4, priority = Priority.NOT_URGENT)
        )

        val selected = ReminderRules.highestPriorityDueTask(tasks, setOf(1L, 2L, 3L, 4L))

        assertEquals(3L, selected?.id)
    }

    @Test
    fun highestPriorityDueTaskUsesCreationOrderWithinSamePriority() {
        val tasks = listOf(
            task(id = 3, priority = Priority.IMPORTANT),
            task(id = 1, priority = Priority.IMPORTANT),
            task(id = 2, priority = Priority.IMPORTANT)
        )

        val selected = ReminderRules.highestPriorityDueTask(tasks, setOf(1L, 2L, 3L))

        assertEquals(1L, selected?.id)
    }

    @Test
    fun highestPriorityDueTaskIgnoresCompletedAndUnknownTasks() {
        val tasks = listOf(
            task(id = 1, priority = Priority.URGENT_IMPORTANT, status = TaskStatus.FINISHED),
            task(id = 2, priority = Priority.URGENT, status = TaskStatus.REVIEWED),
            task(id = 3, priority = Priority.IMPORTANT, status = TaskStatus.PLANNED)
        )

        val selected = ReminderRules.highestPriorityDueTask(tasks, setOf(1L, 2L, 3L, 99L))

        assertEquals(3L, selected?.id)
    }

    @Test
    fun highestPriorityDueTaskReturnsNullWhenNoEligibleTaskExists() {
        val tasks = listOf(
            task(id = 1, priority = Priority.URGENT_IMPORTANT, status = TaskStatus.FINISHED)
        )

        val selected = ReminderRules.highestPriorityDueTask(tasks, setOf(1L, 99L))

        assertEquals(null, selected)
    }

    @Test
    fun startReminderActionIgnoresAcknowledgedTask() {
        assertEquals(
            StartReminderAction.IGNORE,
            ReminderRules.startReminderAction(isAcknowledged = true, focusActive = true)
        )
    }

    @Test
    fun startReminderActionDefersDuringActiveFocus() {
        assertEquals(
            StartReminderAction.DEFER,
            ReminderRules.startReminderAction(isAcknowledged = false, focusActive = true)
        )
    }

    @Test
    fun startReminderActionNotifiesWhenFocusIsInactive() {
        assertEquals(
            StartReminderAction.NOTIFY,
            ReminderRules.startReminderAction(isAcknowledged = false, focusActive = false)
        )
    }

    private fun task(
        id: Long,
        date: LocalDate = LocalDate.of(2026, 5, 11),
        startTime: LocalTime? = LocalTime.of(2, 0),
        status: TaskStatus = TaskStatus.PLANNED,
        priority: Priority = Priority.IMPORTANT
    ): PlannedTask {
        return PlannedTask(
            id = id,
            date = date,
            title = "Task $id",
            target = "Target",
            startTime = startTime,
            durationMinutes = 30,
            priority = priority,
            status = status
        )
    }
}
