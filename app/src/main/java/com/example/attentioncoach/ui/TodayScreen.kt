package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.attentioncoach.R
import com.example.attentioncoach.domain.DatePickerOptions
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.SummaryCalculator
import com.example.attentioncoach.domain.TaskListSorter
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.WeekTimeline
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

@Composable
fun TodayScreen(
    selectedDate: LocalDate,
    tasks: List<PlannedTask>,
    unfinishedTaskDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskSelected: (Long) -> Unit,
    onToggleTaskComplete: (Long) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val summary = remember(tasks) { SummaryCalculator.forTasks(tasks) }
    val visibleTasks = remember(tasks) { TaskListSorter.sortForToday(tasks) }

    Box(modifier = modifier.fillMaxSize().background(UiTokens.Page)) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                WeekTimelineHeader(
                    selectedDate = selectedDate,
                    unfinishedTaskDates = unfinishedTaskDates,
                    onDateTitleClick = { showDatePicker = true },
                    onDateSelected = onDateSelected
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    SummaryCard("PLANNED FOCUS", formatMinutes(summary.plannedMinutes), Modifier.weight(1f))
                    SummaryCard("REVIEWED", "${summary.reviewedCount}/${summary.totalCount}", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text("Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        "Add",
                        color = UiTokens.GoogleBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onAddTask)
                    )
                }
            }
            items(visibleTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskSelected(task.id) },
                    onToggleComplete = { onToggleTaskComplete(task.id) }
                )
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if (showDatePicker) {
            DatePickerSheet(
                selectedDate = selectedDate,
                unfinishedTaskDates = unfinishedTaskDates,
                onDismiss = { showDatePicker = false },
                onDateSelected = {
                    onDateSelected(it)
                    showDatePicker = false
                }
            )
        }
    }
}

@Composable
private fun WeekTimelineHeader(
    selectedDate: LocalDate,
    unfinishedTaskDates: Set<LocalDate>,
    onDateTitleClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var dragTotal by remember { mutableStateOf(0f) }
    val week = WeekTimeline.weekFor(selectedDate)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UiTokens.Page)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onDateTitleClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${selectedDate.year}", color = UiTokens.DateAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(selectedDate.shortMonthDay(), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = UiTokens.DateAccent,
                modifier = Modifier.size(34.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(selectedDate) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragTotal < -45f) onDateSelected(selectedDate.plusDays(7))
                            if (dragTotal > 45f) onDateSelected(selectedDate.minusDays(7))
                            dragTotal = 0f
                        },
                        onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeekNavButton(
                painterId = R.drawable.ic_chevron_left_24,
                contentDescription = "Previous week",
                onClick = { onDateSelected(selectedDate.minusDays(7)) }
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    WeekDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        hasUnfinishedTask = date in unfinishedTaskDates,
                        onClick = { onDateSelected(date) }
                    )
                }
            }
            WeekNavButton(
                painterId = R.drawable.ic_chevron_right_24,
                contentDescription = "Next week",
                onClick = { onDateSelected(selectedDate.plusDays(7)) }
            )
        }
    }
}

@Composable
private fun WeekNavButton(painterId: Int, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
    ) {
        Icon(
            painter = painterResource(painterId),
            contentDescription = contentDescription,
            tint = UiTokens.InkSoft,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasUnfinishedTask: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(date.shortWeekday(), color = UiTokens.InkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) UiTokens.DateAccent else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${date.dayOfMonth}",
                color = if (isSelected) androidx.compose.ui.graphics.Color.White else UiTokens.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier.height(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasUnfinishedTask) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(UiTokens.GoogleBlue)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color = UiTokens.InkSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TaskCard(task: PlannedTask, onClick: () -> Unit, onToggleComplete: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, UiTokens.Outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = UiTokens.Ink,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted()) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Chip(text = "${task.durationMinutes} min", type = ChipType.Neutral)
                    Chip(text = task.priority.displayName(), type = task.priority.chipType())
                }
            }
            CompletionToggle(
                completed = task.isCompleted(),
                onClick = onToggleComplete
            )
        }
    }
}

@Composable
private fun CompletionToggle(completed: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (completed) UiTokens.GoogleBlue else UiTokens.Outline,
                shape = CircleShape
            )
            .background(
                if (completed) UiTokens.GoogleBlue.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(UiTokens.GoogleBlue)
            )
        }
    }
}

