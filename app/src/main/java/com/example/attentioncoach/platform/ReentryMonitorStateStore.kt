package com.example.attentioncoach.platform

import android.content.Context
import android.content.SharedPreferences
import com.example.attentioncoach.domain.FocusPresence

data class ReentryMonitorState(
    val active: Boolean,
    val taskId: Long,
    val taskTitle: String,
    val presence: FocusPresence,
    val violationStartedAtMillis: Long?,
    val lastNotificationMillis: Long?,
    val reentryCooldownMillis: Long
)

class ReentryMonitorStateStore(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun read(): ReentryMonitorState? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val taskId = preferences.getLong(KEY_TASK_ID, -1L)
            .takeIf { it > 0L }
            ?: return null
        val presence = preferences.getString(KEY_PRESENCE, null)
            ?.let { runCatching { FocusPresence.valueOf(it) }.getOrNull() }
            ?: FocusPresence.UNKNOWN
        return ReentryMonitorState(
            active = true,
            taskId = taskId,
            taskTitle = preferences.getString(KEY_TASK_TITLE, null).orEmpty(),
            presence = presence,
            violationStartedAtMillis = nullableLong(KEY_VIOLATION_STARTED_AT),
            lastNotificationMillis = nullableLong(KEY_LAST_NOTIFICATION_AT),
            reentryCooldownMillis = preferences.getLong(KEY_REENTRY_COOLDOWN_MILLIS, DEFAULT_REENTRY_COOLDOWN_MILLIS)
        )
    }

    fun save(state: ReentryMonitorState) {
        preferences.edit()
            .putBoolean(KEY_ACTIVE, state.active)
            .putLong(KEY_TASK_ID, state.taskId)
            .putString(KEY_TASK_TITLE, state.taskTitle)
            .putString(KEY_PRESENCE, state.presence.name)
            .putNullableLong(KEY_VIOLATION_STARTED_AT, state.violationStartedAtMillis)
            .putNullableLong(KEY_LAST_NOTIFICATION_AT, state.lastNotificationMillis)
            .putLong(KEY_REENTRY_COOLDOWN_MILLIS, state.reentryCooldownMillis)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun nullableLong(key: String): Long? {
        if (!preferences.contains(key)) return null
        return preferences.getLong(key, -1L)
    }

    private fun SharedPreferences.Editor.putNullableLong(key: String, value: Long?): SharedPreferences.Editor {
        return if (value == null) remove(key) else putLong(key, value)
    }

    private companion object {
        const val PREFERENCES_NAME = "attention_coach_reentry_monitor_v2"
        const val KEY_ACTIVE = "active"
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_PRESENCE = "presence"
        const val KEY_VIOLATION_STARTED_AT = "violation_started_at"
        const val KEY_LAST_NOTIFICATION_AT = "last_notification_at"
        const val KEY_REENTRY_COOLDOWN_MILLIS = "reentry_cooldown_millis"
        const val DEFAULT_REENTRY_COOLDOWN_MILLIS = 30_000L
    }
}
