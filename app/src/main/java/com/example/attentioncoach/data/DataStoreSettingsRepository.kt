package com.example.attentioncoach.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.AppSettingsDefaults
import com.example.attentioncoach.domain.AppSettingsRules
import com.example.attentioncoach.domain.NeededApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun addNeededApp(app: NeededApp) {
        dataStore.edit { preferences ->
            val updated = AppSettingsRules.addNeededApp(preferences.toSettings(), app)
            preferences.writeSettings(updated)
        }
    }

    override suspend fun removeNeededApp(packageName: String) {
        dataStore.edit { preferences ->
            val updated = AppSettingsRules.removeNeededApp(preferences.toSettings(), packageName)
            preferences.writeSettings(updated)
        }
    }

    override suspend fun setNotificationInterval(seconds: Int) {
        dataStore.edit { preferences ->
            val updated = AppSettingsRules.withNotificationInterval(preferences.toSettings(), seconds)
            preferences.writeSettings(updated)
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val storedApps = this[NEEDED_APPS_KEY]
        val interval = this[NOTIFICATION_INTERVAL_KEY]
            ?.takeIf { it in AppSettingsDefaults.notificationIntervalOptions }
            ?: AppSettingsDefaults.defaultNotificationIntervalSeconds
        return AppSettings(
            neededApps = storedApps?.mapNotNull(::decodeNeededApp)?.sortedBy { it.label }
                ?: AppSettingsDefaults.neededApps,
            notificationIntervalSeconds = interval
        )
    }

    private fun MutablePreferences.writeSettings(settings: AppSettings) {
        this[NEEDED_APPS_KEY] = settings.neededApps.map(::encodeNeededApp).toSet()
        this[NOTIFICATION_INTERVAL_KEY] = settings.notificationIntervalSeconds
    }

    private fun encodeNeededApp(app: NeededApp): String {
        return app.packageName + APP_SEPARATOR + app.label
    }

    private fun decodeNeededApp(encoded: String): NeededApp? {
        val separatorIndex = encoded.indexOf(APP_SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == encoded.lastIndex) return null
        return NeededApp(
            packageName = encoded.substring(0, separatorIndex),
            label = encoded.substring(separatorIndex + 1)
        )
    }

    private companion object {
        const val APP_SEPARATOR = "\u001F"
        val NEEDED_APPS_KEY = stringSetPreferencesKey("needed_apps")
        val NOTIFICATION_INTERVAL_KEY = intPreferencesKey("notification_interval_seconds")
    }
}
