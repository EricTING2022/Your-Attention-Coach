package com.example.attentioncoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.attentioncoach.ui.AttentionCoachApp
import com.example.attentioncoach.ui.theme.AttentionCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttentionCoachTheme {
                AttentionCoachApp()
            }
        }
    }
}
