package com.daymark.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }
enum class TaskStatus { UPCOMING, PLANNED_TODAY, DUE_TODAY, MISSED_PLAN, OVERDUE, COMPLETED, UNSCHEDULED }
enum class OccurrenceType { PLANNED, DEADLINE }

@Entity(tableName = "tasks", indices = [Index("plannedDate"), Index("deadlineDate"), Index("isCompleted"), Index("reminderAt")])
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val plannedDate: LocalDate? = null,
    val deadlineDate: LocalDate? = null,
    val deadlineTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedDurationMinutes: Int? = null,
    val category: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val reminderAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val archivedAt: Long? = null
)

object TaskStatusCalculator {
    fun calculate(task: TaskEntity, today: LocalDate = LocalDate.now(), now: LocalTime = LocalTime.now()): TaskStatus {
        if (task.isCompleted) return TaskStatus.COMPLETED
        val due = task.deadlineDate
        if (due != null && (due < today || (due == today && task.deadlineTime?.let { it < now } == true))) return TaskStatus.OVERDUE
        if (due == today) return TaskStatus.DUE_TODAY
        val planned = task.plannedDate
        if (planned != null && planned < today) return TaskStatus.MISSED_PLAN
        if (planned == today) return TaskStatus.PLANNED_TODAY
        if (planned != null || due != null) return TaskStatus.UPCOMING
        return TaskStatus.UNSCHEDULED
    }
}

data class TaskCalendarOccurrence(val taskId: String, val date: LocalDate, val occurrenceType: OccurrenceType, val calculatedStatus: TaskStatus)
data class DayCalendarSummary(val date: LocalDate, val occurrences: List<TaskCalendarOccurrence>)

object CalendarLogic {
    fun occurrences(tasks: List<TaskEntity>, today: LocalDate = LocalDate.now()): List<TaskCalendarOccurrence> = tasks.flatMap { task ->
        fun occurrenceState(date: LocalDate, type: OccurrenceType): TaskStatus {
            if (task.isCompleted) return TaskStatus.COMPLETED
            if (task.plannedDate == task.deadlineDate && task.plannedDate == date && task.deadlineDate == today) return TaskStatus.DUE_TODAY
            return when (type) {
                OccurrenceType.PLANNED -> when { date < today -> TaskStatus.MISSED_PLAN; date == today -> TaskStatus.PLANNED_TODAY; else -> TaskStatus.UPCOMING }
                OccurrenceType.DEADLINE -> when { date < today -> TaskStatus.OVERDUE; date == today && task.deadlineTime?.let { it < LocalTime.now() } == true -> TaskStatus.OVERDUE; date == today -> TaskStatus.DUE_TODAY; else -> TaskStatus.UPCOMING }
            }
        }
        buildList {
            task.plannedDate?.let { add(TaskCalendarOccurrence(task.id, it, OccurrenceType.PLANNED, occurrenceState(it, if (task.deadlineDate == it) OccurrenceType.DEADLINE else OccurrenceType.PLANNED))) }
            task.deadlineDate?.takeIf { it != task.plannedDate }?.let { add(TaskCalendarOccurrence(task.id, it, OccurrenceType.DEADLINE, occurrenceState(it, OccurrenceType.DEADLINE))) }
        }
    }
    fun monthDates(month: java.time.YearMonth, weekStartsOn: java.time.DayOfWeek = java.time.DayOfWeek.SUNDAY): List<LocalDate> {
        val first = month.atDay(1)
        val shift = (first.dayOfWeek.value % 7 - weekStartsOn.value % 7 + 7) % 7
        val start = first.minusDays(shift.toLong())
        val count = if (shift + month.lengthOfMonth() <= 35) 35 else 42
        return (0 until count).map { start.plusDays(it.toLong()) }
    }
}
