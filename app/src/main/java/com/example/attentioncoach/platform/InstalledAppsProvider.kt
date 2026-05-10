package com.example.attentioncoach.platform

import android.content.Context
import android.content.Intent
import com.example.attentioncoach.domain.NeededApp

class InstalledAppsProvider(private val context: Context) {
    fun launchableApps(): List<NeededApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = context.packageManager
        @Suppress("DEPRECATION")
        val activities = packageManager.queryIntentActivities(intent, 0)

        return activities
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                NeededApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
