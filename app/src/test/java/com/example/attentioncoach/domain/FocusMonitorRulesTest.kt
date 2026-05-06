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
}
