package com.example.attentioncoach.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.attentioncoach.R
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.AppSettingsRules
import com.example.attentioncoach.domain.ActiveWork
import com.example.attentioncoach.domain.CalendarRules
import com.example.attentioncoach.domain.DateIndicatorRules
import com.example.attentioncoach.domain.DemoTaskRepository
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.TopLevelDestination
import com.example.attentioncoach.domain.WorkSessionClock
import com.example.attentioncoach.platform.AlarmPermissionHelper
import com.example.attentioncoach.platform.FocusMonitorService
import com.example.attentioncoach.platform.ReminderScheduleResult
import com.example.attentioncoach.platform.TaskReminderScheduler
import com.example.attentioncoach.platform.launchNeededApp
import java.time.LocalDate

@Composable
fun AttentionCoachApp(
    reentryTaskId: Long? = null,
    onReentryConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val initialTasks = remember { DemoTaskRepository.seed() }
    val alarmPermissionHelper = remember(context) { AlarmPermissionHelper(context) }
    val reminderScheduler = remember(context) { TaskReminderScheduler(context, alarmPermissionHelper) }
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }
    var selectedDate by remember { mutableStateOf(CalendarRules.today()) }
    var tasks by remember { mutableStateOf(initialTasks) }
    var nextTaskId by remember { mutableStateOf(initialTasks.nextTaskId()) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var draftTask by remember { mutableStateOf<PlannedTask?>(null) }
    var activeWork by remember { mutableStateOf<ActiveWork?>(null) }
    var appSettings by remember { mutableStateOf(AppSettings()) }
    var reentryOpen by remember { mutableStateOf(false) }
    var showAlarmPermissionPrompt by remember { mutableStateOf(false) }
    val selectedTask = selectedTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    val detailTask = selectedTask ?: draftTask
    val isCreateMode = selectedTask == null && draftTask != null
    val activeWorkTask = activeWork?.taskId?.let { id -> tasks.firstOrNull { it.id == id } }
    val unfinishedTaskDates = remember(tasks) {
        tasks.groupBy { it.date }
            .filterValues(DateIndicatorRules::hasUnfinishedTaskDot)
            .keys
    }

    fun scheduleReminderIfNeeded(task: PlannedTask) {
        if (reminderScheduler.schedule(task) == ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION) {
            showAlarmPermissionPrompt = true
        }
    }

    LaunchedEffect(reentryTaskId) {
        val taskId = reentryTaskId ?: return@LaunchedEffect
        val task = tasks.firstOrNull { it.id == taskId }
        if (task != null) {
            activeWork = activeWork?.takeIf { it.taskId == taskId } ?: task.toActiveWork()
            selectedTaskId = null
            destination = TopLevelDestination.TASKS
            reentryOpen = true
        }
        onReentryConsumed()
    }

    LaunchedEffect(activeWorkTask?.id, activeWork?.isPaused) {
        if (activeWorkTask != null && activeWork?.isPaused == false) {
            FocusMonitorService.start(
                context = context,
                task = activeWorkTask,
                neededPackages = appSettings.neededApps.map { it.packageName }.toSet(),
                reentryCooldownMillis = appSettings.notificationIntervalSeconds * 1000L
            )
        } else {
            FocusMonitorService.stop(context)
        }
    }

    if (showAlarmPermissionPrompt) {
        AlarmPermissionPrompt(
            onDismiss = { showAlarmPermissionPrompt = false },
            onOpenSettings = {
                showAlarmPermissionPrompt = false
                alarmPermissionHelper.requestExactAlarmAccess()
            }
        )
    }

    val currentWork = activeWork
    if (activeWorkTask != null && currentWork != null) {
        if (reentryOpen) {
            ReentryScreen(
                task = activeWorkTask,
                onResume = { reentryOpen = false },
                onAdjustPlan = {
                    reentryOpen = false
                    activeWork = null
                    selectedTaskId = activeWorkTask.id
                },
                onRecordReason = {
                    reentryOpen = false
                    activeWork = null
                    selectedTaskId = activeWorkTask.id
                }
            )
        } else if (currentWork.isPaused) {
            PauseScreen(
                activeWork = currentWork,
                onResume = { activeWork = activeWork?.resumeAt(System.currentTimeMillis()) }
            )
        } else {
            WorkScreen(
                task = activeWorkTask,
                activeWork = currentWork,
                onPause = {
                    reentryOpen = false
                    activeWork = activeWork?.pauseAt(System.currentTimeMillis())
                },
                onFinish = {
                    val work = activeWork
                    if (work != null) {
                        val activeMillis = WorkSessionClock.activeMillisAt(work, System.currentTimeMillis())
                        tasks = tasks.map {
                            if (it.id == work.taskId) {
                                it.copy(
                                    status = TaskStatus.FINISHED,
                                    actualFocusMinutes = WorkSessionClock.focusMinutesFromMillis(activeMillis)
                                )
                            } else {
                                it
                            }
                        }
                    }
                    activeWork = null
                    reentryOpen = false
                    destination = TopLevelDestination.TASKS
                },
                onExit = {
                    activeWork = null
                    reentryOpen = false
                    destination = TopLevelDestination.TASKS
                },
                neededApps = appSettings.neededApps,
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
            unfinishedTaskDates = unfinishedTaskDates,
            onDateSelected = { selectedDate = it },
            onTaskSelected = {
                draftTask = null
                selectedTaskId = it
            },
            onToggleTaskComplete = { taskId ->
                tasks = tasks.map { task ->
                    if (task.id == taskId) task.toggledCompletion() else task
                }
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
                    status = TaskStatus.PLANNED
                )
            },
            settings = appSettings,
            onAddNeededApp = { appSettings = AppSettingsRules.addNeededApp(appSettings, it) },
            onRemoveNeededApp = { appSettings = AppSettingsRules.removeNeededApp(appSettings, it) },
            onNotificationIntervalSelected = {
                appSettings = AppSettingsRules.withNotificationInterval(appSettings, it)
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
            onSavePlan = { updated ->
                tasks = tasks.replaceTask(updated)
                scheduleReminderIfNeeded(updated)
            },
            onCreateTask = { created ->
                tasks = tasks + created
                scheduleReminderIfNeeded(created)
                nextTaskId = maxOf(nextTaskId, created.id + 1)
                selectedTaskId = null
                draftTask = null
                destination = TopLevelDestination.TASKS
            },
            onDeleteTask = { taskId ->
                tasks = tasks.filterNot { it.id == taskId }
                if (activeWork?.taskId == taskId) {
                    activeWork = null
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
                val task = tasks.firstOrNull { task -> task.id == it }
                activeWork = task?.toActiveWork()
                selectedTaskId = null
            }
        )
    }
}

@Composable
private fun AlarmPermissionPrompt(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow reminder alarms?") },
        text = { Text("Task start-time reminders need exact alarm access. You can keep the task saved and enable access in Android settings.") },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}

@Composable
private fun AttentionBottomBar(
    selected: TopLevelDestination,
    onSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes()),
                        contentDescription = null
                    )
                },
                label = { Text(text = destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = UiTokens.GoogleBlue,
                    selectedTextColor = UiTokens.GoogleBlue,
                    indicatorColor = UiTokens.GoogleBlue.copy(alpha = 0.14f),
                    unselectedIconColor = UiTokens.InkSoft,
                    unselectedTextColor = UiTokens.InkSoft
                )
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
    unfinishedTaskDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskSelected: (Long) -> Unit,
    onToggleTaskComplete: (Long) -> Unit,
    onAddTask: () -> Unit,
    settings: AppSettings,
    onAddNeededApp: (NeededApp) -> Unit,
    onRemoveNeededApp: (String) -> Unit,
    onNotificationIntervalSelected: (Int) -> Unit
) {
    when (destination) {
        TopLevelDestination.TASKS -> TodayScreen(
            selectedDate = selectedDate,
            tasks = tasks,
            unfinishedTaskDates = unfinishedTaskDates,
            onDateSelected = onDateSelected,
            onTaskSelected = onTaskSelected,
            onToggleTaskComplete = onToggleTaskComplete,
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
            settings = settings,
            onAddNeededApp = onAddNeededApp,
            onRemoveNeededApp = onRemoveNeededApp,
            onNotificationIntervalSelected = onNotificationIntervalSelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

private fun TopLevelDestination.iconRes(): Int {
    return when (this) {
        TopLevelDestination.TASKS -> R.drawable.ic_task_alt_24
        TopLevelDestination.INSIGHTS -> R.drawable.ic_insights_24
        TopLevelDestination.SETTINGS -> R.drawable.ic_settings_24
    }
}

private fun List<PlannedTask>.replaceTask(updated: PlannedTask): List<PlannedTask> {
    return map { if (it.id == updated.id) updated else it }
}

private fun List<PlannedTask>.nextTaskId(): Long {
    return (maxOfOrNull { it.id } ?: 0L) + 1L
}

private fun PlannedTask.toActiveWork(nowMillis: Long = System.currentTimeMillis()): ActiveWork {
    return ActiveWork(
        taskId = id,
        isActive = true,
        plannedDurationMinutes = durationMinutes,
        startedAtMillis = nowMillis
    )
}

private fun PlannedTask.toggledCompletion(): PlannedTask {
    return if (status == TaskStatus.FINISHED || status == TaskStatus.REVIEWED) {
        copy(
            status = TaskStatus.PLANNED,
            actualFocusMinutes = 0,
            actualCompletion = "",
            mismatchReason = "",
            nextAdjustment = ""
        )
    } else {
        copy(
            status = TaskStatus.FINISHED,
            actualFocusMinutes = 0
        )
    }
}

private fun ActiveWork.pauseAt(nowMillis: Long): ActiveWork {
    if (isPaused) return this
    return copy(
        accumulatedActiveMillis = WorkSessionClock.activeMillisAt(this, nowMillis),
        pauseStartedAtMillis = nowMillis,
        isPaused = true
    )
}

private fun ActiveWork.resumeAt(nowMillis: Long): ActiveWork {
    if (!isPaused) return this
    return copy(
        startedAtMillis = nowMillis,
        pauseStartedAtMillis = null,
        isPaused = false
    )
}
