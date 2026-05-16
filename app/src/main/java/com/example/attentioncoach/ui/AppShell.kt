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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.attentioncoach.AttentionCoachApplication
import com.example.attentioncoach.R
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.ActiveWork
import com.example.attentioncoach.domain.CalendarRules
import com.example.attentioncoach.domain.DateIndicatorRules
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.ReminderRules
import com.example.attentioncoach.domain.TaskStatus
import com.example.attentioncoach.domain.TopLevelDestination
import com.example.attentioncoach.domain.WorkSessionClock
import com.example.attentioncoach.platform.AccessibilityForegroundHelper
import com.example.attentioncoach.platform.AlarmPermissionHelper
import com.example.attentioncoach.platform.FocusMonitorService
import com.example.attentioncoach.platform.InstalledAppsProvider
import com.example.attentioncoach.platform.ReminderScheduleResult
import com.example.attentioncoach.platform.StartReminderStore
import com.example.attentioncoach.platform.TaskReminderReceiver
import com.example.attentioncoach.platform.TaskReminderScheduler
import com.example.attentioncoach.platform.launchNeededApp
import java.time.LocalDate

@Composable
fun AttentionCoachApp(
    reentryTaskId: Long? = null,
    onReentryConsumed: () -> Unit = {},
    scheduledReminderTaskId: Long? = null,
    onScheduledReminderConsumed: () -> Unit = {},
    appEnteredAtMillis: Long = 0L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = remember(context) {
        (context.applicationContext as AttentionCoachApplication).container
    }
    val viewModel: AttentionCoachViewModel = viewModel(
        factory = AttentionCoachViewModelFactory(container)
    )
    val tasks by viewModel.tasks.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val activeWork by viewModel.activeWork.collectAsState()
    val alarmPermissionHelper = remember(context) { AlarmPermissionHelper(context) }
    val accessibilityForegroundHelper = remember(context) { AccessibilityForegroundHelper(context) }
    var accessibilityDetectionEnabled by remember {
        mutableStateOf(accessibilityForegroundHelper.isForegroundObserverEnabled())
    }
    val reminderScheduler = remember(context) { TaskReminderScheduler(context, alarmPermissionHelper) }
    val startReminderStore = remember(context) { StartReminderStore(context) }
    val installedAppsProvider = remember(context) { InstalledAppsProvider(context) }
    val installedApps = remember(installedAppsProvider) { installedAppsProvider.launchableApps() }
    var destination by remember { mutableStateOf(TopLevelDestination.TASKS) }
    var selectedDate by remember { mutableStateOf(CalendarRules.today()) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var draftTask by remember { mutableStateOf<PlannedTask?>(null) }
    var pendingStartTask by remember { mutableStateOf<PlannedTask?>(null) }
    var showAlarmPermissionPrompt by remember { mutableStateOf(false) }
    var activeDueReminderIds by remember { mutableStateOf(startReminderStore.activeDueIds()) }
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
        if (
            reminderScheduler.schedule(
                task = task,
                repeatIntervalSeconds = appSettings.notificationIntervalSeconds
            ) == ReminderScheduleResult.NEEDS_EXACT_ALARM_PERMISSION
        ) {
            showAlarmPermissionPrompt = true
        }
    }

    fun releaseDeferredStartReminders(sourceTasks: List<PlannedTask>) {
        val releasedTaskIds = startReminderStore.releaseDeferred()
        if (releasedTaskIds.isEmpty()) return
        activeDueReminderIds = startReminderStore.activeDueIds()
        val selectedReminderTask = ReminderRules.highestPriorityDueTask(
            tasks = sourceTasks,
            activeDueIds = activeDueReminderIds
        )
        if (selectedReminderTask != null) {
            TaskReminderReceiver.showReminderNow(
                context = context,
                task = selectedReminderTask,
                repeatIntervalSeconds = appSettings.notificationIntervalSeconds
            )
        }
    }

    DisposableEffect(lifecycleOwner, accessibilityForegroundHelper) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityDetectionEnabled = accessibilityForegroundHelper.isForegroundObserverEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(reentryTaskId) {
        val taskId = reentryTaskId ?: return@LaunchedEffect
        val task = tasks.firstOrNull { it.id == taskId }
        if (task != null) {
            if (activeWork?.taskId != taskId) {
                viewModel.saveActiveWork(task.toActiveWork())
            }
            selectedTaskId = null
            destination = TopLevelDestination.TASKS
        }
        onReentryConsumed()
    }

    LaunchedEffect(scheduledReminderTaskId) {
        val taskId = scheduledReminderTaskId ?: return@LaunchedEffect
        if (tasks.any { it.id == taskId }) {
            TaskReminderReceiver.acknowledgeReminder(context, taskId)
            activeDueReminderIds = startReminderStore.activeDueIds()
            destination = TopLevelDestination.TASKS
            selectedTaskId = taskId
            draftTask = null
        }
        onScheduledReminderConsumed()
    }

    LaunchedEffect(appEnteredAtMillis, tasks) {
        if (appEnteredAtMillis <= 0L) return@LaunchedEffect
        activeDueReminderIds = startReminderStore.activeDueIds()
        activeDueReminderIds.forEach { taskId ->
            TaskReminderReceiver.cancelVisibleNotification(context, taskId)
        }
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
        if (currentWork.isPaused) {
            PauseScreen(
                activeWork = currentWork,
                onResume = { viewModel.saveActiveWork(currentWork.resumeAt(System.currentTimeMillis())) }
            )
        } else {
            WorkScreen(
                task = activeWorkTask,
                activeWork = currentWork,
                onPause = {
                    viewModel.saveActiveWork(currentWork.pauseAt(System.currentTimeMillis()))
                },
                onFinish = {
                    val work = activeWork
                    if (work != null) {
                        val activeMillis = WorkSessionClock.activeMillisAt(work, System.currentTimeMillis())
                        viewModel.saveFocusFinish(
                            taskId = work.taskId,
                            actualFocusMinutes = WorkSessionClock.focusMinutesFromMillis(activeMillis)
                        )
                        viewModel.clearActiveWork()
                        releaseDeferredStartReminders(tasks)
                    }
                    destination = TopLevelDestination.TASKS
                },
                onExit = {
                    viewModel.clearActiveWork()
                    releaseDeferredStartReminders(tasks)
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
            tasks = tasks,
            unfinishedTaskDates = unfinishedTaskDates,
            activeDueReminderIds = activeDueReminderIds,
            onDateSelected = { selectedDate = it },
            onTaskSelected = {
                TaskReminderReceiver.acknowledgeReminder(context, it)
                activeDueReminderIds = startReminderStore.activeDueIds()
                draftTask = null
                selectedTaskId = it
            },
            onToggleTaskComplete = { taskId ->
                viewModel.toggleCompletion(taskId)
            },
            onAddTask = {
                selectedTaskId = null
                draftTask = PlannedTask(
                    id = 0L,
                    date = selectedDate,
                    title = "",
                    target = "",
                    durationMinutes = 30,
                    priority = Priority.IMPORTANT,
                    status = TaskStatus.PLANNED
                )
            },
            settings = appSettings,
            availableApps = installedApps,
            onAddNeededApp = viewModel::addNeededApp,
            onRemoveNeededApp = viewModel::removeNeededApp,
            onNotificationIntervalSelected = viewModel::setNotificationInterval,
            accessibilityDetectionEnabled = accessibilityDetectionEnabled,
            onOpenAccessibilitySettings = accessibilityForegroundHelper::openAccessibilitySettings,
            onSeedDemoDay = {
                viewModel.seedDemoDay { demoDate ->
                    selectedDate = demoDate
                    destination = TopLevelDestination.TASKS
                }
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
                pendingStartTask = updated
                viewModel.updateTask(updated)
                scheduleReminderIfNeeded(updated)
            },
            onCreateTask = { created ->
                viewModel.createTask(created) { saved ->
                    scheduleReminderIfNeeded(saved)
                    selectedTaskId = null
                    draftTask = null
                    destination = TopLevelDestination.TASKS
                }
            },
            onDeleteTask = { taskId ->
                TaskReminderReceiver.acknowledgeReminder(context, taskId)
                activeDueReminderIds = startReminderStore.activeDueIds()
                viewModel.deleteTask(taskId)
                if (activeWork?.taskId == taskId) {
                    viewModel.clearActiveWork()
                }
                selectedTaskId = null
                draftTask = null
                destination = TopLevelDestination.TASKS
            },
            onSaveReview = { taskId, completion, reason, adjustment ->
                viewModel.saveReview(taskId, completion, reason, adjustment)
                selectedTaskId = null
            },
            onStartWork = {
                val task = pendingStartTask?.takeIf { task -> task.id == it }
                    ?: tasks.firstOrNull { task -> task.id == it }
                pendingStartTask = null
                TaskReminderReceiver.acknowledgeReminder(context, it)
                activeDueReminderIds = startReminderStore.activeDueIds()
                if (task != null) {
                    viewModel.saveActiveWork(task.toActiveWork())
                }
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
    activeDueReminderIds: Set<Long>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskSelected: (Long) -> Unit,
    onToggleTaskComplete: (Long) -> Unit,
    onAddTask: () -> Unit,
    settings: AppSettings,
    availableApps: List<NeededApp>,
    onAddNeededApp: (NeededApp) -> Unit,
    onRemoveNeededApp: (String) -> Unit,
    onNotificationIntervalSelected: (Int) -> Unit,
    accessibilityDetectionEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onSeedDemoDay: () -> Unit
) {
    when (destination) {
        TopLevelDestination.TASKS -> TodayScreen(
            selectedDate = selectedDate,
            tasks = tasks.filter { it.date == selectedDate },
            unfinishedTaskDates = unfinishedTaskDates,
            activeDueReminderIds = activeDueReminderIds,
            onDateSelected = onDateSelected,
            onTaskSelected = onTaskSelected,
            onToggleTaskComplete = onToggleTaskComplete,
            onAddTask = onAddTask,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        TopLevelDestination.INSIGHTS -> InsightsScreen(
            tasks = tasks,
            selectedDate = selectedDate,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        TopLevelDestination.SETTINGS -> SettingsScreen(
            settings = settings,
            availableApps = availableApps,
            onAddNeededApp = onAddNeededApp,
            onRemoveNeededApp = onRemoveNeededApp,
            onNotificationIntervalSelected = onNotificationIntervalSelected,
            accessibilityDetectionEnabled = accessibilityDetectionEnabled,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onSeedDemoDay = onSeedDemoDay,
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

private fun PlannedTask.toActiveWork(nowMillis: Long = System.currentTimeMillis()): ActiveWork {
    return ActiveWork(
        taskId = id,
        isActive = true,
        plannedDurationMinutes = durationMinutes,
        startedAtMillis = nowMillis
    )
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
