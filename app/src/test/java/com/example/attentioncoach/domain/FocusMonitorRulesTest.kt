package com.example.attentioncoach.domain

import org.junit.Assert.assertFalse
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
    fun foregroundObservationPrefersRootPackage() {
        val packageName = ForegroundObservationRules.choosePackage(
            eventPackage = "com.sec.android.app.launcher",
            rootPackage = "com.android.chrome",
            windowPackages = listOf("com.google.android.apps.docs")
        )

        assertEquals("com.android.chrome", packageName)
    }

    @Test
    fun foregroundObservationFallsBackToEventPackageWhenRootIsMissing() {
        val packageName = ForegroundObservationRules.choosePackage(
            eventPackage = "com.android.chrome",
            rootPackage = null,
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

    @Test
    fun foregroundObservationRecordsPackageChangesImmediately() {
        assertTrue(
            ForegroundObservationRules.shouldRecord(
                lastPackageName = "com.sec.android.app.launcher",
                lastRecordedAtMillis = 10_000L,
                newPackageName = "com.android.chrome",
                nowMillis = 11_000L
            )
        )
    }

    @Test
    fun foregroundObservationSuppressesRapidDuplicatePackages() {
        assertEquals(
            false,
            ForegroundObservationRules.shouldRecord(
                lastPackageName = "com.android.chrome",
                lastRecordedAtMillis = 10_000L,
                newPackageName = "com.android.chrome",
                nowMillis = 12_000L
            )
        )
    }

    @Test
    fun foregroundObservationRecordsDuplicateAfterThrottleWindow() {
        assertTrue(
            ForegroundObservationRules.shouldRecord(
                lastPackageName = "com.android.chrome",
                lastRecordedAtMillis = 10_000L,
                newPackageName = "com.android.chrome",
                nowMillis = 15_000L
            )
        )
    }

    @Test
    fun presenceClassifierLetsLifecycleForegroundWin() {
        val presence = ForegroundPresenceClassifier.classify(
            attentionCoachInForeground = true,
            observation = ForegroundObservation(
                packageName = "com.android.chrome",
                source = ForegroundSource.ACCESSIBILITY,
                observedAtMillis = 10_000L
            ),
            nowMillis = 11_000L,
            appPackage = "com.example.attentioncoach",
            whitelistPackages = setOf("com.android.chrome"),
            launcherPackages = setOf("com.sec.android.app.launcher")
        )

        assertEquals(FocusPresence.IN_ATTENTION_COACH, presence)
    }

    @Test
    fun presenceClassifierRecognizesAppPackage() {
        val presence = classifyFresh("com.example.attentioncoach")

        assertEquals(FocusPresence.IN_ATTENTION_COACH, presence)
    }

    @Test
    fun presenceClassifierRecognizesWhitelistPackage() {
        val presence = classifyFresh("com.android.chrome")

        assertEquals(FocusPresence.IN_WHITELIST_APP, presence)
    }

    @Test
    fun presenceClassifierRecognizesLauncherPackage() {
        val presence = classifyFresh("com.sec.android.app.launcher")

        assertEquals(FocusPresence.IN_LAUNCHER, presence)
    }

    @Test
    fun presenceClassifierRecognizesOtherPackage() {
        val presence = classifyFresh("com.openrice.android")

        assertEquals(FocusPresence.IN_OTHER_APP, presence)
    }

    @Test
    fun presenceClassifierTreatsSystemUiAsUnknown() {
        val presence = classifyFresh("com.android.systemui")

        assertEquals(FocusPresence.UNKNOWN, presence)
    }

    @Test
    fun presenceMemoryKeepsLastStablePresenceWhenObservationIsUnknown() {
        val presence = ForegroundPresenceMemory.resolve(
            classifiedPresence = FocusPresence.UNKNOWN,
            lastStablePresence = FocusPresence.IN_ATTENTION_COACH
        )

        assertEquals(FocusPresence.IN_ATTENTION_COACH, presence)
    }

    @Test
    fun presenceMemoryUpdatesWhenObservationIsStable() {
        val presence = ForegroundPresenceMemory.resolve(
            classifiedPresence = FocusPresence.IN_LAUNCHER,
            lastStablePresence = FocusPresence.IN_ATTENTION_COACH
        )

        assertEquals(FocusPresence.IN_LAUNCHER, presence)
    }

    @Test
    fun presenceMemoryKeepsUnknownWhenNoStablePresenceExists() {
        val presence = ForegroundPresenceMemory.resolve(
            classifiedPresence = FocusPresence.UNKNOWN,
            lastStablePresence = null
        )

        assertEquals(FocusPresence.UNKNOWN, presence)
    }

    @Test
    fun screenOnReentryWaitsDuringGracePeriod() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.IN_LAUNCHER,
            nowMillis = 11_000L,
            violationStartedAtMillis = 10_000L,
            lastNotificationMillis = null
        )

        assertFalse(decision.shouldNotify)
        assertFalse(decision.shouldClearNotification)
        assertEquals(10_000L, decision.nextViolationStartedAtMillis)
        assertEquals(ReentryReason.GRACE_PERIOD, decision.reason)
    }

    @Test
    fun screenOnReentryNotifiesAfterGracePeriod() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.IN_OTHER_APP,
            nowMillis = 14_000L,
            violationStartedAtMillis = 10_000L,
            lastNotificationMillis = null
        )

        assertTrue(decision.shouldNotify)
        assertEquals(10_000L, decision.nextViolationStartedAtMillis)
        assertEquals(14_000L, decision.nextLastNotificationMillis)
        assertEquals(ReentryReason.NON_NEEDED_APP, decision.reason)
    }

    @Test
    fun screenOnReentryUsesCooldownForRepeatedReminders() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.IN_LAUNCHER,
            nowMillis = 20_000L,
            violationStartedAtMillis = 10_000L,
            lastNotificationMillis = 15_000L,
            reentryCooldownMillis = 30_000L
        )

        assertFalse(decision.shouldNotify)
        assertEquals(15_000L, decision.nextLastNotificationMillis)
        assertEquals(ReentryReason.COOLDOWN, decision.reason)
    }

    @Test
    fun screenOnReentryClearsWhenBackInAttentionCoach() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.IN_ATTENTION_COACH,
            nowMillis = 20_000L,
            violationStartedAtMillis = 10_000L,
            lastNotificationMillis = 15_000L
        )

        assertFalse(decision.shouldNotify)
        assertTrue(decision.shouldClearNotification)
        assertEquals(null, decision.nextViolationStartedAtMillis)
        assertEquals(null, decision.nextLastNotificationMillis)
        assertEquals(ReentryReason.SELF, decision.reason)
    }

    @Test
    fun screenOnReentryClearsVisibleReminderInWhitelistApp() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.IN_WHITELIST_APP,
            nowMillis = 20_000L,
            violationStartedAtMillis = 10_000L,
            lastNotificationMillis = 15_000L
        )

        assertFalse(decision.shouldNotify)
        assertTrue(decision.shouldClearNotification)
        assertEquals(null, decision.nextViolationStartedAtMillis)
        assertEquals(null, decision.nextLastNotificationMillis)
        assertEquals(ReentryReason.NEEDED_APP, decision.reason)
    }

    @Test
    fun screenOnReentryDoesNotTreatUnknownAsWhitelist() {
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = FocusPresence.UNKNOWN,
            nowMillis = 20_000L,
            violationStartedAtMillis = null,
            lastNotificationMillis = null
        )

        assertFalse(decision.shouldNotify)
        assertFalse(decision.shouldClearNotification)
        assertEquals(ReentryReason.UNKNOWN, decision.reason)
    }

    @Test
    fun presenceClassifierMarksStaleObservationUnknown() {
        val presence = ForegroundPresenceClassifier.classify(
            attentionCoachInForeground = false,
            observation = ForegroundObservation(
                packageName = "com.android.chrome",
                source = ForegroundSource.ACCESSIBILITY,
                observedAtMillis = 1_000L
            ),
            nowMillis = 20_000L,
            appPackage = "com.example.attentioncoach",
            whitelistPackages = setOf("com.android.chrome"),
            launcherPackages = setOf("com.sec.android.app.launcher")
        )

        assertEquals(FocusPresence.UNKNOWN, presence)
    }

    @Test
    fun presenceClassifierMarksMissingObservationUnknown() {
        val presence = ForegroundPresenceClassifier.classify(
            attentionCoachInForeground = false,
            observation = null,
            nowMillis = 20_000L,
            appPackage = "com.example.attentioncoach",
            whitelistPackages = setOf("com.android.chrome"),
            launcherPackages = setOf("com.sec.android.app.launcher")
        )

        assertEquals(FocusPresence.UNKNOWN, presence)
    }

    private fun classifyFresh(packageName: String): FocusPresence {
        return ForegroundPresenceClassifier.classify(
            attentionCoachInForeground = false,
            observation = ForegroundObservation(
                packageName = packageName,
                source = ForegroundSource.ACCESSIBILITY,
                observedAtMillis = 10_000L
            ),
            nowMillis = 11_000L,
            appPackage = "com.example.attentioncoach",
            whitelistPackages = setOf("com.android.chrome"),
            launcherPackages = setOf("com.sec.android.app.launcher")
        )
    }
}
