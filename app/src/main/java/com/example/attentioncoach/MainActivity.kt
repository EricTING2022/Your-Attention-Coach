package com.example.attentioncoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.attentioncoach.ui.theme.AttentionCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttentionCoachTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AttentionCoachApp()
                }
            }
        }
    }
}

@Composable
fun AttentionCoachApp() {
    Text(text = "Attention Coach")
}

