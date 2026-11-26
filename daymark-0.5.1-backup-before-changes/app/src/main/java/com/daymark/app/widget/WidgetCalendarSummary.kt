package com.daymark.app.widget

import com.daymark.app.data.CalendarLogic
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskStatus
import java.time.LocalDate

data class WidgetCalendarSummary(
    val tasksByDate: Map<LocalDate, List<TaskEntity>>,
    val statusByDate: Map<LocalDate, TaskStatus>
)

fun buildWidgetCalendarSummary(tasks: List<TaskEntity>, today: LocalDate): WidgetCalendarSummary {
    val occurrences = CalendarLogic.occurrences(tasks, today)
    val tasksById = tasks.associateBy { it.id }
    val tasksByDate = occurrences.groupBy({ it.date }, { occurrence ->
        tasksById.getValue(occurrence.taskId)
    }).mapValues { (_, rows) -> rows.distinctBy { it.id } }
    val statusByDate = occurrences.groupBy { it.date }.mapValues { (_, rows) ->
        rows.map { it.calculatedStatus }.maxByOrNull(::statusPriority) ?: TaskStatus.UNSCHEDULED
    }
    return WidgetCalendarSummary(tasksByDate, statusByDate)
}

private fun statusPriority(status: TaskStatus): Int = when (status) {
    TaskStatus.OVERDUE, TaskStatus.MISSED_PLAN -> 5
    TaskStatus.DUE_TODAY -> 4
    TaskStatus.PLANNED_TODAY -> 3
    TaskStatus.UPCOMING -> 2
    TaskStatus.COMPLETED -> 1
    TaskStatus.UNSCHEDULED -> 0
}
