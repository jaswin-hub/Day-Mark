package com.daymark.app.widget

import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WidgetCalendarSummaryTest {
    private val today = LocalDate.of(2026, 11, 13)

    @Test fun summaryKeepsPlannedDatesAndDeadlinesIndependent() {
        val task = TaskEntity("task", "Assignment", plannedDate = today, deadlineDate = today.plusDays(7))
        val summary = buildWidgetCalendarSummary(listOf(task), today)
        assertEquals(listOf(task), summary.tasksByDate[today])
        assertEquals(listOf(task), summary.tasksByDate[today.plusDays(7)])
        assertEquals(TaskStatus.PLANNED_TODAY, summary.statusByDate[today])
        assertEquals(TaskStatus.UPCOMING, summary.statusByDate[today.plusDays(7)])
    }

    @Test fun summaryPreservesCompletedAndOverdueStatusMarkers() {
        val overdue = TaskEntity("overdue", "Late", deadlineDate = today.minusDays(1))
        val complete = TaskEntity("done", "Done", plannedDate = today, isCompleted = true)
        val summary = buildWidgetCalendarSummary(listOf(overdue, complete), today)
        assertEquals(TaskStatus.OVERDUE, summary.statusByDate[today.minusDays(1)])
        assertEquals(TaskStatus.COMPLETED, summary.statusByDate[today])
    }
}
