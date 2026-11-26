package com.daymark.app.widget

import com.daymark.app.data.CalendarLogic
import com.daymark.app.data.CalendarDaySummary
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskStatus
import java.time.LocalDate

data class WidgetCalendarSummary(
    val days: Map<LocalDate, CalendarDaySummary>,
    val tasksByDate: Map<LocalDate, List<TaskEntity>>,
    val statusByDate: Map<LocalDate, TaskStatus>
)

fun buildWidgetCalendarSummary(tasks: List<TaskEntity>, today: LocalDate): WidgetCalendarSummary {
    val tasksById = tasks.associateBy { it.id }
    val days = CalendarLogic.daySummaries(tasks, today)
    val tasksByDate = days.mapValues { (_, summary) ->
        summary.occurrences.mapNotNull { tasksById[it.taskId] }.distinctBy { it.id }
    }
    val statusByDate = days.mapNotNull { (date, summary) -> summary.dominantStatus?.let { date to it } }.toMap()
    return WidgetCalendarSummary(days, tasksByDate, statusByDate)
}
