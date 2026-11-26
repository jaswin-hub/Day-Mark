package com.daymark.app.widget

import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek

enum class WidgetLayout { MINI, WEEKLY, MONTHLY }
enum class WidgetDisplayMode { AUTO, MINI, WEEKLY, MONTHLY }

data class MonthlyWidgetMetrics(
    val monthHeaderHeight: Float,
    val actionHeaderHeight: Float,
    val weekdayHeight: Float,
    val weekRowHeight: Float
)

fun monthlyWidgetMetrics(fontScale: Float): MonthlyWidgetMetrics {
    val scale = fontScale.coerceAtLeast(1f)
    return MonthlyWidgetMetrics(
        monthHeaderHeight = maxOf(34f, 24f * scale + 8f),
        actionHeaderHeight = maxOf(28f, 18f * scale + 8f),
        weekdayHeight = maxOf(20f, 14f * scale + 6f),
        weekRowHeight = maxOf(26f, 19f * scale + 7f)
    )
}

fun widgetLayoutForSize(
    widthDp: Float,
    heightDp: Float,
    displayMode: WidgetDisplayMode = WidgetDisplayMode.AUTO,
    monthWeekCount: Int = 6,
    fontScale: Float = 1f
): WidgetLayout {
    if (widthDp < 220f) return WidgetLayout.MINI
    val scale = fontScale.coerceAtLeast(1f)
    val monthlyMetrics = monthlyWidgetMetrics(fontScale)
    val minimumAgendaHeight = maxOf(22f, 20f * scale + 2f) + 4f + maxOf(42f, 36f * scale + 6f)
    val minimumMonthlyHeight = 16f + monthlyMetrics.monthHeaderHeight + monthlyMetrics.actionHeaderHeight +
        monthlyMetrics.weekdayHeight + monthWeekCount * monthlyMetrics.weekRowHeight + minimumAgendaHeight
    return when (displayMode) {
        WidgetDisplayMode.MINI -> WidgetLayout.MINI
        WidgetDisplayMode.WEEKLY -> if (heightDp < 120f) WidgetLayout.MINI else WidgetLayout.WEEKLY
        WidgetDisplayMode.MONTHLY -> if (heightDp < minimumMonthlyHeight) WidgetLayout.WEEKLY else WidgetLayout.MONTHLY
        WidgetDisplayMode.AUTO -> when {
            heightDp < minimumMonthlyHeight -> WidgetLayout.WEEKLY
            else -> WidgetLayout.MONTHLY
        }
    }
}

fun widgetShowsAgenda(heightDp: Float): Boolean = heightDp >= 315f

fun widgetMonthDates(month: YearMonth, weekStartsOn: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - weekStartsOn.value + 7) % 7
    val rowCount = (offset + month.lengthOfMonth() + 6) / 7
    return (0 until rowCount * 7).map { first.minusDays(offset.toLong()).plusDays(it.toLong()) }
}

fun monthlyAgendaAvailableHeight(widgetHeightDp: Float, calendarWeekCount: Int, fontScale: Float = 1f): Float {
    val metrics = monthlyWidgetMetrics(fontScale)
    return widgetHeightDp - 16f - metrics.monthHeaderHeight - metrics.actionHeaderHeight -
        metrics.weekdayHeight - 4f - calendarWeekCount * metrics.weekRowHeight
}

fun monthlyAgendaTaskLimit(widgetHeightDp: Float, calendarWeekCount: Int, fontScale: Float = 1f): Int {
    val available = monthlyAgendaAvailableHeight(widgetHeightDp, calendarWeekCount, fontScale)
    val scale = fontScale.coerceAtLeast(1f)
    val rowHeight = maxOf(42f, 36f * scale + 6f)
    val headingHeight = maxOf(22f, 20f * scale + 2f)
    return ((available - headingHeight - 4f) / rowHeight).toInt().coerceIn(0, 3)
}

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
