package com.example.attentioncoach.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithReview(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val review: TaskReviewEntity?
)
