package com.example.attentioncoach.domain

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusMonitorRulesTest {
    @Test
    fun monitorPollsMoreOftenThanItCanNotify() {
        assertTrue(FocusMonitorCadence.POLL_INTERVAL_MILLIS < FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS)
    }

    @Test
    fun reentryBannerCooldownIsThirtySeconds() {
        assertEquals(30_000L, FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS)
    }

    @Test
    fun foregroundObservationPrefersEventPackage() {
        val packageName = ForegroundObservationRules.choosePackage(
            eventPackage = "com.android.chrome",
            rootPackage = "com.example.attentioncoach",
            windowPackages = listOf("com.google.android.apps.docs")
        )

        assertEquals("com.android.chrome", packageName)
    }

    @Test
    fun foregroundObservationFallsBackToRootPackage() {
        val packageName = ForegroundObservationRules.choosePackage(
            eventPackage = null,
            rootPackage = "com.android.chrome",
            windowPackages = listOf("com.google.android.apps.docs")
        )

        assertEquals("com.android.chrome", packageName)
    }

    @Test
    fun foregroundObservationFallsBackToFirstWindowPackage() {
        val packageName = ForegroundObservationRules.choosePackage(
            eventPackage = " ",
            rootPackage = null,
            windowPackages = listOf("", "com.android.launcher")
        )

        assertEquals("com.android.launcher", packageName)
    }

    @Test
    fun foregroundObservationIgnoresStaleObservation() {
        val observation = ForegroundObservation(
            packageName = "com.android.chrome",
            source = ForegroundSource.ACCESSIBILITY,
            observedAtMillis = 1_000L
        )

        assertEquals(
            null,
            ForegroundObservationRules.freshOrNull(
                observation = observation,
                nowMillis = 12_000L,
                maxAgeMillis = 10_000L
            )
        )
    }

    @Test
    fun foregroundObservationKeepsFreshObservationAndSource() {
        val observation = ForegroundObservation(
            packageName = "com.android.chrome",
            source = ForegroundSource.ACCESSIBILITY,
            observedAtMillis = 5_000L
        )

        val fresh = ForegroundObservationRules.freshOrNull(
            observation = observation,
            nowMillis = 12_000L,
            maxAgeMillis = 10_000L
        )

        assertEquals("com.android.chrome", fresh?.packageName)
        assertEquals(ForegroundSource.ACCESSIBILITY, fresh?.source)
    }
}
