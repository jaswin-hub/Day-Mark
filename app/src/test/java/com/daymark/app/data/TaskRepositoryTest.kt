package com.daymark.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TaskRepositoryTest {
    private class FakeDao : TaskDao {
        private val rows = MutableStateFlow<List<TaskEntity>>(emptyList())
        private val dateChanges = MutableStateFlow<List<TaskDateChange>>(emptyList())
        override suspend fun upsert(task: TaskEntity) { rows.value = rows.value.filterNot { it.id==task.id } + task }
        override suspend fun delete(task: TaskEntity) { deleteById(task.id) }
        override suspend fun deleteTaskById(id: String) { rows.value = rows.value.filterNot { it.id==id } }
        override suspend fun deleteDateChanges(id: String) { dateChanges.value = dateChanges.value.filterNot { it.taskId == id } }
        override suspend fun deleteAllTasks() { rows.value = emptyList() }
        override suspend fun deleteAllDateChanges() { dateChanges.value = emptyList() }
        override suspend fun byId(id: String) = rows.value.firstOrNull { it.id==id }
        override fun observeActive(): Flow<List<TaskEntity>> = rows
        override fun observePlanned(date: LocalDate): Flow<List<TaskEntity>> = rows.mapList { it.plannedDate==date && it.archivedAt==null }
        override fun observeDeadline(date: LocalDate): Flow<List<TaskEntity>> = rows.mapList { it.deadlineDate==date && it.archivedAt==null }
        override fun observeInbox(): Flow<List<TaskEntity>> = rows.mapList { !it.isCompleted && it.plannedDate==null && it.deadlineDate==null && it.archivedAt==null }
        override fun observeOverdue(today: LocalDate): Flow<List<TaskEntity>> = rows.mapList { !it.isCompleted && it.deadlineDate?.isBefore(today)==true && it.archivedAt==null }
        override fun observeCompleted(): Flow<List<TaskEntity>> = rows.mapList { it.isCompleted && it.archivedAt==null }
        override fun search(query: String): Flow<List<TaskEntity>> = rows.mapList { it.title.contains(query, ignoreCase=true) && it.archivedAt==null }
        override suspend fun reminders(): List<TaskEntity> = rows.value.filter { it.reminderEnabled && it.reminderAt!=null && !it.isCompleted && it.archivedAt==null }
        override suspend fun widgetTasksInRange(start: LocalDate, end: LocalDate) = rows.value.filter { (it.plannedDate?.let { d -> d in start..end } == true) || (it.deadlineDate?.let { d -> d in start..end } == true) }
        override suspend fun widgetUpcomingTasks(today: LocalDate, untilDate: LocalDate, limit: Int) = rows.value.filter { !it.isCompleted && it.archivedAt == null && ((it.plannedDate?.let { d -> d in today..untilDate } == true) || (it.deadlineDate?.let { d -> d in today..untilDate } == true)) }.take(limit)
        override suspend fun widgetOverdueCount(today: LocalDate, now: java.time.LocalTime) = rows.value.count { !it.isCompleted && it.deadlineDate?.isBefore(today) == true }
        override suspend fun insertDateChange(change: TaskDateChange) { dateChanges.value = dateChanges.value + change }
        override fun observeDateChanges(taskId: String): Flow<List<TaskDateChange>> = dateChanges.map { it.filter { row -> row.taskId == taskId } }
        override suspend fun archive(id: String, at: Long) { rows.value = rows.value.map { if (it.id == id) it.copy(archivedAt = at) else it } }
        private fun Flow<List<TaskEntity>>.mapList(predicate: (TaskEntity)->Boolean) = map { list -> list.filter(predicate) }
    }

    @Test fun createEditDeleteAndGet() = runBlocking {
        val repo=TaskRepository(FakeDao()); val item=TaskEntity("a","Write report")
        repo.save(item); assertEquals("Write report",repo.get("a")?.title)
        repo.save(item.copy(title="Finish report")); assertEquals("Finish report",repo.get("a")?.title)
        repo.delete("a"); assertNull(repo.get("a"))
    }

    @Test fun completionReopenAndInboxQueries() = runBlocking {
        val repo=TaskRepository(FakeDao()); repo.save(TaskEntity("a","Undated")); repo.save(TaskEntity("b","Due",deadlineDate=LocalDate.of(2026,11,15)))
        assertEquals(setOf("a"),repo.observeInbox().first().map{it.id}.toSet())
        repo.complete("a",true); assertEquals(setOf("a"),repo.observeCompleted().first().map{it.id}.toSet())
        repo.complete("a",true); assertEquals(setOf("a"),repo.observeCompleted().first().map{it.id}.toSet())
        repo.complete("a",false); assertTrue(repo.observeCompleted().first().isEmpty())
    }

    @Test fun plannedAndDeadlineDateQueriesRemainIndependent() = runBlocking {
        val repo=TaskRepository(FakeDao()); val plan=LocalDate.of(2026,11,13); val due=LocalDate.of(2026,11,16)
        repo.save(TaskEntity("a","Assignment",plannedDate=plan,deadlineDate=due))
        assertEquals(listOf("a"),repo.observePlanned(plan).first().map{it.id})
        assertEquals(listOf("a"),repo.observeDeadline(due).first().map{it.id})
        assertTrue(repo.observeDeadline(plan).first().isEmpty())
    }

    @Test fun reschedulePreservesPreviousPlannedAndDeadlineDates() = runBlocking {
        val repo = TaskRepository(FakeDao())
        val plan = LocalDate.of(2026, 11, 13)
        val deadline = LocalDate.of(2026, 11, 18)
        val task = TaskEntity("history", "Report", plannedDate = plan, deadlineDate = deadline)
        repo.save(task)
        val nextPlan = LocalDate.of(2026, 11, 14)
        repo.save(task.copy(plannedDate = nextPlan))
        val history = repo.observeDateChanges("history").first().single()
        assertEquals(plan, history.priorPlannedDate)
        assertEquals(deadline, history.priorDeadlineDate)
        assertEquals(nextPlan, history.newPlannedDate)
        assertEquals(deadline, repo.get("history")?.deadlineDate)
    }
}
