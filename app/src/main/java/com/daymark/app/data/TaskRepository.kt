package com.daymark.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val dao: TaskDao) {
    fun observeAll() = dao.observeActive()
    fun observeInbox() = dao.observeInbox()
    fun observePlanned(date: LocalDate) = dao.observePlanned(date)
    fun observeDeadline(date: LocalDate) = dao.observeDeadline(date)
    fun observeCompleted() = dao.observeCompleted()
    fun observeOverdue(today: LocalDate) = dao.observeOverdue(today)
    fun search(query: String): Flow<List<TaskEntity>> = dao.search(query)
    suspend fun get(id: String) = dao.byId(id)
    suspend fun reminders() = dao.reminders()
    suspend fun widgetTasksInRange(start: LocalDate, end: LocalDate) = dao.widgetTasksInRange(start, end)
    suspend fun widgetUpcomingTasks(today: LocalDate, untilDate: LocalDate = today.plusDays(365), limit: Int = 50) = dao.widgetUpcomingTasks(today, untilDate, limit)
    suspend fun widgetOverdueCount(today: LocalDate, now: java.time.LocalTime = java.time.LocalTime.now()) = dao.widgetOverdueCount(today, now)
    fun observeDateChanges(taskId: String) = dao.observeDateChanges(taskId)
    suspend fun save(task: TaskEntity) {
        val existing = dao.byId(task.id)
        if (existing != null && (existing.plannedDate != task.plannedDate || existing.deadlineDate != task.deadlineDate)) {
            dao.reschedule(task.id, task.plannedDate, task.deadlineDate, task.reminderAt, "Dates edited in task editor")
        }
        dao.upsert(task.copy(updatedAt = System.currentTimeMillis()))
    }
    suspend fun delete(id: String) { dao.deleteById(id) }
    suspend fun complete(id: String, completed: Boolean): TaskEntity? {
        val current = dao.byId(id) ?: return null
        return current.copy(isCompleted = completed, completedAt = if (completed) System.currentTimeMillis() else null, updatedAt = System.currentTimeMillis()).also { dao.upsert(it) }
    }
    suspend fun reschedule(task: TaskEntity, date: LocalDate, reason: String) {
        val missedPlan = TaskStatusCalculator.calculate(task) == TaskStatus.MISSED_PLAN
        val nextPlan = task.plannedDate?.let { date } ?: if (missedPlan) date else null
        val overdueDeadline = TaskStatusCalculator.calculate(task) == TaskStatus.OVERDUE
        val nextDeadline = task.deadlineDate?.let { if (overdueDeadline) date else it }
        val oldAnchor = task.deadlineDate ?: task.plannedDate
        val newAnchor = nextDeadline ?: nextPlan
        val nextReminder = if (task.reminderEnabled && oldAnchor != null && newAnchor != null && task.reminderAt != null) {
            val zone = java.time.ZoneId.systemDefault()
            val time = task.deadlineTime ?: java.time.LocalTime.of(9, 0)
            val oldAnchorMillis = oldAnchor.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val leadMinutes = java.time.Duration.between(java.time.Instant.ofEpochMilli(task.reminderAt), java.time.Instant.ofEpochMilli(oldAnchorMillis)).toMinutes().coerceAtLeast(0)
            newAnchor.atTime(time).minusMinutes(leadMinutes).atZone(zone).toInstant().toEpochMilli()
        } else task.reminderAt
        dao.reschedule(task.id, nextPlan, nextDeadline, nextReminder, reason)
    }
    suspend fun archive(id: String) = dao.archive(id, System.currentTimeMillis())
    suspend fun deleteAll() = dao.deleteAll()
}
