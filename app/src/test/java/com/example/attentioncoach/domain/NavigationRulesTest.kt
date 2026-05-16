package com.example.attentioncoach.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRulesTest {
    @Test
    fun topLevelDestinationsMatchFrozenBottomNavigationOrder() {
        assertEquals(
            listOf("Tasks", "Insights", "Settings"),
            TopLevelDestination.entries.map { it.label }
        )
    }

    @Test
    fun workAndDetailRoutesHideBottomNavigation() {
        assertTrue(AppRoute.TaskDetail.hidesBottomNavigation)
        assertTrue(AppRoute.Work.hidesBottomNavigation)
        assertTrue(AppRoute.Pause.hidesBottomNavigation)
    }

    @Test
    fun reentryRouteIsRemovedBecauseNotificationsReturnDirectlyToFocus() {
        assertEquals(
            listOf("Today", "Insights", "Settings", "TaskDetail", "Work", "Pause"),
            AppRoute.entries.map { it.name }
        )
    }
}
