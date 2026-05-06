package com.example.attentioncoach.domain

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

object CalendarRules {
    fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate {
        return LocalDate.now(clock)
    }

    fun daysInMonth(yearMonth: YearMonth): List<LocalDate> {
        return (1..yearMonth.lengthOfMonth()).map { day -> yearMonth.atDay(day) }
    }
}
