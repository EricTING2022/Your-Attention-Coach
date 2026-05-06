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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
    var selectedStartTime by remember(initialStartTime) { mutableStateOf(initialStartTime) }
    var selectedDuration by remember(initialDurationMinutes) { mutableStateOf(initialDurationMinutes) }

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
                    Column(Modifier.weight(1f)) {
                        Text("Schedule", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Start time", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TimeOption(
                            text = "No start time",
                            selected = selectedStartTime == null,
                            onClick = { selectedStartTime = null }
                        )
                    }
                    items(ScheduleOptions.startTimes()) { time ->
                        TimeOption(
                            text = time.shortTimeLabel(),
                            selected = selectedStartTime == time,
                            onClick = { selectedStartTime = time }
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

                Button(
                    onClick = { onSave(selectedStartTime, selectedDuration) },
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Save schedule", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
