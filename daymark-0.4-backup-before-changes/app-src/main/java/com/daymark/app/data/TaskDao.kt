package com.daymark.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DateConverters {
    @TypeConverter fun fromDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun fromTime(value: java.time.LocalTime?): String? = value?.toString()
    @TypeConverter fun toTime(value: String?): java.time.LocalTime? = value?.let(java.time.LocalTime::parse)
    @TypeConverter fun fromPriority(value: TaskPriority): String = value.name
    @TypeConverter fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}

@Dao interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id=:id") suspend fun deleteById(id: String)
    @Query("SELECT * FROM tasks WHERE id=:id") suspend fun byId(id: String): TaskEntity?
    @Query("SELECT * FROM tasks WHERE archivedAt IS NULL ORDER BY isCompleted, COALESCE(deadlineDate, plannedDate), CASE priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC") fun observeActive(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE plannedDate=:date AND archivedAt IS NULL ORDER BY isCompleted, priority DESC") fun observePlanned(date: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE deadlineDate=:date AND archivedAt IS NULL ORDER BY isCompleted, priority DESC") fun observeDeadline(date: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=0 AND plannedDate IS NULL AND deadlineDate IS NULL AND archivedAt IS NULL ORDER BY createdAt DESC") fun observeInbox(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=0 AND deadlineDate < :today AND archivedAt IS NULL ORDER BY deadlineDate") fun observeOverdue(today: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=1 AND archivedAt IS NULL ORDER BY completedAt DESC") fun observeCompleted(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' AND archivedAt IS NULL ORDER BY updatedAt DESC") fun search(query: String): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE reminderEnabled=1 AND reminderAt IS NOT NULL AND isCompleted=0 AND archivedAt IS NULL") suspend fun reminders(): List<TaskEntity>
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = true)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() { abstract fun taskDao(): TaskDao }
