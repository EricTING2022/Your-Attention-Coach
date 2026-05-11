package com.example.attentioncoach.platform

import android.content.Context
import android.content.SharedPreferences

class StartReminderStore(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun defer(taskId: Long) {
        updateIds(KEY_DEFERRED_TASK_IDS) { it + taskId }
        updateIds(KEY_ACTIVE_DUE_TASK_IDS) { it - taskId }
    }

    fun releaseDeferred(): Set<Long> {
        val deferred = deferredTaskIds()
        if (deferred.isEmpty()) return emptySet()
        writeIds(KEY_DEFERRED_TASK_IDS, emptySet())
        updateIds(KEY_ACTIVE_DUE_TASK_IDS) { it + deferred }
        return deferred
    }

    fun markActiveDue(taskId: Long) {
        updateIds(KEY_DEFERRED_TASK_IDS) { it - taskId }
        updateIds(KEY_ACTIVE_DUE_TASK_IDS) { it + taskId }
    }

    fun acknowledge(taskId: Long) {
        updateIds(KEY_ACKNOWLEDGED_TASK_IDS) { it + taskId }
        updateIds(KEY_DEFERRED_TASK_IDS) { it - taskId }
        updateIds(KEY_ACTIVE_DUE_TASK_IDS) { it - taskId }
    }

    fun clearAcknowledged(taskId: Long) {
        updateIds(KEY_ACKNOWLEDGED_TASK_IDS) { it - taskId }
    }

    fun isAcknowledged(taskId: Long): Boolean = taskId in ids(KEY_ACKNOWLEDGED_TASK_IDS)

    fun deferredTaskIds(): Set<Long> = ids(KEY_DEFERRED_TASK_IDS)

    fun activeDueIds(): Set<Long> = ids(KEY_ACTIVE_DUE_TASK_IDS)

    private fun updateIds(key: String, transform: (Set<Long>) -> Set<Long>) {
        writeIds(key, transform(ids(key)))
    }

    private fun ids(key: String): Set<Long> {
        return preferences.getStringSet(key, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    private fun writeIds(key: String, ids: Set<Long>) {
        preferences.edit()
            .putStringSet(key, ids.map { it.toString() }.toSet())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "attention_coach_start_reminders"
        const val KEY_DEFERRED_TASK_IDS = "deferred_task_ids"
        const val KEY_ACTIVE_DUE_TASK_IDS = "active_due_task_ids"
        const val KEY_ACKNOWLEDGED_TASK_IDS = "acknowledged_task_ids"
    }
}
