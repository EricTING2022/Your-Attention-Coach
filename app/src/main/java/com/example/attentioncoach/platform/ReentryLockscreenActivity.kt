package com.example.attentioncoach.platform

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attentioncoach.MainActivity
import com.example.attentioncoach.ui.theme.AttentionCoachTheme

class ReentryLockscreenActivity : ComponentActivity() {
    private val taskId: Long
        get() = intent.getLongExtra(ReentryNotifier.EXTRA_TASK_ID, -1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLockscreenDisplay()
        val taskTitle = intent.getStringExtra(ReentryNotifier.EXTRA_TASK_TITLE)
            .orEmpty()
            .ifBlank { "your focus block" }

        setContent {
            AttentionCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Focus block running",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = taskTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openAttentionCoach() }
                        ) {
                            Text("Return to focus")
                        }
                    }
                }
            }
        }
    }

    private fun enableLockscreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun openAttentionCoach() {
        val id = taskId
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = ReentryNotifier.ACTION_REENTRY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (id > 0L) {
                putExtra(ReentryNotifier.EXTRA_TASK_ID, id)
            }
        }
        startActivity(activityIntent)
        finish()
    }
}
