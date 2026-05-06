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
import androidx.compose.ui.platform.LocalContext
import com.example.attentioncoach.domain.DemoTaskRepository
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.TopLevelDestination
import com.example.attentioncoach.platform.launchNeededApp
import java.time.LocalDate

@Composable
fun AttentionCoachApp() {
    val context = LocalContext.current
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }
    var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 5, 5)) }
    var tasks by remember { mutableStateOf(DemoTaskRepository.seed()) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var activeWorkTaskId by remember { mutableStateOf<Long?>(null) }
    var paused by remember { mutableStateOf(false) }
    val selectedTask = selectedTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    val activeWorkTask = activeWorkTaskId?.let { id -> tasks.firstOrNull { it.id == id } }

    if (activeWorkTask != null) {
        if (paused) {
            PauseScreen(onResume = { paused = false })
        } else {
            WorkScreen(
                task = activeWorkTask,
                onPause = { paused = true },
                onExit = {
                    activeWorkTaskId = null
                    paused = false
                    destination = TopLevelDestination.TASKS
                },
                onNeededAppSelected = { launchNeededApp(context, it) }
            )
        }
        return
    }

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
            onTaskSelected = { selectedTaskId = it },
            onSeedDemo = { tasks = DemoTaskRepository.seed() }
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
            onStartWork = {
                activeWorkTaskId = it
                selectedTaskId = null
            }
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
    onTaskSelected: (Long) -> Unit,
    onSeedDemo: () -> Unit
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

        TopLevelDestination.INSIGHTS -> InsightsScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        TopLevelDestination.SETTINGS -> SettingsScreen(
            onSeedDemo = onSeedDemo,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

private fun TopLevelDestination.iconText(): String {
    return when (this) {
        TopLevelDestination.TASKS -> "T"
        TopLevelDestination.INSIGHTS -> "I"
        TopLevelDestination.SETTINGS -> "S"
    }
}

private fun List<PlannedTask>.replaceTask(updated: PlannedTask): List<PlannedTask> {
    return map { if (it.id == updated.id) updated else it }
}
