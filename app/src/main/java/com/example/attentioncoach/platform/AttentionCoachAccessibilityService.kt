package com.example.attentioncoach.platform

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.attentioncoach.domain.ForegroundObservation
import com.example.attentioncoach.domain.ForegroundObservationRules
import com.example.attentioncoach.domain.ForegroundSource

class AttentionCoachAccessibilityService : AccessibilityService() {
    private lateinit var store: ForegroundObservationStore

    override fun onCreate() {
        super.onCreate()
        store = ForegroundObservationStore(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPackage = event?.packageName?.toString()
        val rootPackage = activeRootPackage()
        val windowPackages = activeWindowPackages()
        val chosenPackage = ForegroundObservationRules.choosePackage(
            eventPackage = eventPackage,
            rootPackage = rootPackage,
            windowPackages = windowPackages
        )
        val nowMillis = System.currentTimeMillis()
        Log.d(
            TAG,
            "eventType=${event?.eventType} eventPackage=$eventPackage " +
                "rootPackage=$rootPackage windowPackages=$windowPackages " +
                "chosenPackage=$chosenPackage at=$nowMillis"
        )
        if (chosenPackage == null) return
        store.save(
            ForegroundObservation(
                packageName = chosenPackage,
                source = ForegroundSource.ACCESSIBILITY,
                observedAtMillis = nowMillis
            )
        )
    }

    override fun onInterrupt() = Unit

    private fun activeRootPackage(): String? {
        return runCatching {
            rootInActiveWindow?.packageName?.toString()
        }.getOrNull()
    }

    private fun activeWindowPackages(): List<String> {
        return runCatching {
            windows.mapNotNull { window ->
                window.root?.packageName?.toString()?.takeIf(String::isNotBlank)
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "AC_ForegroundV2"
    }
}
