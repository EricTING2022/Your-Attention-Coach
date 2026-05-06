package com.example.attentioncoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun TaskDetailSheet(
    task: PlannedTask,
    onDismiss: () -> Unit,
    onSavePlan: (PlannedTask) -> Unit,
    onSaveReview: (Long, String, String, String) -> Unit,
    onStartWork: (Long) -> Unit
) {
    var tab by remember(task.id) { mutableStateOf(DetailTab.Plan) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("<", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss))
                    Column {
                        Text(task.date.shortMonthDay().uppercase(), color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(task.title, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(UiTokens.NeutralChipBg)
                        .padding(5.dp)
                ) {
                    Segment("Plan", tab == DetailTab.Plan, Modifier.weight(1f)) { tab = DetailTab.Plan }
                    Segment("Review", tab == DetailTab.Review, Modifier.weight(1f)) { tab = DetailTab.Review }
                }
                Spacer(Modifier.height(16.dp))
                when (tab) {
                    DetailTab.Plan -> PlanPage(task = task, onSavePlan = onSavePlan, onStartWork = onStartWork)
                    DetailTab.Review -> ReviewPage(task = task, onSaveReview = onSaveReview)
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
    onSavePlan: (PlannedTask) -> Unit,
    onStartWork: (Long) -> Unit
) {
    var target by remember(task.id) { mutableStateOf(task.target) }
    var duration by remember(task.id) { mutableStateOf(task.durationMinutes.toString()) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var note by remember(task.id) { mutableStateOf(task.planningNote) }
    var priorityOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        Priority.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.displayName(), color = it.color(), fontWeight = FontWeight.Bold) },
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
        FieldLabel("Planning note", UiTokens.UrgentChipText)
        OutlinedTextField(value = note, onValueChange = { note = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                onSavePlan(task.copy(target = target, durationMinutes = duration.toIntOrNull() ?: task.durationMinutes, priority = priority, planningNote = note))
                onStartWork(task.id)
            },
            colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleBlue),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        ) {
            Text("Start work block", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
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

private enum class DetailTab { Plan, Review }

private fun Priority.color(): androidx.compose.ui.graphics.Color {
    return when (this) {
        Priority.URGENT_IMPORTANT -> UiTokens.RedChipText
        Priority.URGENT -> UiTokens.UrgentChipText
        Priority.IMPORTANT -> UiTokens.ImportantChipText
        Priority.NOT_URGENT -> UiTokens.LowChipText
    }
}
