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
}
