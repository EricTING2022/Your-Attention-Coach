package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsRulesTest {
    @Test
    fun defaultSettingsUseChromeDocsAndThirtySecondInterval() {
        val settings = AppSettings()

        assertEquals(30, settings.notificationIntervalSeconds)
        assertEquals(
            listOf("com.android.chrome", "com.google.android.apps.docs"),
            settings.neededApps.map { it.packageName }
        )
    }

    @Test
    fun addNeededAppIgnoresDuplicatePackage() {
        val settings = AppSettingsRules.addNeededApp(
            AppSettings(),
            NeededApp(packageName = "com.android.chrome", label = "Chrome")
        )

        assertEquals(2, settings.neededApps.size)
    }

    @Test
    fun removeNeededAppRemovesMatchingPackage() {
        val settings = AppSettingsRules.removeNeededApp(AppSettings(), "com.google.android.apps.docs")

        assertFalse(settings.neededApps.any { it.packageName == "com.google.android.apps.docs" })
        assertTrue(settings.neededApps.any { it.packageName == "com.android.chrome" })
    }

    @Test
    fun notificationIntervalAcceptsSupportedValuesOnly() {
        val settings = AppSettings()

        assertEquals(60, AppSettingsRules.withNotificationInterval(settings, 60).notificationIntervalSeconds)
        assertEquals(30, AppSettingsRules.withNotificationInterval(settings, 45).notificationIntervalSeconds)
    }
}
