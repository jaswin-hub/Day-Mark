package com.daymark.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.CalendarViewMode
import com.daymark.app.data.DateFormatMode
import com.daymark.app.data.DaymarkSettings
import com.daymark.app.data.WeekStart
import com.daymark.app.ui.LocalDaymarkPalette
import com.daymark.app.ui.ThemeMode

@Composable
fun SettingsScreen(settings: DaymarkSettings, onSettingsChange: ((DaymarkSettings) -> DaymarkSettings) -> Unit, onClearData: () -> Unit) {
    val palette = LocalDaymarkPalette.current
    val context = LocalContext.current
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingsSection("APPEARANCE", "Choose Daymark's palette and motion behavior.") {
                ChoiceRow("Theme", settings.theme.label(), ThemeMode.entries.map { it.label() }) { label ->
                    val choice = ThemeMode.entries.firstOrNull { it.label() == label } ?: settings.theme
                    onSettingsChange { it.copy(theme = choice) }
                }
                SettingSwitch("Reduce motion", "Use brief fades or no transitions when reduced motion is enabled.", settings.reduceMotion) { checked -> onSettingsChange { it.copy(reduceMotion = checked) } }
                SettingSwitch("Haptic feedback", "Small tactile feedback for calendar date selection.", settings.hapticsEnabled) { checked -> onSettingsChange { it.copy(hapticsEnabled = checked) } }
            }
        }
        item {
            SettingsSection("CALENDAR", "Set how dates and weeks are organised.") {
                ChoiceRow("Week starts", settings.weekStart.label(), WeekStart.entries.map { it.label() }) { value -> onSettingsChange { it.copy(weekStart = WeekStart.entries.first { it.label() == value }) } }
                SettingSwitch("Show weekends", "Include Saturday and Sunday in calendar grids.", settings.showWeekends) { checked -> onSettingsChange { it.copy(showWeekends = checked) } }
                ChoiceRow("Default view", settings.defaultCalendarView.label(), CalendarViewMode.entries.map { it.label() }) { value -> onSettingsChange { it.copy(defaultCalendarView = CalendarViewMode.entries.first { it.label() == value }) } }
            }
        }
        item {
            SettingsSection("TIME AND DATE", "Choose how task times and dates appear.") {
                ChoiceRow("Clock", if (settings.use24HourTime) "24-hour" else "12-hour", listOf("12-hour", "24-hour")) { label -> onSettingsChange { it.copy(use24HourTime = label == "24-hour") } }
                ChoiceRow("Date format", settings.dateFormat.label(), DateFormatMode.entries.map { it.label() }) { value -> onSettingsChange { it.copy(dateFormat = DateFormatMode.entries.first { it.label() == value }) } }
            }
        }
        item {
            SettingsSection("NOTIFICATIONS", "Android may still require notification permission.") {
                SettingSwitch("Task reminders", "Allow scheduled reminders for tasks that have reminders enabled.", settings.notificationsEnabled) { checked -> onSettingsChange { it.copy(notificationsEnabled = checked) } }
                ChoiceRow("Default reminder", reminderLabel(settings.defaultReminderMinutes), listOf("At task time", "5 minutes before", "15 minutes before", "30 minutes before", "1 hour before", "1 day before")) { label ->
                    val mins = when (label) { "At task time" -> 0; "5 minutes before" -> 5; "15 minutes before" -> 15; "30 minutes before" -> 30; "1 hour before" -> 60; else -> 1440 }
                    onSettingsChange { it.copy(defaultReminderMinutes = mins) }
                }
            }
        }
        item {
            SettingsSection("WIDGETS", "Add Daymark from your launcher widget picker. Resize a placed widget with the launcher's resize handles.") {
                Text("Mini · a small next-task and deadline snapshot. Weekly · a seven-day strip and selected-day agenda. Monthly · a full date grid that grows with the available space.", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Daymark refreshes widgets after task, theme, reminder and date-selection changes. Resizing is controlled by your launcher.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            SettingsSection("DATA AND ABOUT", "Daymark stores tasks and preferences on this device.") {
                Text("Version $version", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                Text("No account or cloud sync is configured. Your calendar, task details and reminders stay in local app storage.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = onClearData, modifier = Modifier.padding(top = 6.dp)) { Text("Clear all local data", color = palette.missed) }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalDaymarkPalette.current
    Surface(color = palette.surface, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = palette.textMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun ChoiceRow(title: String, selected: String, choices: List<String>, onSelect: (String) -> Unit) {
    val palette = LocalDaymarkPalette.current
    var expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
        Box {
            TextButton(onClick = { expanded.value = true }) { Text(selected, color = palette.accent) }
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }, containerColor = palette.surfaceRaised) {
                choices.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { onSelect(choice); expanded.value = false }) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalDaymarkPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun ThemeMode.label() = when (this) { ThemeMode.DARK -> "Graphite + Radium"; ThemeMode.LIGHT -> "Pastel Violet"; ThemeMode.SYSTEM -> "Follow System" }
private fun WeekStart.label() = when (this) { WeekStart.MONDAY -> "Monday"; WeekStart.SUNDAY -> "Sunday" }
private fun CalendarViewMode.label() = when (this) { CalendarViewMode.MONTH -> "Month"; CalendarViewMode.WEEK -> "Week"; CalendarViewMode.AGENDA -> "Agenda" }
private fun DateFormatMode.label() = when (this) { DateFormatMode.WEEKDAY_MONTH_DAY -> "Wed, Nov 13"; DateFormatMode.MONTH_DAY_YEAR -> "Nov 13, 2026"; DateFormatMode.DAY_MONTH_YEAR -> "13 Nov 2026"; DateFormatMode.ISO -> "2026-11-13" }
private fun reminderLabel(minutes: Int) = when (minutes) { 0 -> "At task time"; 5 -> "5 minutes before"; 15 -> "15 minutes before"; 30 -> "30 minutes before"; 60 -> "1 hour before"; else -> "1 day before" }
