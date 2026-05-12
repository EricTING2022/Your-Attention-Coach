package com.example.attentioncoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attentioncoach.AppContainer
import com.example.attentioncoach.data.SettingsRepository
import com.example.attentioncoach.data.TaskRepository
import com.example.attentioncoach.domain.ActiveWork
import com.example.attentioncoach.domain.AppSettings
import com.example.attentioncoach.domain.DemoTaskRepository
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.platform.FocusSessionStore
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttentionCoachViewModel(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val focusSessionStore: FocusSessionStore
) : ViewModel() {
    private var lastSeedDemoAtMillis: Long = 0L

    val tasks: StateFlow<List<PlannedTask>> = taskRepository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val activeWork: StateFlow<ActiveWork?> = focusSessionStore.activeWork
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createTask(task: PlannedTask, onCreated: (PlannedTask) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(taskRepository.createTask(task))
        }
    }

    fun updateTask(task: PlannedTask) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun toggleCompletion(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleCompletion(taskId)
        }
    }

    fun saveFocusFinish(taskId: Long, actualFocusMinutes: Int) {
        viewModelScope.launch {
            taskRepository.saveFocusFinish(taskId, actualFocusMinutes)
        }
    }

    fun saveReview(taskId: Long, completion: String, reason: String, adjustment: String) {
        viewModelScope.launch {
            taskRepository.saveReview(taskId, completion, reason, adjustment)
        }
    }

    fun saveActiveWork(work: ActiveWork) {
        focusSessionStore.setActive(work)
    }

    fun clearActiveWork() {
        focusSessionStore.clearActive()
    }

    fun addNeededApp(app: NeededApp) {
        viewModelScope.launch {
            settingsRepository.addNeededApp(app)
        }
    }

    fun removeNeededApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.removeNeededApp(packageName)
        }
    }

    fun setNotificationInterval(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationInterval(seconds)
        }
    }

    fun seedDemoDay(onSeeded: (LocalDate) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastSeedDemoAtMillis < SEED_DEMO_COOLDOWN_MILLIS) {
            onSeeded(DemoTaskRepository.demoDate)
            return
        }
        lastSeedDemoAtMillis = now
        viewModelScope.launch {
            taskRepository.replaceDemoDay(
                date = DemoTaskRepository.demoDate,
                demoTasks = DemoTaskRepository.demoDayTasks()
            )
            onSeeded(DemoTaskRepository.demoDate)
        }
    }

    private companion object {
        const val SEED_DEMO_COOLDOWN_MILLIS = 1_500L
    }
}

class AttentionCoachViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttentionCoachViewModel::class.java)) {
            return AttentionCoachViewModel(
                taskRepository = container.taskRepository,
                settingsRepository = container.settingsRepository,
                focusSessionStore = container.focusSessionStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
