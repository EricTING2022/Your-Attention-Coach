package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.attentioncoach.domain.ScheduleOptions
import java.time.LocalTime

@Composable
fun TaskScheduleEditor(
    initialStartTime: LocalTime?,
    initialDurationMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (LocalTime?, Int) -> Unit
) {
    val initialWheelTime = remember(initialStartTime) { initialStartTime ?: defaultStartTime() }
    var selectedHour by remember(initialStartTime) { mutableStateOf(initialWheelTime.hour) }
    var selectedMinute by remember(initialStartTime) { mutableStateOf((initialWheelTime.minute / 5) * 5) }
    var selectedDuration by remember(initialDurationMinutes) { mutableStateOf(initialDurationMinutes) }
    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedHour - 2).coerceAtLeast(0))
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = ((selectedMinute / 5) - 2).coerceAtLeast(0))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = UiTokens.Page)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "<",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("SCHEDULE", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Set time", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(UiTokens.GoogleGreen)
                            .clickable { onSave(LocalTime.of(selectedHour, selectedMinute), selectedDuration) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("START TIME", color = UiTokens.LowChipText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WheelColumn(
                            values = ScheduleOptions.hours,
                            selectedValue = selectedHour,
                            label = { it.toString() },
                            state = hourListState,
                            onSelected = { selectedHour = it },
                            modifier = Modifier.weight(1f)
                        )
                        WheelColumn(
                            values = ScheduleOptions.minutes,
                            selectedValue = selectedMinute,
                            label = { it.toString().padStart(2, '0') },
                            state = minuteListState,
                            onSelected = { selectedMinute = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Duration", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScheduleOptions.durationMinutes) { minutes ->
                        DurationOption(
                            minutes = minutes,
                            selected = selectedDuration == minutes,
                            onClick = { selectedDuration = minutes }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    values: List<Int>,
    selectedValue: Int,
    label: (Int) -> String,
    state: LazyListState,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        modifier = modifier.height(218.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(values) { value ->
            val selected = value == selectedValue
            Text(
                text = label(value),
                color = if (selected) UiTokens.LowChipText else UiTokens.InkSoft.copy(alpha = 0.55f),
                fontSize = if (selected) 24.sp else 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) UiTokens.LowChipBg else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 11.dp)
            )
        }
    }
}

private fun defaultStartTime(): LocalTime {
    val now = LocalTime.now()
    return now.withMinute((now.minute / 5) * 5).withSecond(0).withNano(0)
}

@Composable
private fun TimeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) UiTokens.LowChipBg else androidx.compose.ui.graphics.Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) UiTokens.LowChipText else UiTokens.Outline)
        )
        Text(
            text = text,
            color = if (selected) UiTokens.LowChipText else UiTokens.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun DurationOption(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = "${minutes} min",
        color = if (selected) UiTokens.LowChipText else UiTokens.InkSoft,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) UiTokens.LowChipBg else androidx.compose.ui.graphics.Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
