package com.daymark.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskStatusCalculatorTest {
    private val today = LocalDate.of(2026, 11, 12)
    @Test fun upcomingAndPlannedToday() {
        assertEquals(TaskStatus.UPCOMING, TaskStatusCalculator.calculate(TaskEntity("1", "x", plannedDate=today.plusDays(1)), today))
        assertEquals(TaskStatus.PLANNED_TODAY, TaskStatusCalculator.calculate(TaskEntity("2", "x", plannedDate=today), today))
    }
    @Test fun dueAndOverdueAreIndependentOfPlan() {
        assertEquals(TaskStatus.DUE_TODAY, TaskStatusCalculator.calculate(TaskEntity("1", "x", plannedDate=today.minusDays(1), deadlineDate=today), today))
        assertEquals(TaskStatus.MISSED_PLAN, TaskStatusCalculator.calculate(TaskEntity("2", "x", plannedDate=today.minusDays(1), deadlineDate=today.plusDays(3)), today))
        assertEquals(TaskStatus.OVERDUE, TaskStatusCalculator.calculate(TaskEntity("3", "x", deadlineDate=today.minusDays(1)), today))
    }
    @Test fun completionAndReopen() {
        assertEquals(TaskStatus.COMPLETED, TaskStatusCalculator.calculate(TaskEntity("1", "x", deadlineDate=today.minusDays(1), isCompleted=true), today))
        assertEquals(TaskStatus.OVERDUE, TaskStatusCalculator.calculate(TaskEntity("1", "x", deadlineDate=today.minusDays(1), isCompleted=false), today))
    }
    @Test fun unscheduledAndDeadlineTime() {
        assertEquals(TaskStatus.UNSCHEDULED, TaskStatusCalculator.calculate(TaskEntity("1", "x"), today))
        assertEquals(TaskStatus.OVERDUE, TaskStatusCalculator.calculate(TaskEntity("2", "x", deadlineDate=today, deadlineTime=java.time.LocalTime.NOON), today, java.time.LocalTime.of(13,0)))
    }
}
