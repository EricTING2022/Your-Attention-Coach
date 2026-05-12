package com.example.attentioncoach.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun localTimeToMinuteOfDay(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun minuteOfDayToLocalTime(minuteOfDay: Int?): LocalTime? {
        return minuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
    }
}
