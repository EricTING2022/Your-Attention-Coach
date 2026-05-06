package com.example.attentioncoach.ui

import com.example.attentioncoach.domain.Priority
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.shortMonthDay(): String {
    return "${month.getDisplayName(TextStyle.SHORT, Locale.US)} $dayOfMonth"
}

fun LocalDate.shortWeekday(): String {
    return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
}

fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours}h" else "${hours}h ${rest}m"
}

fun Priority.displayName(): String {
    return when (this) {
        Priority.URGENT_IMPORTANT -> "Urgent & important"
        Priority.URGENT -> "Urgent"
        Priority.IMPORTANT -> "Important"
        Priority.NOT_URGENT -> "Not urgent"
    }
}

