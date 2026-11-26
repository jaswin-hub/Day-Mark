package com.daymark.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.LocalTime

class CalendarLogicTest {
    private val today = LocalDate.of(2026, 11, 13)
    private val noon = LocalTime.NOON

    @Test fun monthGridStartsSundayAndIncludesNormalRows() {
        val days=CalendarLogic.monthDates(YearMonth.of(2026,11))
        assertEquals(DayOfWeek.SUNDAY,days.first().dayOfWeek)
        assertTrue(days.size==35||days.size==42)
        assertEquals(30,days.count{it.month==java.time.Month.NOVEMBER})
    }
    @Test fun februaryLeapYearAndSixWeekMonth() {
        assertEquals(29,CalendarLogic.monthDates(YearMonth.of(2024,2)).count{it.month==java.time.Month.FEBRUARY})
        assertEquals(42,CalendarLogic.monthDates(YearMonth.of(2026,8)).size)
    }
    @Test fun plansAndDeadlinesCreateSeparateOccurrences() {
        val task=TaskEntity("a","assignment",plannedDate=LocalDate.of(2026,11,13),deadlineDate=LocalDate.of(2026,11,16))
        val occurrences=CalendarLogic.occurrences(listOf(task),LocalDate.of(2026,11,14))
        assertEquals(2,occurrences.size)
        assertEquals(TaskStatus.MISSED_PLAN,occurrences.first{it.occurrenceType==OccurrenceType.PLANNED}.calculatedStatus)
        assertEquals(TaskStatus.UPCOMING,occurrences.first{it.occurrenceType==OccurrenceType.DEADLINE}.calculatedStatus)
    }

    @Test fun daySummaryAggregatesEachOccurrenceAndDominantStatus() {
        val date = today.plusDays(2)
        val tasks = listOf(
            TaskEntity("upcoming", "Upcoming", plannedDate = date),
            TaskEntity("complete", "Complete", deadlineDate = date, isCompleted = true)
        )
        val summary = CalendarLogic.daySummaries(tasks, today, noon).getValue(date)
        assertEquals(2, summary.totalOccurrences)
        assertEquals(1, summary.completedOccurrences)
        assertEquals(1, summary.upcomingOccurrences)
        assertEquals(TaskStatus.UPCOMING, summary.dominantStatus)
        assertTrue(summary.hasMixedStatuses)
    }

    @Test fun missedPlanDoesNotMakeFutureDeadlineOverdue() {
        val task = TaskEntity("late-plan", "Still due later", plannedDate = today.minusDays(1), deadlineDate = today.plusDays(3))
        val summaries = CalendarLogic.daySummaries(listOf(task), today, noon)
        assertEquals(TaskStatus.MISSED_PLAN, summaries.getValue(today.minusDays(1)).dominantStatus)
        assertEquals(1, summaries.getValue(today.minusDays(1)).missedPlans)
        assertEquals(0, summaries.getValue(today.minusDays(1)).overdueDeadlines)
        assertEquals(TaskStatus.UPCOMING, summaries.getValue(today.plusDays(3)).dominantStatus)
    }

    @Test fun overdueDeadlineAndDeadlineTodayHaveCorrectStatuses() {
        val overdue = TaskEntity("overdue", "Overdue", deadlineDate = today.minusDays(1))
        val dueNow = TaskEntity("due", "Due now", deadlineDate = today, deadlineTime = noon.minusMinutes(1))
        val summaries = CalendarLogic.daySummaries(listOf(overdue, dueNow), today, noon)
        assertEquals(TaskStatus.OVERDUE, summaries.getValue(today.minusDays(1)).dominantStatus)
        assertEquals(1, summaries.getValue(today.minusDays(1)).overdueDeadlines)
        assertEquals(TaskStatus.OVERDUE, summaries.getValue(today).dominantStatus)
        assertEquals(1, summaries.getValue(today).deadlinesToday)
    }

    @Test fun deadlineTodayIsAmberAndCompletedOverridesOverdue() {
        val due = TaskEntity("due", "Due today", deadlineDate = today, deadlineTime = noon.plusHours(1))
        val done = TaskEntity("done", "Done late", deadlineDate = today.minusDays(1), isCompleted = true)
        val summaries = CalendarLogic.daySummaries(listOf(due, done), today, noon)
        assertEquals(TaskStatus.DUE_TODAY, summaries.getValue(today).dominantStatus)
        assertEquals(TaskStatus.COMPLETED, summaries.getValue(today.minusDays(1)).dominantStatus)
        assertEquals(CalendarStatusTone.DEADLINE_TODAY, summaries.getValue(today).dominantStatus?.calendarStatusTone())
    }

    @Test fun urgentUnfinishedStatusOutranksCompletedAndLessUrgentStatuses() {
        val date = today.minusDays(1)
        val completed = TaskEntity("done", "Done", plannedDate = date, isCompleted = true)
        val overdue = TaskEntity("late", "Late", deadlineDate = date)
        val summary = CalendarLogic.daySummaries(listOf(completed, overdue), today, noon).getValue(date)
        assertEquals(TaskStatus.OVERDUE, summary.dominantStatus)
        assertTrue(summary.hasMixedStatuses)
    }

    @Test fun sameTaskWithSamePlanAndDeadlineProducesTwoOccurrences() {
        val task = TaskEntity("same-day", "Plan and due", plannedDate = today, deadlineDate = today)
        val summary = CalendarLogic.daySummaries(listOf(task), today, noon).getValue(today)
        assertEquals(2, summary.totalOccurrences)
        assertEquals(setOf(OccurrenceType.PLANNED, OccurrenceType.DEADLINE), summary.occurrences.map { it.occurrenceType }.toSet())
        assertEquals(1, summary.upcomingOccurrences + summary.deadlinesToday)
        assertTrue(summary.hasMixedStatuses)
    }

    @Test fun allCompletedOccurrencesAreGreenAndNoOccurrencesAreNeutral() {
        val date = today.plusDays(1)
        val done = TaskEntity("done", "Done", plannedDate = date, deadlineDate = date, isCompleted = true)
        val summary = CalendarLogic.daySummaries(listOf(done), today, noon).getValue(date)
        assertEquals(TaskStatus.COMPLETED, summary.dominantStatus)
        assertEquals(CalendarStatusTone.COMPLETED, summary.dominantStatus?.calendarStatusTone())
        assertTrue(CalendarLogic.daySummaries(emptyList(), today, noon).isEmpty())
    }
}
