package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InsightRulesTest {
    @Test
    fun weeklySummaryComparesPlannedAndActualMinutes() {
        val selectedDate = LocalDate.of(2026, 5, 6)
        val tasks = listOf(
            task(id = 1, date = LocalDate.of(2026, 5, 3), durationMinutes = 30, actualFocusMinutes = 25),
            task(id = 2, date = LocalDate.of(2026, 5, 9), durationMinutes = 40, actualFocusMinutes = 50),
            task(id = 3, date = LocalDate.of(2026, 5, 2), durationMinutes = 90, actualFocusMinutes = 90),
            task(id = 4, date = LocalDate.of(2026, 5, 10), durationMinutes = 120, actualFocusMinutes = 130)
        )

        val insight = InsightRules.weeklySummary(tasks, selectedDate)

        assertEquals(70, insight.plannedMinutes)
        assertEquals(75, insight.actualMinutes)
        assertEquals(5, insight.actualMinusPlannedMinutes)
        assertEquals(
            (3..9).map { LocalDate.of(2026, 5, it) },
            insight.daily.map { it.date }
        )
        assertEquals(30, insight.daily.first().plannedMinutes)
        assertEquals(40, insight.daily.last().plannedMinutes)
    }

    @Test
    fun weeklySummaryIncludesDefaultReasonsEvenWhenZero() {
        val selectedDate = LocalDate.of(2026, 5, 10)
        val tasks = listOf(
            task(id = 1, mismatchReason = "Attention faded"),
            task(id = 2, mismatchReason = "Attention faded"),
            task(id = 3, mismatchReason = "Task too large"),
            task(id = 4, mismatchReason = "")
        )

        val insight = InsightRules.weeklySummary(tasks, selectedDate)

        val reasonCounts = insight.commonReasons.associate { it.reason to it.count }
        ReviewReasonOptions.defaultReasons.forEach {
            check(reasonCounts.containsKey(it))
        }
        assertEquals(2, reasonCounts["Attention faded"])
        assertEquals(1, reasonCounts["Task too large"])
        assertEquals(0, reasonCounts["Entertainment app distraction"])
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
