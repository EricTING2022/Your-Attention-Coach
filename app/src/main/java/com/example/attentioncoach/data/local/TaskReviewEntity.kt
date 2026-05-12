package com.example.attentioncoach.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_reviews",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskReviewEntity(
    @PrimaryKey
    val taskId: Long,
    val actualFocusMinutes: Int,
    val actualCompletion: String,
    val mismatchReason: String,
    val nextAdjustment: String,
    val updatedAtMillis: Long
)
