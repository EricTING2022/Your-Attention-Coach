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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attentioncoach.R
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.AppSettingsDefaults
import com.example.attentioncoach.domain.DailyInsight
import com.example.attentioncoach.domain.InsightRules
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.SettingsDisplayRules
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class SettingsPane {
    HOME,
    WHITELIST,
    INTERVAL
}

private val ReasonColors = listOf(
    UiTokens.GoogleBlue,
    Color(0xFFEA4335),
    Color(0xFFFBBC04),
    UiTokens.GoogleGreen,
    UiTokens.UrgentChipText,
    UiTokens.InkSoft
)

@Composable
fun InsightsScreen(
    tasks: List<PlannedTask>,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val insight = InsightRules.weeklySummary(tasks, selectedDate)
    val sessions = insight.daily.count { it.actualMinutes > 0 }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Insights", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Planned vs actual", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(UiTokens.ImportantChipBg)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(formatSessionCount(sessions), color = UiTokens.ImportantChipText, fontWeight = FontWeight.Bold)
                    }
                }
                WeeklyBarChart(insight.daily)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot("Planned", UiTokens.GoogleBlue)
                    LegendDot("Actual", Color(0xFFEA4335))
                }
            }
        }
        item {
            InfoCard {
                Text("Common reasons", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                insight.commonReasons.forEachIndexed { index, reason ->
                    ReasonRow(reason.reason, reason.count, ReasonColors[index % ReasonColors.size])
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    availableApps: List<NeededApp>,
    onAddNeededApp: (NeededApp) -> Unit,
    onRemoveNeededApp: (String) -> Unit,
    onNotificationIntervalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pane by remember { mutableStateOf(SettingsPane.HOME) }
    when (pane) {
        SettingsPane.HOME -> SettingsHome(
            settings = settings,
            onWhitelistClick = { pane = SettingsPane.WHITELIST },
            onIntervalClick = { pane = SettingsPane.INTERVAL },
            modifier = modifier
        )

        SettingsPane.WHITELIST -> AppsWhitelistScreen(
            settings = settings,
            availableApps = availableApps,
            onAddNeededApp = onAddNeededApp,
            onRemoveNeededApp = onRemoveNeededApp,
            onBack = { pane = SettingsPane.HOME },
            modifier = modifier
        )

        SettingsPane.INTERVAL -> NotificationIntervalScreen(
            settings = settings,
            onIntervalConfirmed = {
                onNotificationIntervalSelected(it)
                pane = SettingsPane.HOME
            },
            onBack = { pane = SettingsPane.HOME },
            modifier = modifier
        )
    }
}

@Composable
private fun SettingsHome(
    settings: AppSettings,
    onWhitelistClick: () -> Unit,
    onIntervalClick: () -> Unit,
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
                onWhitelistClick = onWhitelistClick,
                onIntervalClick = onIntervalClick
            )
        }
    }
}

@Composable
private fun AppsWhitelistScreen(
    settings: AppSettings,
    availableApps: List<NeededApp>,
    onAddNeededApp: (NeededApp) -> Unit,
    onRemoveNeededApp: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerOpen by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left_24),
                    contentDescription = "Back",
                    tint = UiTokens.Ink,
                    modifier = Modifier.size(42.dp).clickable(onClick = onBack)
                )
                Text(
                    "Apps whitelist",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (settings.neededApps.isEmpty()) {
                        Text(
                            "No apps in whitelist.",
                            color = UiTokens.InkSoft,
                            modifier = Modifier.padding(22.dp)
                        )
                    } else {
                        settings.neededApps.forEachIndexed { index, app ->
                            WhitelistAppRow(
                                app = app,
                                onRemove = { onRemoveNeededApp(app.packageName) }
                            )
                            if (index != settings.neededApps.lastIndex) {
                                HorizontalDivider(color = UiTokens.Outline, modifier = Modifier.padding(start = 116.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Text("+  Add app", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pickerOpen) {
        AppPickerDialog(
            availableApps = availableApps.filterNot { candidate ->
                settings.neededApps.any { it.packageName == candidate.packageName }
            },
            onDismiss = { pickerOpen = false },
            onAppSelected = {
                onAddNeededApp(it)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun NotificationIntervalScreen(
    settings: AppSettings,
    onIntervalConfirmed: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedInterval by remember(settings.notificationIntervalSeconds) {
        mutableStateOf(settings.notificationIntervalSeconds)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(UiTokens.Page).padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left_24),
                    contentDescription = "Back",
                    tint = UiTokens.Ink,
                    modifier = Modifier.size(42.dp).clickable(onClick = onBack)
                )
                Text(
                    "Notification interval",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 430.dp)) {
                    items(AppSettingsDefaults.notificationIntervalOptions) { seconds ->
                        IntervalOptionRow(
                            seconds = seconds,
                            selected = selectedInterval == seconds,
                            onClick = { selectedInterval = seconds }
                        )
                        if (seconds != AppSettingsDefaults.notificationIntervalOptions.last()) {
                            HorizontalDivider(color = UiTokens.Outline, modifier = Modifier.padding(horizontal = 28.dp))
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onIntervalConfirmed(selectedInterval) },
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Text("Confirm", fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
private fun WeeklyBarChart(days: List<DailyInsight>) {
    val maxMinutes = maxOf(1, days.maxOfOrNull { maxOf(it.plannedMinutes, it.actualMinutes) } ?: 1)
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            DayBars(
                day = day,
                maxMinutes = maxMinutes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayBars(
    day: DailyInsight,
    maxMinutes: Int,
    modifier: Modifier = Modifier
) {
    val plannedHeight = barHeight(day.plannedMinutes, maxMinutes)
    val actualHeight = barHeight(day.actualMinutes, maxMinutes)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(UiTokens.NeutralChipBg),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(plannedHeight.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(UiTokens.GoogleBlue)
                )
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(actualHeight.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFFEA4335))
                )
            }
        }
        Text(
            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            color = UiTokens.InkSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = UiTokens.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReasonRow(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = UiTokens.InkSoft, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(count.toString(), fontWeight = FontWeight.Bold)
    }
}

private fun barHeight(minutes: Int, maxMinutes: Int): Int {
    if (minutes <= 0) return 0
    return ((minutes.toFloat() / maxMinutes.toFloat()) * 132f).toInt().coerceAtLeast(8)
}

private fun formatSessionCount(count: Int): String {
    return if (count == 1) "1 session" else "$count sessions"
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

@Composable
private fun WhitelistAppRow(
    app: NeededApp,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(UiTokens.ImportantChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_apps_grid_24),
                contentDescription = null,
                tint = UiTokens.GoogleBlue,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(app.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(app.packageName, color = UiTokens.InkSoft, fontSize = 15.sp)
        }
        TextButton(onClick = onRemove) {
            Text("Remove", color = UiTokens.GoogleBlue, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IntervalOptionRow(
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            SettingsDisplayRules.intervalLabel(seconds),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun AppPickerDialog(
    availableApps: List<NeededApp>,
    onDismiss: () -> Unit,
    onAppSelected: (NeededApp) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add app") },
        text = {
            if (availableApps.isEmpty()) {
                Text("No available apps to add.", color = UiTokens.InkSoft)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(availableApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(UiTokens.ImportantChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_apps_grid_24),
                                    contentDescription = null,
                                    tint = UiTokens.GoogleBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(app.label, fontWeight = FontWeight.Bold)
                                Text(app.packageName, color = UiTokens.InkSoft, fontSize = 12.sp)
                            }
                            Text("Add", color = UiTokens.GoogleBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
