package com.example.attentioncoach.data

import androidx.room.withTransaction
import com.example.attentioncoach.data.local.AttentionCoachDatabase
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val database: AttentionCoachDatabase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : TaskRepository {
    private val dao = database.dao()

    override fun observeTasks(): Flow<List<PlannedTask>> {
        return dao.observeTasksWithReviews().map { tasks -> tasks.map { it.toDomain() } }
    }

    override fun observeTasksForDate(date: LocalDate): Flow<List<PlannedTask>> {
        return dao.observeTasksWithReviewsForDate(date.toEpochDay()).map { tasks -> tasks.map { it.toDomain() } }
    }

    override suspend fun taskById(taskId: Long): PlannedTask? {
        return dao.taskWithReviewById(taskId)?.toDomain()
    }

    override suspend fun createTask(task: PlannedTask): PlannedTask {
        val now = nowMillis()
        val entities = task.copy(id = 0L).toEntities(nowMillis = now)
        val taskId = dao.insertTask(entities.task)
        entities.review?.let { dao.upsertReview(it.copy(taskId = taskId)) }
        return task.copy(id = taskId)
    }

    override suspend fun updateTask(task: PlannedTask) {
        val existing = dao.taskWithReviewById(task.id) ?: return
        writeExistingTask(
            task = task,
            isDemo = existing.task.isDemo,
            createdAtMillis = existing.task.createdAtMillis
        )
    }

    override suspend fun deleteTask(taskId: Long) {
        dao.deleteTask(taskId)
    }

    override suspend fun toggleCompletion(taskId: Long) {
        val existing = taskById(taskId) ?: return
        val updated = if (existing.status == TaskStatus.FINISHED || existing.status == TaskStatus.REVIEWED) {
            existing.copy(
                status = TaskStatus.PLANNED,
                actualFocusMinutes = 0,
                actualCompletion = "",
                mismatchReason = "",
                nextAdjustment = ""
            )
        } else {
            existing.copy(
                status = TaskStatus.FINISHED,
                actualFocusMinutes = 0
            )
        }
        updateTask(updated)
    }

    override suspend fun saveFocusFinish(taskId: Long, actualFocusMinutes: Int) {
        val existing = taskById(taskId) ?: return
        updateTask(
            existing.copy(
                status = TaskStatus.FINISHED,
                actualFocusMinutes = actualFocusMinutes
            )
        )
    }

    override suspend fun saveReview(
        taskId: Long,
        completion: String,
        reason: String,
        adjustment: String
    ) {
        val existing = taskById(taskId) ?: return
        updateTask(
            existing.copy(
                status = TaskStatus.REVIEWED,
                actualCompletion = completion,
                mismatchReason = reason,
                nextAdjustment = adjustment
            )
        )
    }

    override suspend fun replaceDemoDay(date: LocalDate, demoTasks: List<PlannedTask>) {
        val now = nowMillis()
        database.withTransaction {
            dao.deleteDemoTasksForDate(date.toEpochDay())
            demoTasks
                .filter { it.date == date }
                .forEach { demoTask ->
                    val entities = demoTask.copy(id = 0L).toEntities(isDemo = true, nowMillis = now)
                    val taskId = dao.insertTask(entities.task)
                    entities.review?.let { dao.upsertReview(it.copy(taskId = taskId)) }
                }
        }
    }

    private suspend fun writeExistingTask(
        task: PlannedTask,
        isDemo: Boolean,
        createdAtMillis: Long
    ) {
        val now = nowMillis()
        val entities = task.toEntities(isDemo = isDemo, nowMillis = now)
        dao.updateTask(entities.task.copy(createdAtMillis = createdAtMillis))
        if (entities.review == null) {
            dao.deleteReview(task.id)
        } else {
            dao.upsertReview(entities.review)
        }
    }
}
