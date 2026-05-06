package com.example.attentioncoach.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class UsageStatsBoundary(private val context: Context) {
    fun latestForegroundPackage(sinceMillis: Long, nowMillis: Long): String? {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val events = manager.queryEvents(sinceMillis, nowMillis)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType.isForegroundEvent()) {
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    @Suppress("DEPRECATION")
    private fun Int.isForegroundEvent(): Boolean {
        return this == UsageEvents.Event.ACTIVITY_RESUMED ||
            this == UsageEvents.Event.MOVE_TO_FOREGROUND
    }
}
