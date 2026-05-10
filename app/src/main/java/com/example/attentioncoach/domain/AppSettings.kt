package com.example.attentioncoach.domain

data class NeededApp(
    val packageName: String,
    val label: String
)

data class AppSettings(
    val neededApps: List<NeededApp> = AppSettingsDefaults.neededApps,
    val notificationIntervalSeconds: Int = AppSettingsDefaults.defaultNotificationIntervalSeconds
)

object AppSettingsDefaults {
    const val defaultNotificationIntervalSeconds = 30
    val notificationIntervalOptions = listOf(30, 60, 120, 300)
    val neededApps = listOf(
        NeededApp(packageName = "com.android.chrome", label = "Chrome"),
        NeededApp(packageName = "com.google.android.apps.docs", label = "Docs")
    )
}

object AppSettingsRules {
    fun addNeededApp(settings: AppSettings, app: NeededApp): AppSettings {
        if (settings.neededApps.any { it.packageName == app.packageName }) return settings
        return settings.copy(neededApps = settings.neededApps + app)
    }

    fun removeNeededApp(settings: AppSettings, packageName: String): AppSettings {
        return settings.copy(neededApps = settings.neededApps.filterNot { it.packageName == packageName })
    }

    fun withNotificationInterval(settings: AppSettings, seconds: Int): AppSettings {
        if (seconds !in AppSettingsDefaults.notificationIntervalOptions) return settings
        return settings.copy(notificationIntervalSeconds = seconds)
    }
}

object SettingsDisplayRules {
    fun whitelistSummary(count: Int): String {
        return when (count) {
            0 -> "No apps"
            1 -> "1 app"
            else -> "$count apps"
        }
    }

    fun intervalLabel(seconds: Int): String {
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            "$minutes min"
        }
    }
}
