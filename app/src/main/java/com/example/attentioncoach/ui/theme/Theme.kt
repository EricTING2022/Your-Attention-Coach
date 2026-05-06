package com.example.attentioncoach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    error = Color(0xFFD93025),
    background = Color(0xFFF8FAFD),
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = Color(0xFF202124),
    onBackground = Color(0xFF202124)
)

@Composable
fun AttentionCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

