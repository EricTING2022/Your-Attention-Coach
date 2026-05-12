package com.example.attentioncoach

import android.content.Context
import androidx.room.Room
import com.example.attentioncoach.data.DataStoreSettingsRepository
import com.example.attentioncoach.data.RoomTaskRepository
import com.example.attentioncoach.data.SettingsRepository
import com.example.attentioncoach.data.TaskRepository
import com.example.attentioncoach.data.local.AttentionCoachDatabase
import com.example.attentioncoach.platform.FocusSessionStore
import com.example.attentioncoach.data.settingsDataStore
import com.example.attentioncoach.data.focusSessionDataStore

class AppContainer(
    applicationContext: Context
) {
    val database: AttentionCoachDatabase = Room.databaseBuilder(
        applicationContext,
        AttentionCoachDatabase::class.java,
        "attention_coach.db"
    ).build()

    val taskRepository: TaskRepository = RoomTaskRepository(database)

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(
        applicationContext.settingsDataStore
    )

    val focusSessionStore = FocusSessionStore(applicationContext.focusSessionDataStore)
}
