package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ScheduleOptionsTest {
    @Test
    fun startTimesCoverFullDayInFiveMinuteSteps() {
        val options = ScheduleOptions.startTimes()

        assertEquals(288, options.size)
        assertEquals(LocalTime.MIDNIGHT, options.first())
        assertEquals(LocalTime.of(23, 55), options.last())
        assertEquals(LocalTime.of(7, 50), options[94])
    }

    @Test
    fun durationOptionsContainDemoFriendlyDurations() {
        assertEquals(listOf(15, 30, 45, 60, 90), ScheduleOptions.durationMinutes)
    }
}
