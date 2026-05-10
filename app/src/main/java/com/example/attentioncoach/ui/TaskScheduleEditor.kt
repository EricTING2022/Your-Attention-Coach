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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.attentioncoach.domain.ScheduleOptions
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
    var customDurationText by remember(initialDurationMinutes) {
        mutableStateOf(if (initialDurationMinutes in ScheduleOptions.durationMinutes) "" else initialDurationMinutes.toString())
    }
    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = selectedHour)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinute / 5)

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
                        .height(222.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
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

                Text("DURATION", color = UiTokens.LowChipText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                DurationGrid(
                    selectedDuration = selectedDuration,
                    customDurationText = customDurationText,
                    onPresetSelected = {
                        selectedDuration = it
                        customDurationText = ""
                    },
                    onCustomChanged = { value ->
                        customDurationText = value.filter(Char::isDigit).take(3)
                        ScheduleOptions.customDurationFromInput(customDurationText)?.let {
                            selectedDuration = it
                        }
                    }
                )
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val wheelHeight = 210.dp
    val itemHeight = 54.dp
    val centerPadding = (wheelHeight - itemHeight) / 2
    val itemHeightPx = with(density) { itemHeight.roundToPx() }

    fun centeredIndex(): Int {
        val offsetIndex = if (state.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
        return (state.firstVisibleItemIndex + offsetIndex).coerceIn(values.indices)
    }

    LaunchedEffect(state, values) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect {
                onSelected(values[centeredIndex()])
            }
    }

    LaunchedEffect(state, values) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (!isScrolling) {
                    state.animateScrollToItem(centeredIndex())
                }
            }
    }

    Box(modifier = modifier.height(wheelHeight), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(UiTokens.LowChipBg)
        )
        LazyColumn(
            state = state,
            modifier = Modifier.height(wheelHeight),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = centerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(values) { index, value ->
                val selected = value == selectedValue
                Text(
                    text = label(value),
                    color = if (selected) UiTokens.LowChipText else UiTokens.InkSoft.copy(alpha = 0.55f),
                    fontSize = if (selected) 24.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            onSelected(value)
                            scope.launch { state.animateScrollToItem(index) }
                        }
                        .padding(vertical = 11.dp)
                )
            }
        }
    }
}

private fun defaultStartTime(): LocalTime {
    val now = LocalTime.now()
    return now.withMinute((now.minute / 5) * 5).withSecond(0).withNano(0)
}

@Composable
private fun DurationGrid(
    selectedDuration: Int,
    customDurationText: String,
    onPresetSelected: (Int) -> Unit,
    onCustomChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScheduleOptions.durationMinutes.take(3).forEach { minutes ->
                DurationOption(
                    minutes = minutes,
                    selected = selectedDuration == minutes,
                    onClick = { onPresetSelected(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScheduleOptions.durationMinutes.drop(3).forEach { minutes ->
                DurationOption(
                    minutes = minutes,
                    selected = selectedDuration == minutes,
                    onClick = { onPresetSelected(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
            CustomDurationOption(
                value = customDurationText,
                selected = customDurationText.isNotBlank() && selectedDuration !in ScheduleOptions.durationMinutes,
                onValueChange = onCustomChanged,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DurationOption(minutes: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier
            .then(modifier)
            .height(70.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) UiTokens.LowChipBg else Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(70.dp)) {
            Text(
                text = "${minutes} min",
                color = if (selected) UiTokens.LowChipText else UiTokens.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun CustomDurationOption(
    value: String,
    selected: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .then(modifier)
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) UiTokens.LowChipBg else Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                placeholder = {
                    Text(
                        "Custom",
                        color = UiTokens.InkSoft.copy(alpha = 0.58f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                suffix = { Text("min", color = UiTokens.LowChipText, fontWeight = FontWeight.Bold) },
                textStyle = TextStyle(
                    color = if (selected) UiTokens.LowChipText else UiTokens.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UiTokens.LowChipText,
                    unfocusedBorderColor = UiTokens.Outline,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = UiTokens.LowChipText
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
