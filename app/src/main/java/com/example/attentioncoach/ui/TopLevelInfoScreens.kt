package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("This week", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        item {
            InfoCard {
                Text("SUGGESTED NEXT BLOCK", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("30 minutes", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Reviewed sessions stay strongest before the 35-minute mark.", color = UiTokens.InkSoft)
            }
        }
        item {
            InfoCard {
                Text("Planned vs actual", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Mon" to "62%", "Tue" to "69%", "Wed" to "58%", "Thu" to "64%").forEach {
                        Column(Modifier.weight(1f)) {
                            Text(it.first, color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
                            Text(it.second, color = UiTokens.GoogleBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            InfoCard {
                Text("Common reasons", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                ReasonRow("Attention faded", "3")
                ReasonRow("Entertainment app distraction", "2")
                ReasonRow("Task too large", "1")
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onSeedDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Preferences", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        item {
            InfoCard {
                SettingsRow("Selected leisure apps", "4")
                SettingsRow("Needed apps", "2")
                SettingsRow("Reminder sensitivity", "Medium")
                SettingsRow("Default block", "30 min")
                SettingsRow("Usage access", "Ready")
                SettingsRow("Notifications", "Ready")
            }
        }
        item {
            Button(onClick = onSeedDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Seed demo day", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun ReasonRow(label: String, count: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = UiTokens.InkSoft, modifier = Modifier.weight(1f))
        Text(count, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
    }
}
