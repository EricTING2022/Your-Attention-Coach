package com.example.attentioncoach.domain

enum class TopLevelDestination(val label: String) {
    TASKS("Tasks"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

enum class AppRoute(val hidesBottomNavigation: Boolean) {
    Today(false),
    Insights(false),
    Settings(false),
    TaskDetail(true),
    Work(true),
    Pause(true),
    Reentry(true)
}

