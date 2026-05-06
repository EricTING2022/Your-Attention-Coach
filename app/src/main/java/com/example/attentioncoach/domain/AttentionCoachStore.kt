package com.example.attentioncoach.domain

import java.time.LocalDate

class AttentionCoachStore(seedTasks: List<PlannedTask>) {
    private val tasksById = seedTasks.associateBy { it.id }.toMutableMap()

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

    fun startWork(taskId: Long) {
        if (tasksById.containsKey(taskId)) {
            activeWork = ActiveWork(taskId = taskId, isActive = true)
        }
    }

    fun exitWork() {
        val work = activeWork ?: return
        activeWork = work.copy(isActive = false)
    }
}
