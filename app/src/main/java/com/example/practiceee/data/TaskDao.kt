package com.example.practiceee.data


import androidx.room.*

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert
    suspend fun insertTasks(tasks: Collection<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks") // Replace 'tasks' with your actual table name
    suspend fun deleteAllTasks()
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("SELECT COUNT(*) FROM tasks WHERE createdAt >= :since")
    suspend fun countCreatedSince(since: Long): Int

    // Count all completed since some timestamp
    @Query("""
    SELECT COUNT(*) FROM tasks
    WHERE isCompleted = 1
      AND completedAt IS NOT NULL
      AND completedAt >= :since
  """)
    suspend fun countCompletedSince(since: Long): Int
}





