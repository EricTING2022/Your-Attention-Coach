package com.example.attentioncoach.domain

import java.time.LocalDate

object DemoTaskRepository {
    fun seed(): List<PlannedTask> {
        val mayFive = LocalDate.of(2026, 5, 5)
        return listOf(
            PlannedTask(
                id = 1L,
                date = mayFive,
                title = "COMP4521 design spec",
                target = "Finish the review section and tighten the UI flow before implementation planning.",
                durationMinutes = 40,
                priority = Priority.IMPORTANT,
                status = TaskStatus.PLANNED,
                planningNote = "Keep the scope demo-friendly. Do not add hard blocking.",
                actualFocusMinutes = 28,
                actualCompletion = "Completed UI flow decisions; implementation order still needs review.",
                mismatchReason = "Attention faded",
                nextAdjustment = "Start with a 30-minute block next time, then review before extending."
            ),
            PlannedTask(
                id = 2L,
                date = mayFive,
                title = "Lecture notes cleanup",
                target = "Sort L5-L8 notes into implementation references.",
                durationMinutes = 30,
                priority = Priority.NOT_URGENT,
                status = TaskStatus.REVIEWED,
                planningNote = "Focus on Room, WorkManager, UsageStats, Notification.",
                actualFocusMinutes = 31,
                actualCompletion = "Finished L5 and L7 references.",
                mismatchReason = "Task too large",
                nextAdjustment = "Split cloud and mobile features into separate tasks."
            ),
            PlannedTask(
                id = 3L,
                date = mayFive,
                title = "Android project skeleton",
                target = "Create the app shell and package structure.",
                durationMinutes = 35,
                priority = Priority.URGENT,
                status = TaskStatus.PAUSED,
                planningNote = "Start only after UI prototype is approved.",
                actualFocusMinutes = 18,
                actualCompletion = "Not started in code.",
                mismatchReason = "Unclear next step",
                nextAdjustment = "Write implementation plan first."
            ),
            PlannedTask(
                id = 4L,
                date = mayFive,
                title = "Final report outline",
                target = "Create report sections aligned with grading rubric.",
                durationMinutes = 25,
                priority = Priority.URGENT_IMPORTANT,
                status = TaskStatus.PLANNED,
                planningNote = "Include test case table early."
            ),
            PlannedTask(
                id = 5L,
                date = LocalDate.of(2026, 5, 6),
                title = "Room schema implementation",
                target = "Implement Task, WorkSession, Review, UsageEvent entities.",
                durationMinutes = 45,
                priority = Priority.IMPORTANT,
                status = TaskStatus.PLANNED,
                planningNote = "Keep schema close to design spec."
            )
        )
    }
}

