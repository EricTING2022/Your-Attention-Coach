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

    @Test
    fun hourOptionsCoverZeroThroughTwentyThree() {
        assertEquals((0..23).toList(), ScheduleOptions.hours)
    }

    @Test
    fun minuteOptionsUseFiveMinuteSteps() {
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55), ScheduleOptions.minutes)
    }

    @Test
    fun customDurationInputKeepsPositiveMinuteDigitsOnly() {
        assertEquals(50, ScheduleOptions.customDurationFromInput("50 min"))
        assertEquals(123, ScheduleOptions.customDurationFromInput("1234"))
        assertEquals(null, ScheduleOptions.customDurationFromInput("0"))
        assertEquals(null, ScheduleOptions.customDurationFromInput("abc"))
    }

    @Test
    fun disabledStartTimeSavesNull() {
        val saved = ScheduleEditorRules.savedStartTime(
            startTimeEnabled = false,
            selectedStartTime = LocalTime.of(7, 50)
        )

        assertEquals(null, saved)
    }

    @Test
    fun enabledStartTimeSavesSelectedTime() {
        val saved = ScheduleEditorRules.savedStartTime(
            startTimeEnabled = true,
            selectedStartTime = LocalTime.of(7, 50)
        )

        assertEquals(LocalTime.of(7, 50), saved)
    }
}
