package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.ReviewAvailability
import com.example.attentioncoach.domain.TaskStatus
import kotlinx.coroutines.launch

@Composable
fun TaskDetailSheet(
    task: PlannedTask,
    isCreateMode: Boolean = false,
    onDismiss: () -> Unit,
    onSavePlan: (PlannedTask) -> Unit,
    onCreateTask: (PlannedTask) -> Unit = {},
    onDeleteTask: (Long) -> Unit = {},
    onSaveReview: (Long, String, String, String) -> Unit,
    onStartWork: (Long) -> Unit
) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var taskMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val canReview = !isCreateMode && ReviewAvailability.canReview(task.status)
    val pagerState = rememberPagerState(pageCount = { if (canReview) 2 else 1 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(canReview) {
        if (!canReview) pagerState.scrollToPage(0)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text("This task will be removed from the selected day.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteTask(task.id)
                    }
                ) {
                    Text("Delete", color = UiTokens.RedChipText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = UiTokens.GoogleBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = UiTokens.Page)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("<", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss))
                    Column(Modifier.weight(1f)) {
                        Text(task.date.shortMonthDay().uppercase(), color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isCreateMode) "New task" else task.title,
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!isCreateMode) {
                        Box {
                            Text(
                                "...",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { taskMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = taskMenuOpen,
                                onDismissRequest = { taskMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete", color = UiTokens.RedChipText, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        taskMenuOpen = false
                                        confirmDelete = true
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (canReview) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(UiTokens.NeutralChipBg)
                            .padding(5.dp)
                    ) {
                        Segment("Plan", pagerState.currentPage == 0, Modifier.weight(1f)) {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                        Segment("Review", pagerState.currentPage == 1, Modifier.weight(1f)) {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (canReview) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (page == 0) {
                                PlanPage(
                                    task = task,
                                    isCreateMode = isCreateMode,
                                    title = title,
                                    onTitleChange = { title = it },
                                    onSavePlan = onSavePlan,
                                    onCreateTask = onCreateTask,
                                    onStartWork = onStartWork
                                )
                            } else {
                                ReviewPage(task = task, onSaveReview = onSaveReview)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PlanPage(
                            task = task,
                            isCreateMode = isCreateMode,
                            title = title,
                            onTitleChange = { title = it },
                            onSavePlan = onSavePlan,
                            onCreateTask = onCreateTask,
                            onStartWork = onStartWork
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (selected) UiTokens.Ink else UiTokens.InkSoft,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
    }
}

@Composable
private fun PlanPage(
    task: PlannedTask,
    isCreateMode: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    onSavePlan: (PlannedTask) -> Unit,
    onCreateTask: (PlannedTask) -> Unit,
    onStartWork: (Long) -> Unit
) {
    var target by remember(task.id) { mutableStateOf(task.target) }
    var duration by remember(task.id) { mutableStateOf(task.durationMinutes.toString()) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var note by remember(task.id) { mutableStateOf(task.planningNote) }
    var priorityOpen by remember { mutableStateOf(false) }
    val priorityScroll = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (isCreateMode) {
            FieldLabel("Title", UiTokens.GoogleBlue)
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        FieldLabel("Targets", UiTokens.GoogleBlue)
        OutlinedTextField(value = target, onValueChange = { target = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Duration", UiTokens.LowChipText)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit) },
                    suffix = { Text("min", color = UiTokens.LowChipText, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("Priority", priority.color())
                Box {
                    OutlinedTextField(
                        value = priority.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Text("v") },
                        modifier = Modifier.fillMaxWidth().clickable { priorityOpen = true }
                    )
                    DropdownMenu(expanded = priorityOpen, onDismissRequest = { priorityOpen = false }) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 178.dp)
                                .verticalScroll(priorityScroll)
                                .padding(6.dp)
                        ) {
                            Priority.entries.forEach {
                                PriorityOption(
                                    priority = it,
                                    selected = it == priority,
                                    onClick = {
                                        priority = it
                                        priorityOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        FieldLabel("Planning note", UiTokens.UrgentChipText)
        OutlinedTextField(value = note, onValueChange = { note = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val updated = task.copy(
                    title = if (isCreateMode) title.trim().ifBlank { "Untitled task" } else task.title,
                    target = target,
                    durationMinutes = duration.toIntOrNull()?.coerceAtLeast(1) ?: task.durationMinutes,
                    priority = priority,
                    status = if (isCreateMode) TaskStatus.PLANNED else task.status,
                    planningNote = note
                )
                if (isCreateMode) {
                    onCreateTask(updated)
                } else {
                    onSavePlan(updated)
                    onStartWork(task.id)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleBlue),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        ) {
            Text(if (isCreateMode) "Save task" else "Start work block", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PriorityOption(priority: Priority, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) UiTokens.NeutralChipBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(priority.displayName(), color = priority.color(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReviewPage(
    task: PlannedTask,
    onSaveReview: (Long, String, String, String) -> Unit
) {
    var completion by remember(task.id) { mutableStateOf(task.actualCompletion) }
    var reason by remember(task.id) { mutableStateOf(task.mismatchReason) }
    var adjustment by remember(task.id) { mutableStateOf(task.nextAdjustment) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = UiTokens.LowChipBg), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("ACTUAL FOCUS", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${task.actualFocusMinutes} min", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        FieldLabel("Actual completion", UiTokens.GoogleBlue)
        OutlinedTextField(value = completion, onValueChange = { completion = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
        FieldLabel("Reason", UiTokens.LowChipText)
        OutlinedTextField(value = reason, onValueChange = { reason = it }, modifier = Modifier.fillMaxWidth())
        FieldLabel("Next adjustment", UiTokens.UrgentChipText)
        OutlinedTextField(value = adjustment, onValueChange = { adjustment = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onSaveReview(task.id, completion, reason, adjustment) },
            colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleBlue),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Save review", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FieldLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text.uppercase(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

private fun Priority.color(): androidx.compose.ui.graphics.Color {
    return when (this) {
        Priority.URGENT_IMPORTANT -> UiTokens.RedChipText
        Priority.URGENT -> UiTokens.UrgentChipText
        Priority.IMPORTANT -> UiTokens.ImportantChipText
        Priority.NOT_URGENT -> UiTokens.LowChipText
    }
}
