package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Month
import java.time.YearMonth

class DatePickerOptionsTest {
    @Test
    fun yearOptionsAreFiniteForTheProjectDemoRange() {
        assertEquals((2020..2035).toList(), DatePickerOptions.years)
    }

    @Test
    fun monthOptionsContainAllTwelveMonths() {
        assertEquals(12, DatePickerOptions.months.size)
        assertTrue(DatePickerOptions.months.contains(Month.SEPTEMBER))
        assertEquals(Month.JANUARY, DatePickerOptions.months.first())
        assertEquals(Month.DECEMBER, DatePickerOptions.months.last())
    }

    @Test
    fun selectingYearKeepsTheCurrentMonth() {
        assertEquals(
            YearMonth.of(2024, Month.SEPTEMBER),
            DatePickerOptions.withYear(YearMonth.of(2026, Month.SEPTEMBER), 2024)
        )
    }

    @Test
    fun selectingMonthKeepsTheCurrentYear() {
        assertEquals(
            YearMonth.of(2026, Month.DECEMBER),
            DatePickerOptions.withMonth(YearMonth.of(2026, Month.SEPTEMBER), Month.DECEMBER)
        )
    }
}
