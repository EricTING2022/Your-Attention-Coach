package com.example.attentioncoach

import android.content.Context
import androidx.room.Room
import com.example.attentioncoach.data.RoomTaskRepository
import com.example.attentioncoach.data.TaskRepository
import com.example.attentioncoach.data.local.AttentionCoachDatabase

class AppContainer(
    applicationContext: Context
) {
    val database: AttentionCoachDatabase = Room.databaseBuilder(
        applicationContext,
        AttentionCoachDatabase::class.java,
        "attention_coach.db"
    ).build()

    val taskRepository: TaskRepository = RoomTaskRepository(database)
}
