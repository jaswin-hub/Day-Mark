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
    @Query("DELETE FROM tasks WHERE id=:id") suspend fun deleteTaskById(id: String)
    @Query("DELETE FROM task_date_changes WHERE taskId=:id") suspend fun deleteDateChanges(id: String)
    @Transaction suspend fun deleteById(id: String) { deleteDateChanges(id); deleteTaskById(id) }
    @Query("SELECT * FROM tasks WHERE id=:id") suspend fun byId(id: String): TaskEntity?
    @Query("SELECT * FROM tasks WHERE archivedAt IS NULL ORDER BY isCompleted, COALESCE(deadlineDate, plannedDate), CASE priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC") fun observeActive(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE plannedDate=:date AND archivedAt IS NULL ORDER BY isCompleted, priority DESC") fun observePlanned(date: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE deadlineDate=:date AND archivedAt IS NULL ORDER BY isCompleted, priority DESC") fun observeDeadline(date: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=0 AND plannedDate IS NULL AND deadlineDate IS NULL AND archivedAt IS NULL ORDER BY createdAt DESC") fun observeInbox(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=0 AND deadlineDate < :today AND archivedAt IS NULL ORDER BY deadlineDate") fun observeOverdue(today: LocalDate): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted=1 AND archivedAt IS NULL ORDER BY completedAt DESC") fun observeCompleted(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' AND archivedAt IS NULL ORDER BY updatedAt DESC") fun search(query: String): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE reminderEnabled=1 AND reminderAt IS NOT NULL AND isCompleted=0 AND archivedAt IS NULL") suspend fun reminders(): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE archivedAt IS NULL AND (((plannedDate BETWEEN :start AND :end) OR (deadlineDate BETWEEN :start AND :end)) OR ((plannedDate BETWEEN :today AND :untilDate) OR (deadlineDate BETWEEN :today AND :untilDate))) ORDER BY isCompleted, COALESCE(deadlineDate, plannedDate), priority DESC LIMIT :limit")
    suspend fun widgetTasksInRange(start: LocalDate, end: LocalDate, today: LocalDate, untilDate: LocalDate, limit: Int): List<TaskEntity>
    @Query("SELECT COUNT(*) FROM tasks WHERE archivedAt IS NULL AND isCompleted=0 AND (deadlineDate < :today OR (deadlineDate = :today AND deadlineTime < :now))")
    suspend fun widgetOverdueCount(today: LocalDate, now: java.time.LocalTime): Int
    @Insert suspend fun insertDateChange(change: TaskDateChange)
    @Query("SELECT * FROM task_date_changes WHERE taskId=:taskId ORDER BY changedAt DESC") fun observeDateChanges(taskId: String): Flow<List<TaskDateChange>>
    @Query("UPDATE tasks SET archivedAt=:at, updatedAt=:at WHERE id=:id") suspend fun archive(id: String, at: Long)
    @Query("DELETE FROM tasks") suspend fun deleteAllTasks()
    @Query("DELETE FROM task_date_changes") suspend fun deleteAllDateChanges()
    @Transaction suspend fun deleteAll() { deleteAllDateChanges(); deleteAllTasks() }

    @Transaction
    suspend fun reschedule(taskId: String, planned: LocalDate?, deadline: LocalDate?, reminderAt: Long?, reason: String) {
        val current = byId(taskId) ?: return
        if (current.plannedDate == planned && current.deadlineDate == deadline) return
        insertDateChange(TaskDateChange(
            taskId = taskId,
            priorPlannedDate = current.plannedDate,
            priorDeadlineDate = current.deadlineDate,
            newPlannedDate = planned,
            newDeadlineDate = deadline,
            reason = reason
        ))
        upsert(current.copy(plannedDate = planned, deadlineDate = deadline, reminderAt = reminderAt, updatedAt = System.currentTimeMillis()))
    }
}

@Database(entities = [TaskEntity::class, TaskDateChange::class], version = 2, exportSchema = true)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS task_date_changes (id TEXT NOT NULL, taskId TEXT NOT NULL, priorPlannedDate TEXT, priorDeadlineDate TEXT, newPlannedDate TEXT, newDeadlineDate TEXT, changedAt INTEGER NOT NULL, reason TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_date_changes_taskId ON task_date_changes(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_date_changes_changedAt ON task_date_changes(changedAt)")
            }
        }
    }
}
