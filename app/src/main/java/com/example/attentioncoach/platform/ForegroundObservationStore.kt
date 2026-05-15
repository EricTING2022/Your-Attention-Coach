package com.example.attentioncoach.platform

import android.content.Context
import android.content.SharedPreferences
import com.example.attentioncoach.domain.ForegroundObservation
import com.example.attentioncoach.domain.ForegroundSource

class ForegroundObservationStore(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun read(): ForegroundObservation? {
        val packageName = preferences.getString(KEY_PACKAGE_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val source = preferences.getString(KEY_SOURCE, null)
            ?.let { runCatching { ForegroundSource.valueOf(it) }.getOrNull() }
            ?: return null
        val observedAtMillis = preferences.getLong(KEY_OBSERVED_AT, -1L)
            .takeIf { it >= 0L }
            ?: return null
        return ForegroundObservation(
            packageName = packageName,
            source = source,
            observedAtMillis = observedAtMillis
        )
    }

    fun save(observation: ForegroundObservation) {
        preferences.edit()
            .putString(KEY_PACKAGE_NAME, observation.packageName)
            .putString(KEY_SOURCE, observation.source.name)
            .putLong(KEY_OBSERVED_AT, observation.observedAtMillis)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "attention_coach_foreground_observation_v2"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_SOURCE = "source"
        const val KEY_OBSERVED_AT = "observed_at"
    }
}
