package com.example.attentioncoach.data

import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.NeededApp
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun addNeededApp(app: NeededApp)

    suspend fun removeNeededApp(packageName: String)

    suspend fun setNotificationInterval(seconds: Int)
}
