package com.example.attentioncoach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.attentioncoach.platform.ReentryNotifier
import com.example.attentioncoach.ui.AttentionCoachApp
import com.example.attentioncoach.ui.theme.AttentionCoachTheme

class MainActivity : ComponentActivity() {
    private val reentryTaskId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureReentryIntent(intent)
        requestNotificationPermissionIfNeeded()
        setContent {
            AttentionCoachTheme {
                AttentionCoachApp(
                    reentryTaskId = reentryTaskId.value,
                    onReentryConsumed = { reentryTaskId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureReentryIntent(intent)
    }

    private fun captureReentryIntent(intent: Intent?) {
        if (intent?.action != ReentryNotifier.ACTION_REENTRY) return
        val taskId = intent.getLongExtra(ReentryNotifier.EXTRA_TASK_ID, -1L)
        reentryTaskId.value = taskId.takeIf { it > 0L }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 4521
    }
}
