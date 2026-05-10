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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.AppSettingsDefaults
import com.example.attentioncoach.domain.InsightRules
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import java.time.LocalDate

@Composable
fun InsightsScreen(
    tasks: List<PlannedTask>,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val insight = InsightRules.weeklySummary(tasks, selectedDate)
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("This week", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        item {
            InfoCard {
                Text("PLANNED VS ACTUAL", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${insight.actualMinusPlannedMinutes} min", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Actual focus minus planned focus over the last 7 days.", color = UiTokens.InkSoft)
            }
        }
        item {
            InfoCard {
                Text("Planned vs actual", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Planned" to "${insight.plannedMinutes}m", "Actual" to "${insight.actualMinutes}m").forEach {
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
                if (insight.commonReasons.isEmpty()) {
                    Text("No reviewed reasons yet.", color = UiTokens.InkSoft)
                } else {
                    insight.commonReasons.forEach {
                        ReasonRow(it.reason, it.count.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAddNeededApp: (NeededApp) -> Unit,
    onRemoveNeededApp: (String) -> Unit,
    onNotificationIntervalSelected: (Int) -> Unit,
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
                Text("Needed apps", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                settings.neededApps.forEach { app ->
                    SettingsActionRow(
                        label = app.label,
                        value = "Remove",
                        onClick = { onRemoveNeededApp(app.packageName) }
                    )
                }
                AppSettingsDefaults.neededApps
                    .filterNot { candidate -> settings.neededApps.any { it.packageName == candidate.packageName } }
                    .forEach { app ->
                        SettingsActionRow(
                            label = app.label,
                            value = "Add",
                            onClick = { onAddNeededApp(app) }
                        )
                    }
            }
        }
        item {
            InfoCard {
                Text("Notification interval", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AppSettingsDefaults.notificationIntervalOptions.forEach { seconds ->
                        val selected = seconds == settings.notificationIntervalSeconds
                        val label = if (seconds < 60) "${seconds}s" else "${seconds / 60}m"
                        if (selected) {
                            Button(onClick = { onNotificationIntervalSelected(seconds) }, modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(onClick = { onNotificationIntervalSelected(seconds) }, modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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

@Composable
private fun SettingsActionRow(label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text(value, color = UiTokens.GoogleBlue, fontWeight = FontWeight.Bold)
        }
    }
}
