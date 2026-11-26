package com.daymark.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetCalendarStateTest {
    @Test fun responsiveBreakpointsSelectMiniWeeklyAndMonthlyLayouts() {
        assertEquals(WidgetLayout.MINI, widgetLayoutForSize(120f, 120f))
        assertEquals(WidgetLayout.WEEKLY, widgetLayoutForSize(260f, 120f))
        assertEquals(WidgetLayout.MONTHLY, widgetLayoutForSize(260f, 260f))
    }

    @Test fun displayModePreferenceHonorsAvailableSize() {
        assertEquals(WidgetLayout.WEEKLY, widgetLayoutForSize(280f, 210f, WidgetDisplayMode.WEEKLY))
        assertEquals(WidgetLayout.WEEKLY, widgetLayoutForSize(280f, 185f, WidgetDisplayMode.MONTHLY))
        assertEquals(WidgetLayout.MINI, widgetLayoutForSize(200f, 320f, WidgetDisplayMode.MONTHLY))
    }
    @Test fun dateSelectionMovesWidgetMonthAndSelectionTogether() {
        val selected = WidgetCalendarState(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 4))
            .select(LocalDate.of(2026, 12, 15))
        assertEquals(YearMonth.of(2026, 12), selected.month)
        assertEquals(LocalDate.of(2026, 12, 15), selected.selectedDate)
    }

    @Test fun nextMonthChangesDisplayedMonthWithoutReplacingSelectedDate() {
        val state = WidgetCalendarState(YearMonth.of(2024, 1), LocalDate.of(2024, 1, 31)).moveMonth(1)
        assertEquals(YearMonth.of(2024, 2), state.month)
        assertEquals(LocalDate.of(2024, 1, 31), state.selectedDate)
    }

    @Test fun previousMonthNavigationKeepsSelectionIndependent() {
        val state = WidgetCalendarState(YearMonth.of(2024, 3), LocalDate.of(2024, 3, 31)).moveMonth(-1)
        assertEquals(YearMonth.of(2024, 2), state.month)
        assertEquals(LocalDate.of(2024, 3, 31), state.selectedDate)
    }

    @Test fun invalidStoredValuesFallBackToToday() {
        val today = LocalDate.of(2026, 11, 12)
        assertEquals(WidgetCalendarState(YearMonth.from(today), today), WidgetCalendarState.fromStored("bad", "bad", today))
    }

    @Test fun selectedDateSurvivesStateRecreation() {
        val selected = LocalDate.of(2026, 12, 24)
        assertEquals(
            WidgetCalendarState(YearMonth.from(selected), selected),
            WidgetCalendarState.fromStored("2026-12", selected.toString())
        )
    }

    @Test fun widgetInstancesKeepIndependentSelections() {
        val first = WidgetCalendarState.fromStored("2026-11", "2026-11-10")
        val second = WidgetCalendarState.fromStored("2026-11", "2026-11-22")
        assertEquals(LocalDate.of(2026, 11, 10), first.selectedDate)
        assertEquals(LocalDate.of(2026, 11, 22), second.selectedDate)
        assertEquals(LocalDate.of(2026, 11, 10), first.select(LocalDate.of(2026, 11, 10)).selectedDate)
    }

    @Test fun agendaOnlyAppearsWhenWidgetHasEnoughHeight() {
        assertEquals(false, widgetShowsAgenda(210f))
        assertEquals(false, widgetShowsAgenda(300f))
        assertEquals(true, widgetShowsAgenda(320f))
    }

    @Test fun newWidgetFollowsCurrentDateAndNormalizesInvalidState() {
        val today = LocalDate.of(2026, 11, 13)
        val state = normalizeWidgetDateState("bad", "bad", null, null, today)
        assertEquals(WidgetCalendarState(YearMonth.from(today), today), state.state)
        assertEquals(true, state.followsToday)
    }

    @Test fun legacyValidSelectionIsPreservedAsIntentional() {
        val today = LocalDate.of(2026, 11, 13)
        val historical = LocalDate.of(2026, 9, 23)
        val state = normalizeWidgetDateState("2026-09", historical.toString(), null, null, today)
        assertEquals(historical, state.state.selectedDate)
        assertEquals(YearMonth.of(2026, 9), state.state.month)
        assertEquals(false, state.followsToday)
    }

    @Test fun unsupportedStateFormatRecoversToCurrentDate() {
        val today = LocalDate.of(2026, 11, 13)
        val recovered = normalizeWidgetDateState("2026-09", "2026-09-23", false, "2026-09-23", today, stateVersion = 99)
        assertEquals(WidgetCalendarState(YearMonth.from(today), today), recovered.state)
        assertEquals(true, recovered.followsToday)
    }

    @Test fun followTodayAdvancesAcrossDeviceDateChangeButExplicitSelectionDoesNot() {
        val old = LocalDate.of(2026, 11, 13)
        val next = old.plusDays(1)
        val following = normalizeWidgetDateState(old.toString().substring(0, 7), old.toString(), true, old.toString(), next)
        assertEquals(WidgetCalendarState(YearMonth.from(next), next), following.state)
        val explicit = normalizeWidgetDateState("2026-09", "2026-09-23", false, old.toString(), next)
        assertEquals(LocalDate.of(2026, 9, 23), explicit.state.selectedDate)
    }
}
