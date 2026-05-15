package com.example.attentioncoach.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class LauncherPackagesProvider(private val context: Context) {
    fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val packageManager = context.packageManager
        val resolvedPackage = packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName

        @Suppress("DEPRECATION")
        val allHomePackages = packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }

        return (allHomePackages + listOfNotNull(resolvedPackage))
            .filter { it.isNotBlank() }
            .toSet()
    }
}
