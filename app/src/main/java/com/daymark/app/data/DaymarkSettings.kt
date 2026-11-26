package com.daymark.app.data

import com.daymark.app.ui.ThemeMode

enum class WeekStart { SUNDAY, MONDAY }
enum class CalendarViewMode { MONTH, WEEK, AGENDA }
enum class DateFormatMode { WEEKDAY_MONTH_DAY, MONTH_DAY_YEAR, DAY_MONTH_YEAR, ISO }

fun java.time.LocalDate.formatForDaymark(mode: DateFormatMode): String = when (mode) {
    DateFormatMode.WEEKDAY_MONTH_DAY -> format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    DateFormatMode.MONTH_DAY_YEAR -> format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
    DateFormatMode.DAY_MONTH_YEAR -> format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    DateFormatMode.ISO -> toString()
}

data class DaymarkSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val weekStart: WeekStart = WeekStart.SUNDAY,
    val showWeekends: Boolean = true,
    val defaultCalendarView: CalendarViewMode = CalendarViewMode.MONTH,
    val use24HourTime: Boolean = false,
    val dateFormat: DateFormatMode = DateFormatMode.WEEKDAY_MONTH_DAY,
    val notificationsEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15
)
