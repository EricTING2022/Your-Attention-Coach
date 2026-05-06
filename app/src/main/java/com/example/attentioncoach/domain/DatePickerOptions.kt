package com.example.attentioncoach.domain

import java.time.Month
import java.time.YearMonth

object DatePickerOptions {
    val years: List<Int> = (2020..2035).toList()
    val months: List<Month> = Month.entries.toList()

    fun withYear(current: YearMonth, year: Int): YearMonth {
        return YearMonth.of(year, current.month)
    }

    fun withMonth(current: YearMonth, month: Month): YearMonth {
        return YearMonth.of(current.year, month)
    }
}
