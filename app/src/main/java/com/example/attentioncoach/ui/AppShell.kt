package com.example.attentioncoach.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attentioncoach.domain.DemoTaskRepository
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.TopLevelDestination
import java.time.LocalDate

@Composable
fun AttentionCoachApp() {
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }
    var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 5, 5)) }
    var tasks by remember { mutableStateOf(DemoTaskRepository.seed()) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    val selectedTask = selectedTaskId?.let { id -> tasks.firstOrNull { it.id == id } }

    Scaffold(
        bottomBar = {
            AttentionBottomBar(
                selected = destination,
                onSelected = { destination = it }
            )
        }
    ) { padding ->
        TopLevelScreen(
            destination = destination,
            paddingValues = padding,
            selectedDate = selectedDate,
            tasks = tasks.filter { it.date == selectedDate },
            onDateSelected = { selectedDate = it },
            onTaskSelected = { selectedTaskId = it }
        )
    }

    if (selectedTask != null) {
        TaskDetailSheet(
            task = selectedTask,
            onDismiss = { selectedTaskId = null },
            onSavePlan = { updated -> tasks = tasks.replaceTask(updated) },
            onSaveReview = { taskId, completion, reason, adjustment ->
                tasks = tasks.map {
                    if (it.id == taskId) {
                        it.copy(
                            status = TaskStatus.REVIEWED,
                            actualCompletion = completion,
                            mismatchReason = reason,
                            nextAdjustment = adjustment
                        )
                    } else {
                        it
                    }
                }
                selectedTaskId = null
            },
            onStartWork = { selectedTaskId = null }
        )
    }
}

@Composable
private fun AttentionBottomBar(
    selected: TopLevelDestination,
    onSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = { Text(text = destination.iconText()) },
                label = { Text(text = destination.label) }
            )
        }
    }
}

@Composable
private fun TopLevelScreen(
    destination: TopLevelDestination,
    paddingValues: PaddingValues,
    selectedDate: LocalDate,
    tasks: List<PlannedTask>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskSelected: (Long) -> Unit
) {
    when (destination) {
        TopLevelDestination.TASKS -> TodayScreen(
            selectedDate = selectedDate,
            tasks = tasks,
            onDateSelected = onDateSelected,
            onTaskSelected = onTaskSelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        TopLevelDestination.INSIGHTS -> PlaceholderScreen(
            title = "This week",
            body = "Weekly insight cards will render here.",
            paddingValues = paddingValues
        )

        TopLevelDestination.SETTINGS -> PlaceholderScreen(
            title = "Preferences",
            body = "Usage access, notifications, and demo settings will render here.",
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    paddingValues: PaddingValues
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = body,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun TopLevelDestination.iconText(): String {
    return when (this) {
        TopLevelDestination.TASKS -> "✓"
        TopLevelDestination.INSIGHTS -> "↗"
        TopLevelDestination.SETTINGS -> "⚙"
    }
}

private fun List<PlannedTask>.replaceTask(updated: PlannedTask): List<PlannedTask> {
    return map { if (it.id == updated.id) updated else it }
}
