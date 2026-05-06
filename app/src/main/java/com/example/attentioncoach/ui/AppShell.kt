package com.example.attentioncoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attentioncoach.domain.TopLevelDestination

@Composable
fun AttentionCoachApp() {
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }

    Scaffold(
        bottomBar = {
            AttentionBottomBar(
                selected = destination,
                onSelected = { destination = it }
            )
        }
    ) { padding ->
        TopLevelScreen(destination = destination, paddingValues = padding)
    }
}

@Composable
private fun AttentionBottomBar(
    selected: TopLevelDestination,
    onSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = { Text(text = destination.iconText()) },
                label = { Text(text = destination.label) }
            )
        }
    }
}

@Composable
private fun TopLevelScreen(
    destination: TopLevelDestination,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = destination.label,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = when (destination) {
                TopLevelDestination.TASKS -> "Today planning will render here."
                TopLevelDestination.INSIGHTS -> "Weekly insight cards will render here."
                TopLevelDestination.SETTINGS -> "Preferences will render here."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun TopLevelDestination.iconText(): String {
    return when (this) {
        TopLevelDestination.TASKS -> "✓"
        TopLevelDestination.INSIGHTS -> "↗"
        TopLevelDestination.SETTINGS -> "⚙"
    }
}