private fun PlannedTask.isCompleted(): Boolean {
    return status == TaskStatus.FINISHED || status == TaskStatus.REVIEWED
}

private enum class ChipType { Neutral, Red, Urgent, Important, Low }

@Composable
private fun Chip(text: String, type: ChipType) {
    val (bg, fg) = when (type) {
        ChipType.Neutral -> UiTokens.NeutralChipBg to UiTokens.NeutralChipText
        ChipType.Red -> UiTokens.RedChipBg to UiTokens.RedChipText
        ChipType.Urgent -> UiTokens.UrgentChipBg to UiTokens.UrgentChipText
        ChipType.Important -> UiTokens.ImportantChipBg to UiTokens.ImportantChipText
        ChipType.Low -> UiTokens.LowChipBg to UiTokens.LowChipText
    }
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

private fun Priority.chipType(): ChipType {
    return when (this) {
        Priority.URGENT_IMPORTANT -> ChipType.Red
        Priority.URGENT -> ChipType.Urgent
        Priority.IMPORTANT -> ChipType.Important
        Priority.NOT_URGENT -> ChipType.Low
    }
}

@Composable
private fun DatePickerSheet(
    selectedDate: LocalDate,
    unfinishedTaskDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var pickerMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var wheelMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(650.dp),
            shape = RoundedCornerShape(34.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { wheelMode = !wheelMode },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${pickerMonth.year}", color = UiTokens.DateAccent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                        Text(pickerMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(if (wheelMode) " v" else " >", color = UiTokens.DateAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = UiTokens.Page, contentColor = UiTokens.Ink)
                    ) {
                        Text("X")
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (wheelMode) {
                    YearMonthWheel(
                        pickerMonth = pickerMonth,
                        onYearSelected = {
                            pickerMonth = DatePickerOptions.withYear(pickerMonth, it)
                        },
                        onMonthSelected = {
                            pickerMonth = DatePickerOptions.withMonth(pickerMonth, it)
                            wheelMode = false
                        }
                    )
                } else {
                    MonthGrid(
                        pickerMonth = pickerMonth,
                        selectedDate = selectedDate,
                        unfinishedTaskDates = unfinishedTaskDates,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    pickerMonth: YearMonth,
    selectedDate: LocalDate,
    unfinishedTaskDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val first = pickerMonth.atDay(1)
    val offset = first.dayOfWeek.value % 7
    val dates = buildList<LocalDate?> {
        repeat(offset) { add(null) }
        for (day in 1..pickerMonth.lengthOfMonth()) add(pickerMonth.atDay(day))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
            Text(it, color = UiTokens.InkSoft, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(18.dp))
    LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(dates) { date ->
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .then(if (date != null) Modifier.clickable { onDateSelected(date) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (date != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (date == selectedDate) UiTokens.DateAccent else androidx.compose.ui.graphics.Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${date.dayOfMonth}",
                                color = if (date == selectedDate) androidx.compose.ui.graphics.Color.White else UiTokens.Ink,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier.height(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date in unfinishedTaskDates) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(UiTokens.GoogleBlue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearMonthWheel(
    pickerMonth: YearMonth,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Month) -> Unit
) {
    val yearState = rememberLazyListState(
        initialFirstVisibleItemIndex = DatePickerOptions.years.indexOf(pickerMonth.year).coerceAtLeast(0)
    )
    val monthState = rememberLazyListState(initialFirstVisibleItemIndex = pickerMonth.monthValue - 1)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(UiTokens.Page)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyColumn(
                state = yearState,
                modifier = Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 172.dp)
            ) {
                items(DatePickerOptions.years) { year ->
                    WheelOption(
                        text = "$year",
                        selected = year == pickerMonth.year,
                        onClick = { onYearSelected(year) }
                    )
                }
            }
            LazyColumn(
                state = monthState,
                modifier = Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 172.dp)
            ) {
                items(DatePickerOptions.months) { month ->
                    WheelOption(
                        text = month.displayName(),
                        selected = month == pickerMonth.month,
                        onClick = { onMonthSelected(month) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontSize = if (selected) 24.sp else 21.sp,
        color = if (selected) UiTokens.Ink else UiTokens.InkSoft.copy(alpha = 0.58f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    )
}

private fun Month.displayName(): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
}
