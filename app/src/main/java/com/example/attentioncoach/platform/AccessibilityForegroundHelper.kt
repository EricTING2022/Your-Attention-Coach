package com.example.attentioncoach.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

class AccessibilityForegroundHelper(private val context: Context) {
    fun isForegroundObserverEnabled(): Boolean {
        val expected = ComponentName(context, AttentionCoachAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices
            .split(':')
            .any { flattened ->
                flattened.equals(expected.flattenToString(), ignoreCase = true) ||
                    flattened.equals(expected.flattenToShortString(), ignoreCase = true)
            }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
