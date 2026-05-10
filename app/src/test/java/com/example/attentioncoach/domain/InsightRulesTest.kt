package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InsightRulesTest {
    @Test
    fun weeklySummaryComparesPlannedAndActualMinutes() {
        val selectedDate = LocalDate.of(2026, 5, 10)
        val tasks = listOf(
            task(id = 1, date = LocalDate.of(2026, 5, 10), durationMinutes = 30, actualFocusMinutes = 25),
            task(id = 2, date = LocalDate.of(2026, 5, 7), durationMinutes = 40, actualFocusMinutes = 50),
            task(id = 3, date = LocalDate.of(2026, 5, 2), durationMinutes = 90, actualFocusMinutes = 90)
        )

        val insight = InsightRules.weeklySummary(tasks, selectedDate)

        assertEquals(70, insight.plannedMinutes)
        assertEquals(75, insight.actualMinutes)
        assertEquals(5, insight.actualMinusPlannedMinutes)
    }

    @Test
    fun weeklySummaryCountsCommonNonBlankReasons() {
        val selectedDate = LocalDate.of(2026, 5, 10)
        val tasks = listOf(
            task(id = 1, mismatchReason = "Attention faded"),
            task(id = 2, mismatchReason = "Attention faded"),
            task(id = 3, mismatchReason = "Task was unclear"),
            task(id = 4, mismatchReason = "")
        )

        val insight = InsightRules.weeklySummary(tasks, selectedDate)

        assertEquals(
            listOf(
                ReasonCount("Attention faded", 2),
                ReasonCount("Task was unclear", 1)
            ),
            insight.commonReasons
        )
    }

    private fun task(
        id: Long,
        date: LocalDate = LocalDate.of(2026, 5, 10),
        durationMinutes: Int = 30,
        actualFocusMinutes: Int = 0,
        mismatchReason: String = ""
    ): PlannedTask {
        return PlannedTask(
            id = id,
            date = date,
            title = "Task $id",
            target = "Target",
            durationMinutes = durationMinutes,
            priority = Priority.IMPORTANT,
            status = TaskStatus.REVIEWED,
            actualFocusMinutes = actualFocusMinutes,
            mismatchReason = mismatchReason
        )
    }
}
