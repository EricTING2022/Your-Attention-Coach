package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkSessionClockTest {
    @Test
    fun workTimerCountsDownFromPlannedDuration() {
        assertEquals("39:30", WorkSessionClock.workTimerText(plannedDurationMinutes = 40, activeMillis = 30_000L))
    }

    @Test
    fun workTimerShowsOvertimeAfterPlannedDuration() {
        assertEquals("+02:15", WorkSessionClock.workTimerText(plannedDurationMinutes = 40, activeMillis = 2_535_000L))
    }

    @Test
    fun pauseTimerStartsAtThreeMinutesAndClampsAtZero() {
        assertEquals("03:00", WorkSessionClock.pauseTimerText(pauseStartedAtMillis = 1_000L, nowMillis = 1_000L))
        assertEquals("00:00", WorkSessionClock.pauseTimerText(pauseStartedAtMillis = 1_000L, nowMillis = 190_000L))
    }

    @Test
    fun activeMillisExcludesPausedTime() {
        val work = ActiveWork(
            taskId = 1L,
            isActive = true,
            plannedDurationMinutes = 40,
            startedAtMillis = 0L,
            accumulatedActiveMillis = 20_000L,
            pauseStartedAtMillis = 30_000L,
            isPaused = true
        )

        assertEquals(20_000L, WorkSessionClock.activeMillisAt(work, nowMillis = 180_000L))
    }

    @Test
    fun focusMinutesRoundUp() {
        assertEquals(1, WorkSessionClock.focusMinutesFromMillis(1L))
        assertEquals(2, WorkSessionClock.focusMinutesFromMillis(60_001L))
    }
}
