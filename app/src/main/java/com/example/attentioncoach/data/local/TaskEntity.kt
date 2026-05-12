package com.example.attentioncoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochDay: Long,
    val title: String,
    val target: String,
    val startMinuteOfDay: Int?,
    val durationMinutes: Int,
    val priorityName: String,
    val statusName: String,
    val isDemo: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
