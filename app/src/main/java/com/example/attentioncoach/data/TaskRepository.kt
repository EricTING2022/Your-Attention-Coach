package com.example.attentioncoach.data

import com.example.attentioncoach.domain.PlannedTask
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<PlannedTask>>

    fun observeTasksForDate(date: LocalDate): Flow<List<PlannedTask>>

    suspend fun taskById(taskId: Long): PlannedTask?

    suspend fun createTask(task: PlannedTask): PlannedTask

    suspend fun updateTask(task: PlannedTask)

    suspend fun deleteTask(taskId: Long)

    suspend fun toggleCompletion(taskId: Long)

    suspend fun saveFocusFinish(taskId: Long, actualFocusMinutes: Int)

    suspend fun saveReview(taskId: Long, completion: String, reason: String, adjustment: String)

    suspend fun replaceDemoDay(date: LocalDate, demoTasks: List<PlannedTask>)
}
