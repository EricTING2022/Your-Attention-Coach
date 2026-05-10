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

    @Test
    fun whitelistSummaryReflectsCurrentAppCount() {
        assertEquals("No apps", SettingsDisplayRules.whitelistSummary(0))
        assertEquals("1 app", SettingsDisplayRules.whitelistSummary(1))
        assertEquals("2 apps", SettingsDisplayRules.whitelistSummary(2))
        assertEquals("4 apps", SettingsDisplayRules.whitelistSummary(4))
    }

    @Test
    fun notificationIntervalLabelReflectsCurrentSeconds() {
        assertEquals("30s", SettingsDisplayRules.intervalLabel(30))
        assertEquals("1 min", SettingsDisplayRules.intervalLabel(60))
        assertEquals("2 min", SettingsDisplayRules.intervalLabel(120))
        assertEquals("5 min", SettingsDisplayRules.intervalLabel(300))
    }
}
