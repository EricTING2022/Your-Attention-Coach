package com.example.attentioncoach.domain

import java.time.LocalDate
import java.time.LocalTime

class AttentionCoachStore(seedTasks: List<PlannedTask>) {
    private val tasksById = seedTasks.associateBy { it.id }.toMutableMap()
    private var nextTaskId = (seedTasks.maxOfOrNull { it.id } ?: 0L) + 1L

    var activeWork: ActiveWork? = null
        private set

    fun tasksForDate(date: LocalDate): List<PlannedTask> {
        return tasksById.values
            .filter { it.date == date }
            .sortedBy { it.id }
    }

    fun taskById(taskId: Long): PlannedTask? {
        return tasksById[taskId]
    }

    fun createTask(
        date: LocalDate,
        title: String,
        target: String = "",
        startTime: LocalTime? = null,
        durationMinutes: Int = 30,
        priority: Priority = Priority.IMPORTANT
    ): PlannedTask {
        val task = PlannedTask(
            id = nextTaskId++,
            date = date,
            title = title,
            target = target,
            startTime = startTime,
            durationMinutes = durationMinutes,
            priority = priority,
            status = TaskStatus.PLANNED
        )
        tasksById[task.id] = task
        return task
    }

    fun deleteTask(taskId: Long) {
        tasksById.remove(taskId)
        if (activeWork?.taskId == taskId) {
            activeWork = activeWork?.copy(isActive = false)
        }
    }

    fun toggleTaskCompletion(taskId: Long) {
        val task = tasksById[taskId] ?: return
        tasksById[taskId] = if (task.status == TaskStatus.FINISHED || task.status == TaskStatus.REVIEWED) {
            task.copy(
                status = TaskStatus.PLANNED,
                actualFocusMinutes = 0,
                actualCompletion = "",
                mismatchReason = "",
                nextAdjustment = ""
            )
        } else {
            task.copy(
                status = TaskStatus.FINISHED,
                actualFocusMinutes = 0
            )
        }
    }

    fun saveReview(
        taskId: Long,
        actualCompletion: String,
        mismatchReason: String,
        nextAdjustment: String
    ) {
        val task = tasksById[taskId] ?: return
        tasksById[taskId] = task.copy(
            status = TaskStatus.REVIEWED,
            actualCompletion = actualCompletion,
            mismatchReason = mismatchReason,
            nextAdjustment = nextAdjustment
        )
    }

    fun updatePlan(
        taskId: Long,
        target: String,
        startTime: LocalTime?,
        durationMinutes: Int,
        priority: Priority
    ) {
        val task = tasksById[taskId] ?: return
        tasksById[taskId] = task.copy(
            target = target,
            startTime = startTime,
            durationMinutes = durationMinutes,
            priority = priority
        )
    }

    fun startWork(taskId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val task = tasksById[taskId] ?: return
        activeWork = ActiveWork(
            taskId = taskId,
            isActive = true,
            plannedDurationMinutes = task.durationMinutes,
            startedAtMillis = nowMillis
        )
    }

    fun pauseWork(nowMillis: Long = System.currentTimeMillis()) {
        val work = activeWork ?: return
        if (!work.isActive || work.isPaused) return
        activeWork = work.copy(
            accumulatedActiveMillis = work.accumulatedActiveMillis + (nowMillis - work.startedAtMillis).coerceAtLeast(0L),
            pauseStartedAtMillis = nowMillis,
            isPaused = true
        )
    }

    fun resumeWork(nowMillis: Long = System.currentTimeMillis()) {
        val work = activeWork ?: return
        if (!work.isActive || !work.isPaused) return
        activeWork = work.copy(
            startedAtMillis = nowMillis,
            pauseStartedAtMillis = null,
            isPaused = false
        )
    }

    fun finishWork(nowMillis: Long = System.currentTimeMillis()) {
        val work = activeWork ?: return
        val task = tasksById[work.taskId] ?: return
        val activeMillis = WorkSessionClock.activeMillisAt(work, nowMillis)
        tasksById[work.taskId] = task.copy(
            status = TaskStatus.FINISHED,
            actualFocusMinutes = WorkSessionClock.focusMinutesFromMillis(activeMillis)
        )
        activeWork = work.copy(isActive = false)
    }

    fun exitWork() {
        val work = activeWork ?: return
        activeWork = work.copy(isActive = false)
    }

}
