package com.example.attentioncoach.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TaskEntity::class, TaskReviewEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AttentionCoachDatabase : RoomDatabase() {
    abstract fun dao(): AttentionCoachDao
}
