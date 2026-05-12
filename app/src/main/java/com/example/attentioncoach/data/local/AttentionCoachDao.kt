package com.example.attentioncoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AttentionCoachDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY dateEpochDay ASC, createdAtMillis ASC")
    fun observeTasksWithReviews(): Flow<List<TaskWithReview>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtMillis ASC")
    fun observeTasksWithReviewsForDate(dateEpochDay: Long): Flow<List<TaskWithReview>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun taskWithReviewById(taskId: Long): TaskWithReview?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Upsert
    suspend fun upsertReview(review: TaskReviewEntity)

    @Query("DELETE FROM task_reviews WHERE taskId = :taskId")
    suspend fun deleteReview(taskId: Long)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("DELETE FROM tasks WHERE isDemo = 1 AND dateEpochDay = :dateEpochDay")
    suspend fun deleteDemoTasksForDate(dateEpochDay: Long)
}
