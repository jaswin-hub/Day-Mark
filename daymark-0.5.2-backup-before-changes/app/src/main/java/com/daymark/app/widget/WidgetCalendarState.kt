package com.daymark.app.widget

import java.time.LocalDate
import java.time.YearMonth

enum class WidgetLayout { MINI, WEEKLY, MONTHLY }
enum class WidgetDisplayMode { AUTO, MINI, WEEKLY, MONTHLY }

fun widgetLayoutForSize(widthDp: Float, heightDp: Float, displayMode: WidgetDisplayMode = WidgetDisplayMode.AUTO): WidgetLayout {
    if (widthDp < 220f) return WidgetLayout.MINI
    return when (displayMode) {
        WidgetDisplayMode.MINI -> WidgetLayout.MINI
        WidgetDisplayMode.WEEKLY -> if (heightDp < 120f) WidgetLayout.MINI else WidgetLayout.WEEKLY
        WidgetDisplayMode.MONTHLY -> if (heightDp < 210f) WidgetLayout.WEEKLY else WidgetLayout.MONTHLY
        WidgetDisplayMode.AUTO -> when {
            heightDp < 210f -> WidgetLayout.WEEKLY
            else -> WidgetLayout.MONTHLY
        }
    }
}

fun widgetShowsAgenda(heightDp: Float): Boolean = heightDp >= 315f

data class WidgetCalendarState(val month: YearMonth, val selectedDate: LocalDate) {
    fun select(date: LocalDate) = WidgetCalendarState(YearMonth.from(date), date)

    fun moveMonth(delta: Long): WidgetCalendarState {
        val nextMonth = month.plusMonths(delta)
        val nextDate = nextMonth.atDay(selectedDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
        return WidgetCalendarState(nextMonth, nextDate)
    }

    companion object {
        fun fromStored(month: String?, selectedDate: String?, today: LocalDate = LocalDate.now()): WidgetCalendarState {
            val date = selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return WidgetCalendarState(YearMonth.from(today), today)
            val displayMonth = month?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(date)
            return WidgetCalendarState(displayMonth, date)
        }
    }
}

data class NormalizedWidgetDateState(val state: WidgetCalendarState, val followsToday: Boolean, val lastSeenToday: LocalDate)

fun normalizeWidgetDateState(
    month: String?, selectedDate: String?, followsToday: Boolean?, lastSeenToday: String?, today: LocalDate,
    stateVersion: Int? = null
): NormalizedWidgetDateState {
    if (stateVersion != null && stateVersion !in 1..2) {
        return NormalizedWidgetDateState(WidgetCalendarState(YearMonth.from(today), today), true, today)
    }
    val parsedDate = selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (parsedDate == null) return NormalizedWidgetDateState(WidgetCalendarState(YearMonth.from(today), today), true, today)
    val follow = followsToday ?: false // Existing valid selections are kept as intentional during migration.
    val lastSeen = lastSeenToday?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (follow && lastSeen != today) {
        return NormalizedWidgetDateState(WidgetCalendarState(YearMonth.from(today), today), true, today)
    }
    val displayMonth = month?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(parsedDate)
    return NormalizedWidgetDateState(WidgetCalendarState(displayMonth, parsedDate), follow, today)
}
