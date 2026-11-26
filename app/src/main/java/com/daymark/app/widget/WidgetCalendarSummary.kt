package com.daymark.app.widget

import com.daymark.app.data.CalendarLogic
import com.daymark.app.data.CalendarDaySummary
import com.daymark.app.data.CalendarStatusTone
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskStatus
import com.daymark.app.data.calendarStatusTone
import java.time.LocalDate

data class WidgetCalendarSummary(
    val days: Map<LocalDate, CalendarDaySummary>,
    val tasksByDate: Map<LocalDate, List<TaskEntity>>,
    val statusByDate: Map<LocalDate, TaskStatus>
)

data class WidgetDateCellState(
    val status: TaskStatus?,
    val tone: CalendarStatusTone,
    val selected: Boolean,
    val today: Boolean
)

fun widgetDateCellState(date: LocalDate, selectedDate: LocalDate, today: LocalDate, summary: CalendarDaySummary?): WidgetDateCellState {
    val status = summary?.dominantStatus
    return WidgetDateCellState(status, status?.calendarStatusTone() ?: CalendarStatusTone.NEUTRAL, date == selectedDate, date == today)
}

fun buildWidgetCalendarSummary(tasks: List<TaskEntity>, today: LocalDate): WidgetCalendarSummary {
    val tasksById = tasks.associateBy { it.id }
    val days = CalendarLogic.daySummaries(tasks, today)
    val tasksByDate = days.mapValues { (_, summary) ->
        summary.occurrences.mapNotNull { tasksById[it.taskId] }.distinctBy { it.id }
    }
    val statusByDate = days.mapNotNull { (date, summary) -> summary.dominantStatus?.let { date to it } }.toMap()
    return WidgetCalendarSummary(days, tasksByDate, statusByDate)
}
