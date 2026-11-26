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
enum class CalendarStatusTone(val argb: Int?) {
    UPCOMING(0xFF3478D4.toInt()),
    MISSED(0xFFD94B4B.toInt()),
    DEADLINE_TODAY(0xFFD9A044.toInt()),
    COMPLETED(0xFF3D936F.toInt()),
    NEUTRAL(null)
}

fun TaskStatus.calendarStatusTone(): CalendarStatusTone = when (this) {
    TaskStatus.UPCOMING, TaskStatus.PLANNED_TODAY -> CalendarStatusTone.UPCOMING
    TaskStatus.MISSED_PLAN, TaskStatus.OVERDUE -> CalendarStatusTone.MISSED
    TaskStatus.DUE_TODAY -> CalendarStatusTone.DEADLINE_TODAY
    TaskStatus.COMPLETED -> CalendarStatusTone.COMPLETED
    TaskStatus.UNSCHEDULED -> CalendarStatusTone.NEUTRAL
}

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

@Entity(tableName = "task_date_changes", indices = [Index("taskId"), Index("changedAt")])
data class TaskDateChange(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val priorPlannedDate: LocalDate?,
    val priorDeadlineDate: LocalDate?,
    val newPlannedDate: LocalDate?,
    val newDeadlineDate: LocalDate?,
    val changedAt: Long = System.currentTimeMillis(),
    val reason: String
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

    fun occurrenceStatus(task: TaskEntity, type: OccurrenceType, date: LocalDate, today: LocalDate, now: LocalTime): TaskStatus {
        if (task.isCompleted) return TaskStatus.COMPLETED
        return when (type) {
            OccurrenceType.PLANNED -> when {
                date < today -> TaskStatus.MISSED_PLAN
                date == today -> TaskStatus.PLANNED_TODAY
                else -> TaskStatus.UPCOMING
            }
            OccurrenceType.DEADLINE -> when {
                date < today || (date == today && task.deadlineTime?.let { it < now } == true) -> TaskStatus.OVERDUE
                date == today -> TaskStatus.DUE_TODAY
                else -> TaskStatus.UPCOMING
            }
        }
    }
}

data class TaskCalendarOccurrence(val taskId: String, val date: LocalDate, val occurrenceType: OccurrenceType, val calculatedStatus: TaskStatus)
data class CalendarDaySummary(
    val date: LocalDate,
    val occurrences: List<TaskCalendarOccurrence>,
    val totalOccurrences: Int,
    val completedOccurrences: Int,
    val upcomingOccurrences: Int,
    val missedPlans: Int,
    val overdueDeadlines: Int,
    val deadlinesToday: Int,
    val dominantStatus: TaskStatus?,
    val hasMixedStatuses: Boolean
)

object CalendarLogic {
    fun occurrences(tasks: List<TaskEntity>, today: LocalDate = LocalDate.now(), now: LocalTime = LocalTime.now()): List<TaskCalendarOccurrence> = tasks.flatMap { task ->
        buildList {
            task.plannedDate?.let { date ->
                add(TaskCalendarOccurrence(task.id, date, OccurrenceType.PLANNED, TaskStatusCalculator.occurrenceStatus(task, OccurrenceType.PLANNED, date, today, now)))
            }
            task.deadlineDate?.let { date ->
                add(TaskCalendarOccurrence(task.id, date, OccurrenceType.DEADLINE, TaskStatusCalculator.occurrenceStatus(task, OccurrenceType.DEADLINE, date, today, now)))
            }
        }
    }

    fun daySummaries(tasks: List<TaskEntity>, today: LocalDate = LocalDate.now(), now: LocalTime = LocalTime.now()): Map<LocalDate, CalendarDaySummary> =
        occurrences(tasks, today, now).groupBy { it.date }.mapValues { (date, occurrences) ->
            val statuses = occurrences.map { it.calculatedStatus }.distinct()
            CalendarDaySummary(
                date = date,
                occurrences = occurrences,
                totalOccurrences = occurrences.size,
                completedOccurrences = occurrences.count { it.calculatedStatus == TaskStatus.COMPLETED },
                upcomingOccurrences = occurrences.count { it.calculatedStatus == TaskStatus.UPCOMING },
                missedPlans = occurrences.count { it.occurrenceType == OccurrenceType.PLANNED && it.calculatedStatus == TaskStatus.MISSED_PLAN },
                overdueDeadlines = occurrences.count { it.occurrenceType == OccurrenceType.DEADLINE && it.calculatedStatus == TaskStatus.OVERDUE },
                deadlinesToday = occurrences.count { it.occurrenceType == OccurrenceType.DEADLINE && it.date == today },
                dominantStatus = statuses.maxByOrNull(::statusSeverity),
                hasMixedStatuses = statuses.size > 1
            )
        }

    private fun statusSeverity(status: TaskStatus): Int = when (status) {
        TaskStatus.OVERDUE, TaskStatus.MISSED_PLAN -> 5
        TaskStatus.DUE_TODAY -> 4
        TaskStatus.UPCOMING, TaskStatus.PLANNED_TODAY -> 3
        TaskStatus.COMPLETED -> 2
        TaskStatus.UNSCHEDULED -> 0
    }
    fun monthDates(month: java.time.YearMonth, weekStartsOn: java.time.DayOfWeek = java.time.DayOfWeek.SUNDAY): List<LocalDate> {
        val first = month.atDay(1)
        val shift = (first.dayOfWeek.value % 7 - weekStartsOn.value % 7 + 7) % 7
        val start = first.minusDays(shift.toLong())
        val count = if (shift + month.lengthOfMonth() <= 35) 35 else 42
        return (0 until count).map { start.plusDays(it.toLong()) }
    }
}
