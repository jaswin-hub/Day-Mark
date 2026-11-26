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
    suspend fun save(task: TaskEntity) { dao.upsert(task.copy(updatedAt = System.currentTimeMillis())) }
    suspend fun delete(id: String) { dao.deleteById(id) }
    suspend fun complete(id: String, completed: Boolean) { dao.byId(id)?.let { dao.upsert(it.copy(isCompleted = completed, completedAt = if (completed) System.currentTimeMillis() else null, updatedAt = System.currentTimeMillis())) } }
}
