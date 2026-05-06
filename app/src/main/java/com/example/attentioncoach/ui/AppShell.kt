package com.example.attentioncoach.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.attentioncoach.domain.DemoTaskRepository
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.TopLevelDestination
import com.example.attentioncoach.platform.FocusMonitorService
import com.example.attentioncoach.platform.launchNeededApp
import java.time.LocalDate

@Composable
fun AttentionCoachApp(
    reentryTaskId: Long? = null,
    onReentryConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val initialTasks = remember { DemoTaskRepository.seed() }
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }
    var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 5, 5)) }
    var tasks by remember { mutableStateOf(initialTasks) }
    var nextTaskId by remember { mutableStateOf(initialTasks.nextTaskId()) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var draftTask by remember { mutableStateOf<PlannedTask?>(null) }
    var activeWorkTaskId by remember { mutableStateOf<Long?>(null) }
    var paused by remember { mutableStateOf(false) }
    var reentryOpen by remember { mutableStateOf(false) }
    val selectedTask = selectedTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    val detailTask = selectedTask ?: draftTask
    val isCreateMode = selectedTask == null && draftTask != null
    val activeWorkTask = activeWorkTaskId?.let { id -> tasks.firstOrNull { it.id == id } }

    LaunchedEffect(reentryTaskId) {
        val taskId = reentryTaskId ?: return@LaunchedEffect
        if (tasks.any { it.id == taskId }) {
            activeWorkTaskId = taskId
            selectedTaskId = null
            destination = TopLevelDestination.TASKS
            paused = false
            reentryOpen = true
        }
        onReentryConsumed()
    }

    LaunchedEffect(activeWorkTask?.id, paused) {
        if (activeWorkTask != null && !paused) {
            FocusMonitorService.start(context, activeWorkTask)
        } else {
            FocusMonitorService.stop(context)
        }
    }

    if (activeWorkTask != null) {
        if (reentryOpen) {
            ReentryScreen(
                task = activeWorkTask,
                onResume = { reentryOpen = false },
                onAdjustPlan = {
                    reentryOpen = false
                    activeWorkTaskId = null
                    selectedTaskId = activeWorkTask.id
                },
                onRecordReason = {
                    reentryOpen = false
                    activeWorkTaskId = null
                    selectedTaskId = activeWorkTask.id
                }
            )
        } else if (paused) {
            PauseScreen(onResume = { paused = false })
        } else {
            WorkScreen(
                task = activeWorkTask,
                onPause = {
                    reentryOpen = false
                    paused = true
                },
                onExit = {
                    activeWorkTaskId = null
                    paused = false
                    reentryOpen = false
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
            onTaskSelected = {
                draftTask = null
                selectedTaskId = it
            },
            onAddTask = {
                selectedTaskId = null
                draftTask = PlannedTask(
                    id = nextTaskId,
                    date = selectedDate,
                    title = "",
                    target = "",
                    durationMinutes = 30,
                    priority = Priority.IMPORTANT,
                    status = TaskStatus.PLANNED,
                    planningNote = ""
                )
            },
            onSeedDemo = {
                val seeded = DemoTaskRepository.seed()
                tasks = seeded
                nextTaskId = seeded.nextTaskId()
                selectedTaskId = null
                draftTask = null
            }
        )
    }

    if (detailTask != null) {
        TaskDetailSheet(
            task = detailTask,
            isCreateMode = isCreateMode,
            onDismiss = {
                selectedTaskId = null
                draftTask = null
            },
            onSavePlan = { updated -> tasks = tasks.replaceTask(updated) },
            onCreateTask = { created ->
                tasks = tasks + created
                nextTaskId = maxOf(nextTaskId, created.id + 1)
                selectedTaskId = null
                draftTask = null
                destination = TopLevelDestination.TASKS
            },
            onDeleteTask = { taskId ->
                tasks = tasks.filterNot { it.id == taskId }
                if (activeWorkTaskId == taskId) {
                    activeWorkTaskId = null
                    paused = false
                    reentryOpen = false
                }
                selectedTaskId = null
                draftTask = null
                destination = TopLevelDestination.TASKS
            },
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
    onAddTask: () -> Unit,
    onSeedDemo: () -> Unit
) {
    when (destination) {
        TopLevelDestination.TASKS -> TodayScreen(
            selectedDate = selectedDate,
            tasks = tasks,
            onDateSelected = onDateSelected,
            onTaskSelected = onTaskSelected,
            onAddTask = onAddTask,
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

private fun List<PlannedTask>.nextTaskId(): Long {
    return (maxOfOrNull { it.id } ?: 0L) + 1L
}
