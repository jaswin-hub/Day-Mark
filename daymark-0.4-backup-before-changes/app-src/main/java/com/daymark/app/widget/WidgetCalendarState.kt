package com.daymark.app.widget

import java.time.LocalDate
import java.time.YearMonth

data class WidgetCalendarState(val month: YearMonth, val selectedDate: LocalDate) {
    fun select(date: LocalDate) = WidgetCalendarState(YearMonth.from(date), date)

    fun moveMonth(delta: Long): WidgetCalendarState {
        val next = month.plusMonths(delta)
        val day = selectedDate.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        return WidgetCalendarState(next, next.atDay(day))
    }

    companion object {
        fun fromStored(month: String?, selectedDate: String?, today: LocalDate = LocalDate.now()): WidgetCalendarState {
            val date = selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val displayMonth = month?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(date)
            val safeDate = if (YearMonth.from(date) == displayMonth) date else displayMonth.atDay(date.dayOfMonth.coerceAtMost(displayMonth.lengthOfMonth()))
            return WidgetCalendarState(displayMonth, safeDate)
        }
    }
}
