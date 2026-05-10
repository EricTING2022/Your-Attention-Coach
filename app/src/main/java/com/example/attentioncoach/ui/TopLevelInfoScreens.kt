package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attentioncoach.R
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.InsightRules
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.SettingsDisplayRules
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
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Preferences", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        item {
            SettingsMenuCard(
                whitelistSummary = SettingsDisplayRules.whitelistSummary(settings.neededApps.size),
                intervalSummary = SettingsDisplayRules.intervalLabel(settings.notificationIntervalSeconds),
                onWhitelistClick = { },
                onIntervalClick = { }
            )
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
private fun SettingsMenuCard(
    whitelistSummary: String,
    intervalSummary: String,
    onWhitelistClick: () -> Unit,
    onIntervalClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            SettingsMenuRow(
                iconRes = R.drawable.ic_apps_grid_24,
                label = "Apps whitelist",
                value = whitelistSummary,
                onClick = onWhitelistClick
            )
            HorizontalDivider(color = UiTokens.Outline)
            SettingsMenuRow(
                iconRes = R.drawable.ic_notifications_24,
                label = "Notification interval",
                value = intervalSummary,
                onClick = onIntervalClick
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(
    iconRes: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(UiTokens.ImportantChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = UiTokens.Ink,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
        )
        Text(value, color = UiTokens.InkSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = UiTokens.InkSoft,
            modifier = Modifier.padding(start = 14.dp).size(28.dp)
        )
    }
}
