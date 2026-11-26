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
        override suspend fun upsert(task: TaskEntity) { rows.value = rows.value.filterNot { it.id==task.id } + task }
        override suspend fun delete(task: TaskEntity) { deleteById(task.id) }
        override suspend fun deleteById(id: String) { rows.value = rows.value.filterNot { it.id==id } }
        override suspend fun byId(id: String) = rows.value.firstOrNull { it.id==id }
        override fun observeActive(): Flow<List<TaskEntity>> = rows
        override fun observePlanned(date: LocalDate): Flow<List<TaskEntity>> = rows.mapList { it.plannedDate==date && it.archivedAt==null }
        override fun observeDeadline(date: LocalDate): Flow<List<TaskEntity>> = rows.mapList { it.deadlineDate==date && it.archivedAt==null }
        override fun observeInbox(): Flow<List<TaskEntity>> = rows.mapList { !it.isCompleted && it.plannedDate==null && it.deadlineDate==null && it.archivedAt==null }
        override fun observeOverdue(today: LocalDate): Flow<List<TaskEntity>> = rows.mapList { !it.isCompleted && it.deadlineDate?.isBefore(today)==true && it.archivedAt==null }
        override fun observeCompleted(): Flow<List<TaskEntity>> = rows.mapList { it.isCompleted && it.archivedAt==null }
        override fun search(query: String): Flow<List<TaskEntity>> = rows.mapList { it.title.contains(query, ignoreCase=true) && it.archivedAt==null }
        override suspend fun reminders(): List<TaskEntity> = rows.value.filter { it.reminderEnabled && it.reminderAt!=null && !it.isCompleted && it.archivedAt==null }
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
        repo.complete("a",false); assertTrue(repo.observeCompleted().first().isEmpty())
    }

    @Test fun plannedAndDeadlineDateQueriesRemainIndependent() = runBlocking {
        val repo=TaskRepository(FakeDao()); val plan=LocalDate.of(2026,11,13); val due=LocalDate.of(2026,11,16)
        repo.save(TaskEntity("a","Assignment",plannedDate=plan,deadlineDate=due))
        assertEquals(listOf("a"),repo.observePlanned(plan).first().map{it.id})
        assertEquals(listOf("a"),repo.observeDeadline(due).first().map{it.id})
        assertTrue(repo.observeDeadline(plan).first().isEmpty())
    }
}
