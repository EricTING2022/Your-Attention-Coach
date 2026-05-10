package com.example.attentioncoach.domain

import java.time.LocalTime

object ScheduleOptions {
    val durationMinutes: List<Int> = listOf(15, 30, 45, 60, 90)
    val hours: List<Int> = (0..23).toList()
    val minutes: List<Int> = (0..55 step 5).toList()

    fun startTimes(): List<LocalTime> {
        return (0 until 24 * 60 step 5).map { minuteOfDay ->
            LocalTime.MIDNIGHT.plusMinutes(minuteOfDay.toLong())
        }
    }
}
