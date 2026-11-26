package com.daymark.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetCalendarStateTest {
    @Test fun dateSelectionMovesWidgetMonthAndSelectionTogether() {
        val selected = WidgetCalendarState(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 4))
            .select(LocalDate.of(2026, 12, 15))
        assertEquals(YearMonth.of(2026, 12), selected.month)
        assertEquals(LocalDate.of(2026, 12, 15), selected.selectedDate)
    }

    @Test fun nextMonthClampsDayWhenDestinationMonthIsShorter() {
        val state = WidgetCalendarState(YearMonth.of(2024, 1), LocalDate.of(2024, 1, 31)).moveMonth(1)
        assertEquals(YearMonth.of(2024, 2), state.month)
        assertEquals(LocalDate.of(2024, 2, 29), state.selectedDate)
    }

    @Test fun previousMonthHandlesLeapDay() {
        val state = WidgetCalendarState(YearMonth.of(2024, 3), LocalDate.of(2024, 3, 31)).moveMonth(-1)
        assertEquals(LocalDate.of(2024, 2, 29), state.selectedDate)
    }

    @Test fun invalidStoredValuesFallBackToToday() {
        val today = LocalDate.of(2026, 11, 12)
        assertEquals(WidgetCalendarState(YearMonth.from(today), today), WidgetCalendarState.fromStored("bad", "bad", today))
    }
}
