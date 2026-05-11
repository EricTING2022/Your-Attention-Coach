package com.example.attentioncoach.platform

import android.content.Context
import android.content.SharedPreferences

class FocusSessionStore(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun setActive(taskId: Long) {
        preferences.edit()
            .putBoolean(KEY_FOCUS_ACTIVE, true)
            .putLong(KEY_ACTIVE_TASK_ID, taskId)
            .apply()
    }

    fun clearActive() {
        preferences.edit()
            .putBoolean(KEY_FOCUS_ACTIVE, false)
            .remove(KEY_ACTIVE_TASK_ID)
            .apply()
    }

    fun isActive(): Boolean = preferences.getBoolean(KEY_FOCUS_ACTIVE, false)

    fun activeTaskId(): Long? {
        val taskId = preferences.getLong(KEY_ACTIVE_TASK_ID, NO_TASK_ID)
        return if (taskId == NO_TASK_ID) null else taskId
    }

    private companion object {
        const val PREFERENCES_NAME = "attention_coach_focus_session"
        const val KEY_FOCUS_ACTIVE = "focus_active"
        const val KEY_ACTIVE_TASK_ID = "active_task_id"
        const val NO_TASK_ID = -1L
    }
}
