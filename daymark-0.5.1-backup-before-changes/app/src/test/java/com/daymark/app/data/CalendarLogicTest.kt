package com.daymark.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {
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
}
