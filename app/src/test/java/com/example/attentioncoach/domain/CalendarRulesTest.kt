package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarRulesTest {
    @Test
    fun todayUsesProvidedClock() {
        val zone = ZoneId.of("America/Los_Angeles")
        val clock = Clock.fixed(
            LocalDate.of(2026, 5, 6).atStartOfDay(zone).toInstant(),
            zone
        )

        assertEquals(LocalDate.of(2026, 5, 6), CalendarRules.today(clock))
    }

    @Test
    fun maySevenTwentyTwentySixIsThursday() {
        assertEquals(DayOfWeek.THURSDAY, LocalDate.of(2026, 5, 7).dayOfWeek)
    }

    @Test
    fun daysInMonthUsesActualMonthLength() {
        val days = CalendarRules.daysInMonth(YearMonth.of(2026, 5))

        assertEquals(31, days.size)
        assertEquals(LocalDate.of(2026, 5, 1), days.first())
        assertEquals(LocalDate.of(2026, 5, 31), days.last())
    }
}
