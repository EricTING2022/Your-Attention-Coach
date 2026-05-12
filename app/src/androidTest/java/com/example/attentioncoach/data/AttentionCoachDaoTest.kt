package com.example.attentioncoach.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.attentioncoach.data.local.AttentionCoachDatabase
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.Priority
import com.example.attentioncoach.domain.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AttentionCoachDaoTest {
    private lateinit var database: AttentionCoachDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AttentionCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTaskThenObserveAllTasks() = runBlocking {
        val dao = database.dao()
        val id = dao.insertTask(task(id = 0L).toEntities(nowMillis = 100L).task)

        val tasks = dao.observeTasksWithReviews().first()

        assertEquals(listOf(id), tasks.map { it.task.id })
    }

    @Test
    fun upsertReviewOverwritesPreviousReview() = runBlocking {
        val dao = database.dao()
        val id = dao.insertTask(task(id = 0L).toEntities(nowMillis = 100L).task)

        dao.upsertReview(task(id = id, actualFocusMinutes = 10).toEntities(nowMillis = 200L).review!!)
        dao.upsertReview(task(id = id, actualFocusMinutes = 20).toEntities(nowMillis = 300L).review!!)

        val restored = dao.taskWithReviewById(id)!!.toDomain()
        assertEquals(20, restored.actualFocusMinutes)
    }

    @Test
    fun deleteTaskCascadesReview() = runBlocking {
        val dao = database.dao()
        val id = dao.insertTask(task(id = 0L).toEntities(nowMillis = 100L).task)
        dao.upsertReview(task(id = id, actualFocusMinutes = 10).toEntities(nowMillis = 200L).review!!)

        dao.deleteTask(id)

        assertEquals(null, dao.taskWithReviewById(id))
    }

    @Test
    fun deleteDemoTasksForDateKeepsUserTasks() = runBlocking {
        val dao = database.dao()
        val date = LocalDate.of(2026, 5, 5)
        val demo = task(id = 0L, title = "Demo").toEntities(isDemo = true, nowMillis = 100L).task
        val user = task(id = 0L, title = "User").toEntities(isDemo = false, nowMillis = 100L).task
        dao.insertTask(demo)
        dao.insertTask(user)

        dao.deleteDemoTasksForDate(date.toEpochDay())

        assertEquals(listOf("User"), dao.observeTasksWithReviewsForDate(date.toEpochDay()).first().map { it.task.title })
    }

    private fun task(
        id: Long,
        title: String = "Task",
        actualFocusMinutes: Int = 0
    ): PlannedTask {
        return PlannedTask(
            id = id,
            date = LocalDate.of(2026, 5, 5),
            title = title,
            target = "Target",
            durationMinutes = 30,
            priority = Priority.IMPORTANT,
            status = if (actualFocusMinutes > 0) TaskStatus.FINISHED else TaskStatus.PLANNED,
            actualFocusMinutes = actualFocusMinutes
        )
    }
}
