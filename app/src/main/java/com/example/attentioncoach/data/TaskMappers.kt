package com.example.attentioncoach.data

import com.example.attentioncoach.data.local.TaskEntity
import com.example.attentioncoach.data.local.TaskReviewEntity
import com.example.attentioncoach.data.local.TaskWithReview
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.TaskStatus
import java.time.LocalDate
import java.time.LocalTime

data class TaskEntities(
    val task: TaskEntity,
    val review: TaskReviewEntity?
)

fun PlannedTask.toEntities(isDemo: Boolean = false, nowMillis: Long): TaskEntities {
    return TaskEntities(
        task = TaskEntity(
            id = id,
            dateEpochDay = date.toEpochDay(),
            title = title,
            target = target,
            startMinuteOfDay = startTime.toMinuteOfDay(),
            durationMinutes = durationMinutes,
            priorityName = priority.name,
            statusName = status.name,
            isDemo = isDemo,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        ),
        review = toReviewEntity(nowMillis)
    )
}

fun TaskWithReview.toDomain(): PlannedTask {
    return PlannedTask(
        id = task.id,
        date = LocalDate.ofEpochDay(task.dateEpochDay),
        title = task.title,
        target = task.target,
        startTime = task.startMinuteOfDay.toLocalTime(),
        durationMinutes = task.durationMinutes,
        priority = Priority.valueOf(task.priorityName),
        status = TaskStatus.valueOf(task.statusName),
        actualFocusMinutes = review?.actualFocusMinutes ?: 0,
        actualCompletion = review?.actualCompletion.orEmpty(),
        mismatchReason = review?.mismatchReason.orEmpty(),
        nextAdjustment = review?.nextAdjustment.orEmpty()
    )
}

internal fun PlannedTask.toReviewEntity(nowMillis: Long): TaskReviewEntity? {
    val hasReviewData = actualFocusMinutes > 0 ||
        actualCompletion.isNotBlank() ||
        mismatchReason.isNotBlank() ||
        nextAdjustment.isNotBlank() ||
        status == TaskStatus.REVIEWED
    if (!hasReviewData) return null
    return TaskReviewEntity(
        taskId = id,
        actualFocusMinutes = actualFocusMinutes,
        actualCompletion = actualCompletion,
        mismatchReason = mismatchReason,
        nextAdjustment = nextAdjustment,
        updatedAtMillis = nowMillis
    )
}

private fun LocalTime?.toMinuteOfDay(): Int? = this?.let { it.hour * 60 + it.minute }

private fun Int?.toLocalTime(): LocalTime? = this?.let { LocalTime.of(it / 60, it % 60) }
