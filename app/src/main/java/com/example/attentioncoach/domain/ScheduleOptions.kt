package com.example.attentioncoach.domain

import java.time.LocalTime

object ScheduleOptions {
    val durationMinutes: List<Int> = listOf(15, 30, 45, 60, 90)

    fun startTimes(): List<LocalTime> {
        return (0 until 24 * 60 step 5).map { minuteOfDay ->
            LocalTime.MIDNIGHT.plusMinutes(minuteOfDay.toLong())
        }
    }
}
