package com.example.attentioncoach.platform

import android.content.Context
import android.widget.Toast

fun launchNeededApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent == null) {
        Toast.makeText(context, "Needed app is not installed on this device.", Toast.LENGTH_SHORT).show()
        return
    }
    context.startActivity(launchIntent)
}

