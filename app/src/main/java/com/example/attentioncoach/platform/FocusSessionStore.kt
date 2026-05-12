package com.example.attentioncoach.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.attentioncoach.data.focusSessionDataStore
import com.example.attentioncoach.domain.ActiveWork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class FocusSessionStore(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.focusSessionDataStore)

    val activeWork: Flow<ActiveWork?> = dataStore.data.map { preferences ->
        preferences.toActiveWork()
    }

    fun setActive(taskId: Long) {
        setActive(
            ActiveWork(
                taskId = taskId,
                isActive = true
            )
        )
    }

    fun setActive(work: ActiveWork) = runBlocking {
        dataStore.edit { preferences ->
            preferences[KEY_FOCUS_ACTIVE] = work.isActive
            preferences[KEY_ACTIVE_TASK_ID] = work.taskId
            preferences[KEY_PLANNED_DURATION_MINUTES] = work.plannedDurationMinutes
            preferences[KEY_STARTED_AT_MILLIS] = work.startedAtMillis
            preferences[KEY_ACCUMULATED_ACTIVE_MILLIS] = work.accumulatedActiveMillis
            preferences[KEY_PAUSE_STARTED_AT_MILLIS] = work.pauseStartedAtMillis ?: NO_VALUE
            preferences[KEY_IS_PAUSED] = work.isPaused
        }
    }

    fun clearActive() = runBlocking {
        dataStore.edit { preferences ->
            preferences.remove(KEY_FOCUS_ACTIVE)
            preferences.remove(KEY_ACTIVE_TASK_ID)
            preferences.remove(KEY_PLANNED_DURATION_MINUTES)
            preferences.remove(KEY_STARTED_AT_MILLIS)
            preferences.remove(KEY_ACCUMULATED_ACTIVE_MILLIS)
            preferences.remove(KEY_PAUSE_STARTED_AT_MILLIS)
            preferences.remove(KEY_IS_PAUSED)
        }
    }

    fun isActive(): Boolean = runBlocking {
        activeWork.first()?.isActive == true
    }

    fun activeTaskId(): Long? = runBlocking {
        activeWork.first()?.taskId
    }

    private fun Preferences.toActiveWork(): ActiveWork? {
        if (this[KEY_FOCUS_ACTIVE] != true) return null
        val taskId = this[KEY_ACTIVE_TASK_ID] ?: return null
        val pauseStartedAt = this[KEY_PAUSE_STARTED_AT_MILLIS]
            ?.takeIf { it != NO_VALUE }
        return ActiveWork(
            taskId = taskId,
            isActive = true,
            plannedDurationMinutes = this[KEY_PLANNED_DURATION_MINUTES] ?: 0,
            startedAtMillis = this[KEY_STARTED_AT_MILLIS] ?: 0L,
            accumulatedActiveMillis = this[KEY_ACCUMULATED_ACTIVE_MILLIS] ?: 0L,
            pauseStartedAtMillis = pauseStartedAt,
            isPaused = this[KEY_IS_PAUSED] ?: false
        )
    }

    private companion object {
        const val NO_VALUE = -1L
        val KEY_FOCUS_ACTIVE = booleanPreferencesKey("focus_active")
        val KEY_ACTIVE_TASK_ID = longPreferencesKey("active_task_id")
        val KEY_PLANNED_DURATION_MINUTES = intPreferencesKey("planned_duration_minutes")
        val KEY_STARTED_AT_MILLIS = longPreferencesKey("started_at_millis")
        val KEY_ACCUMULATED_ACTIVE_MILLIS = longPreferencesKey("accumulated_active_millis")
        val KEY_PAUSE_STARTED_AT_MILLIS = longPreferencesKey("pause_started_at_millis")
        val KEY_IS_PAUSED = booleanPreferencesKey("is_paused")
    }
}
