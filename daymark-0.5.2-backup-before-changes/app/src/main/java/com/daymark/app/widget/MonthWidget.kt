package com.daymark.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.appwidget.AppWidgetManager
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.daymark.app.BuildConfig
import com.daymark.app.DaymarkApp
import com.daymark.app.MainActivity
import com.daymark.app.data.*
import com.daymark.app.data.formatForDaymark
import com.daymark.app.ui.DaymarkPalette
import com.daymark.app.ui.DaymarkPalettes
import com.daymark.app.ui.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val TAG = "DaymarkWidget"
private fun debugWidgetLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

internal object WidgetStateKeys {
    val DISPLAY_MONTH = stringPreferencesKey("display_month")
    val SELECTED_DATE = stringPreferencesKey("selected_date")
    val STATE_FORMAT = intPreferencesKey("widget_state_format")
    val FOLLOWS_TODAY = booleanPreferencesKey("follows_today")
    val LAST_SEEN_TODAY = stringPreferencesKey("last_seen_today")
    val DISPLAY_MODE = stringPreferencesKey("widget_display_mode")
    val SHOW_COMPLETED = booleanPreferencesKey("widget_show_completed")
    val SHOW_AGENDA = booleanPreferencesKey("widget_show_agenda")
    val SHOW_COUNTS = booleanPreferencesKey("widget_show_counts")
    val DATE_PARAMETER = ActionParameters.Key<String>("selected_date")
    val TASK_PARAMETER = ActionParameters.Key<String>("task_id")
    val COMPLETION_PARAMETER = ActionParameters.Key<Boolean>("completed")
}

internal data class WidgetDataSnapshot(val tasks: List<TaskEntity>, val overdueCount: Int)

private object WidgetCalendarDataCache {
    private data class Key(val start: LocalDate, val end: LocalDate, val today: LocalDate)
    private data class Entry(val snapshot: WidgetDataSnapshot, val loadedAt: Long)
    private val mutex = Mutex()
    private val entries = linkedMapOf<Key, Entry>()
    private const val MAX_ENTRIES = 8
    private const val MAX_AGE_MS = 15 * 60 * 1000L

    suspend fun load(app: DaymarkApp, id: GlanceId, month: YearMonth, start: LocalDate, end: LocalDate, today: LocalDate): WidgetDataSnapshot = mutex.withLock {
        val key = Key(start, end, today)
        val cached = entries[key]
        if (cached != null && SystemClock.elapsedRealtime() - cached.loadedAt < MAX_AGE_MS) {
            debugWidgetLog("db-summary cache-hit id=$id month=$month rows=${cached.snapshot.tasks.size}")
            return@withLock cached.snapshot
        }
        val started = SystemClock.elapsedRealtime()
        debugWidgetLog("db-summary query started id=$id range=$start..$end upcomingUntil=${today.plusDays(365)}")
        val snapshot = withContext(Dispatchers.IO) {
            val taskStarted = SystemClock.elapsedRealtime()
            val tasks = app.repository.widgetTasksInRange(start, end, today, today.plusDays(365), 300)
            debugWidgetLog("db-task-query completed id=$id rows=${tasks.size} elapsedMs=${SystemClock.elapsedRealtime() - taskStarted}")
            val overdueStarted = SystemClock.elapsedRealtime()
            val overdue = app.repository.widgetOverdueCount(today)
            debugWidgetLog("db-overdue-query completed id=$id value=$overdue elapsedMs=${SystemClock.elapsedRealtime() - overdueStarted}")
            WidgetDataSnapshot(tasks, overdue)
        }
        debugWidgetLog("db-summary query completed id=$id rows=${snapshot.tasks.size} overdue=${snapshot.overdueCount} elapsedMs=${SystemClock.elapsedRealtime() - started}")
        entries[key] = Entry(snapshot, SystemClock.elapsedRealtime())
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
        snapshot
    }

    suspend fun invalidate() = mutex.withLock { entries.clear() }
}

class MonthWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(120.dp, 120.dp), DpSize(260.dp, 150.dp), DpSize(280.dp, 210.dp), DpSize(280.dp, 320.dp), DpSize(280.dp, 390.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DaymarkApp
        val today = LocalDate.now()
        updateAppWidgetState(context, id) { prefs ->
            val normalized = normalizeWidgetDateState(
                prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE],
                prefs[WidgetStateKeys.FOLLOWS_TODAY], prefs[WidgetStateKeys.LAST_SEEN_TODAY], today,
                prefs[WidgetStateKeys.STATE_FORMAT]
            )
            prefs[WidgetStateKeys.DISPLAY_MONTH] = normalized.state.month.toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = normalized.state.selectedDate.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = normalized.followsToday
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = today.toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        val stored = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)
        val state = WidgetCalendarState.fromStored(stored[WidgetStateKeys.DISPLAY_MONTH], stored[WidgetStateKeys.SELECTED_DATE], today)
        val settings = app.settings.settings.first()
        val firstDay = if (settings.weekStart == WeekStart.MONDAY) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val monthDates = CalendarLogic.monthDates(state.month, firstDay)
        val snapshot = WidgetCalendarDataCache.load(app, id, state.month, monthDates.first(), monthDates.last(), today)
        val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = when (settings.theme) { ThemeMode.SYSTEM -> systemDark; ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
        val palette = if (dark) DaymarkPalettes.Dark else DaymarkPalettes.Light
        debugWidgetLog("render id=$id month=${state.month} selected=${state.selectedDate} today=$today followsToday=${stored[WidgetStateKeys.FOLLOWS_TODAY]} theme=${settings.theme} systemDark=$systemDark dark=$dark")
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val configIntent = Intent(context, WidgetConfigurationActivity::class.java)
            .setData(Uri.parse("daymark://widget-config/$appWidgetId"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        provideContent {
            WidgetContent(
                today = today,
                state = state,
                monthDates = monthDates,
                monthTasks = snapshot.tasks,
                upcomingTasks = snapshot.tasks,
                overdueCount = snapshot.overdueCount,
                palette = palette,
                weekStart = firstDay,
                use24HourTime = settings.use24HourTime,
                dateFormat = settings.dateFormat,
                displayMode = stored[WidgetStateKeys.DISPLAY_MODE]?.let { runCatching { WidgetDisplayMode.valueOf(it) }.getOrNull() } ?: WidgetDisplayMode.AUTO,
                showCompleted = stored[WidgetStateKeys.SHOW_COMPLETED] ?: false,
                showAgenda = stored[WidgetStateKeys.SHOW_AGENDA] ?: true,
                showCounts = stored[WidgetStateKeys.SHOW_COUNTS] ?: true,
                configIntent = configIntent
            )
        }
    }
}

@Composable
private fun WidgetContent(
    today: LocalDate,
    state: WidgetCalendarState,
    monthDates: List<LocalDate>,
    monthTasks: List<TaskEntity>,
    upcomingTasks: List<TaskEntity>,
    overdueCount: Int,
    palette: DaymarkPalette,
    weekStart: DayOfWeek,
    use24HourTime: Boolean,
    dateFormat: DateFormatMode,
    displayMode: WidgetDisplayMode,
    showCompleted: Boolean,
    showAgenda: Boolean,
    showCounts: Boolean,
    configIntent: Intent
) {
    val prefs = currentState<Preferences>()
    val liveState = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE], today)
    val allTasks = remember(monthTasks, upcomingTasks) { (monthTasks + upcomingTasks).distinctBy { it.id } }
    val summary = remember(allTasks, today) { buildWidgetCalendarSummary(allTasks, today) }
    val tasksByDate = summary.tasksByDate
    val size = LocalSize.current
    val layout = widgetLayoutForSize(size.width.value, size.height.value, displayMode)
    val quickAddIntent = Intent().setClassName("com.daymark.app", MainActivity::class.java.name)
        .setData(Uri.parse("daymark://quick-add/${liveState.selectedDate}"))
        .putExtra("quick_add_date", liveState.selectedDate.toString())
    val quickAdd = GlanceModifier.clickable(actionStartActivity(quickAddIntent))
        .padding(horizontal = 9.dp, vertical = 5.dp).background(ColorProvider(palette.accent)).cornerRadius(8.dp)
    val quickAddInline = GlanceModifier.clickable(actionStartActivity(quickAddIntent)).padding(horizontal = 5.dp, vertical = 4.dp)
    val configure = GlanceModifier.clickable(actionStartActivity(configIntent)).padding(horizontal = 5.dp, vertical = 4.dp)
    Box(GlanceModifier.fillMaxSize().background(ColorProvider(palette.background))) {
      Column(GlanceModifier.fillMaxSize().background(ColorProvider(palette.surface)).cornerRadius(16.dp).padding(horizontal = if (layout == WidgetLayout.MINI) 9.dp else 10.dp, vertical = if (layout == WidgetLayout.MONTHLY) 8.dp else 10.dp)) {
        if (layout == WidgetLayout.MINI) {
            MiniLayout(today, upcomingTasks, overdueCount, quickAdd, configure, palette, use24HourTime, dateFormat, showCounts)
        } else if (layout == WidgetLayout.WEEKLY) {
            WeeklyLayout(today, liveState, summary.days, tasksByDate, quickAdd, quickAddInline, configure, palette, weekStart, use24HourTime, dateFormat, showCompleted, showAgenda, showCounts)
        } else {
            MonthlyLayout(today, liveState, monthDates, allTasks, summary.days, tasksByDate, quickAdd, quickAddInline, configure, palette, weekStart, use24HourTime, dateFormat, showAgenda && widgetShowsAgenda(size.height.value), showCompleted, showCounts, size.height.value >= 370f, size.height.value >= 390f, size.width.value >= 260f, size.width.value >= 245f)
        }
      }
    }
}

@Composable
private fun MiniLayout(today: LocalDate, tasks: List<TaskEntity>, overdueCount: Int, quickAdd: GlanceModifier, configure: GlanceModifier, palette: DaymarkPalette, use24HourTime: Boolean, dateFormat: DateFormatMode, showCounts: Boolean) {
    val active = tasks.filterNot { it.isCompleted }
    val nextTask = active.mapNotNull { task -> listOfNotNull(task.plannedDate, task.deadlineDate).filter { it >= today }.minOrNull()?.let { it to task } }.minByOrNull { it.first }?.second
    val nextDeadline = active.filter { it.deadlineDate != null && it.deadlineDate >= today }.minByOrNull { it.deadlineDate!! }
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("DAYMARK", style = TextStyle(color = ColorProvider(palette.accent), fontSize = 11.sp))
        Spacer(GlanceModifier.defaultWeight())
        Text("+", quickAdd, style = TextStyle(color = ColorProvider(palette.onAccent), fontSize = 14.sp))
        Text("⚙", configure, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp))
    }
    Spacer(GlanceModifier.height(6.dp))
    MiniLine("NEXT TASK", nextTask?.title ?: "Nothing scheduled", nextTask?.let { listOfNotNull(it.plannedDate, it.deadlineDate).filter { date -> date >= today }.minOrNull()?.formatForDaymark(dateFormat) }, palette)
    Spacer(GlanceModifier.height(5.dp))
    MiniLine("NEXT DEADLINE", nextDeadline?.title ?: "You're clear. No upcoming deadlines.", nextDeadline?.deadlineDate?.formatForDaymark(dateFormat), palette)
    Spacer(GlanceModifier.height(4.dp))
    if (showCounts) Text(if (overdueCount > 0) "$overdueCount overdue" else "No overdue tasks", style = TextStyle(color = ColorProvider(if (overdueCount > 0) palette.missed else palette.textSecondary), fontSize = 11.sp))
}

@Composable
private fun MiniLine(label: String, title: String, date: String?, palette: DaymarkPalette) {
    Column {
        Text(label, style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 9.sp))
        Text(title, maxLines = if (title.startsWith("You're clear")) 2 else 1, style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 12.sp))
        if (date != null) Text(date, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 10.sp))
    }
}

@Composable
private fun WeeklyLayout(today: LocalDate, state: WidgetCalendarState, daySummaries: Map<LocalDate, CalendarDaySummary>, tasksByDate: Map<LocalDate, List<TaskEntity>>, quickAdd: GlanceModifier, quickAddInline: GlanceModifier, configure: GlanceModifier, palette: DaymarkPalette, weekStart: DayOfWeek, use24HourTime: Boolean, dateFormat: DateFormatMode, showCompleted: Boolean, showAgenda: Boolean, showCounts: Boolean) {
    val weekDate = state.selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(weekStart))
    val dates = (0L..6L).map { weekDate.plusDays(it) }
    Row(GlanceModifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 13.sp))
        Spacer(GlanceModifier.defaultWeight())
        Text("Today", GlanceModifier.clickable(actionRunCallback<GoToWidgetTodayAction>()).padding(horizontal = 4.dp, vertical = 3.dp), style = TextStyle(color = ColorProvider(palette.accent), fontSize = 11.sp))
        Text("+", quickAdd, style = TextStyle(color = ColorProvider(palette.onAccent), fontSize = 14.sp))
        Text("⚙", configure, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp))
    }
    Row(GlanceModifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        dates.forEach { date ->
            val isSelected = date == state.selectedDate
            val action = actionRunCallback<SelectWidgetDateAction>(actionParametersOf(WidgetStateKeys.DATE_PARAMETER to date.toString()))
            val tone = daySummaries[date]?.dominantStatus?.calendarStatusTone() ?: CalendarStatusTone.NEUTRAL
            val fill = tone.argb?.let { Color(it) } ?: palette.surfaceSecondary
            val text = when (tone) {
                CalendarStatusTone.DEADLINE_TODAY, CalendarStatusTone.COMPLETED -> Color(0xFF191C1A)
                CalendarStatusTone.NEUTRAL -> palette.textPrimary
                else -> Color.White
            }
            Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(1.dp).background(ColorProvider(if (isSelected || date == today) palette.accent else Color.Transparent)).cornerRadius(7.dp).clickable(action)) {
              Column(GlanceModifier.fillMaxSize().padding(if (isSelected || date == today) 1.dp else 0.dp).background(ColorProvider(fill)).cornerRadius(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfWeek.name.take(1), style = TextStyle(color = ColorProvider(if (tone == CalendarStatusTone.NEUTRAL) palette.textMuted else text), fontSize = 9.sp))
                Text(date.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(text), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                val count = daySummaries[date]?.totalOccurrences ?: 0
                if (showCounts && count > 0) Text(count.toString(), style = TextStyle(color = ColorProvider(text), fontSize = 9.sp))
              }
            }
        }
    }
    if (showAgenda) {
        val selectedTasks = tasksByDate[state.selectedDate].orEmpty().distinctBy { it.id }.filter { showCompleted || !it.isCompleted }
        Text(if (showCounts) "${state.selectedDate.formatForDaymark(dateFormat)} · ${selectedTasks.size} tasks" else state.selectedDate.formatForDaymark(dateFormat), style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 12.sp))
        selectedTasks.take(2).forEach { task -> WidgetTaskLine(task, palette, use24HourTime) }
        if (selectedTasks.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("No tasks planned for this day.", style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 12.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text("+ Quick Add", quickAddInline, style = TextStyle(color = ColorProvider(palette.accent), fontSize = 12.sp))
        }
    }
}

@Composable
private fun MonthlyLayout(today: LocalDate, state: WidgetCalendarState, monthDates: List<LocalDate>, tasks: List<TaskEntity>, daySummaries: Map<LocalDate, CalendarDaySummary>, tasksByDate: Map<LocalDate, List<TaskEntity>>, quickAdd: GlanceModifier, quickAddInline: GlanceModifier, configure: GlanceModifier, palette: DaymarkPalette, weekStart: DayOfWeek, use24HourTime: Boolean, dateFormat: DateFormatMode, showAgenda: Boolean, showCompleted: Boolean, showCounts: Boolean, showUpcoming: Boolean, showUpcomingWithFullAgenda: Boolean, showTodayAction: Boolean, showConfigureAction: Boolean) {
    Row(GlanceModifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", GlanceModifier.clickable(actionRunCallback<PreviousWidgetMonthAction>()).padding(horizontal = 8.dp, vertical = 5.dp), style = TextStyle(color = ColorProvider(palette.accent), fontSize = 18.sp, fontWeight = FontWeight.Medium))
        Text(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 18.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.defaultWeight())
        if (showTodayAction) Text("Today", GlanceModifier.clickable(actionRunCallback<GoToWidgetTodayAction>()).padding(horizontal = 4.dp, vertical = 5.dp), style = TextStyle(color = ColorProvider(palette.accent), fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Text("+", quickAdd, style = TextStyle(color = ColorProvider(palette.onAccent), fontSize = 14.sp))
        if (showConfigureAction) Text("⚙", configure, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp))
        Text("›", GlanceModifier.clickable(actionRunCallback<NextWidgetMonthAction>()).padding(horizontal = 8.dp, vertical = 5.dp), style = TextStyle(color = ColorProvider(palette.accent), fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
        Row(GlanceModifier.fillMaxWidth().height(15.dp), verticalAlignment = Alignment.CenterVertically) {
        (0..6).map { weekStart.plus(it.toLong()).name.take(1) }.forEach { day -> Text(day, GlanceModifier.defaultWeight(), style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 13.sp, fontWeight = FontWeight.Medium)) }
    }
    monthDates.chunked(7).forEach { week ->
        Row(GlanceModifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            week.forEach { date ->
                val selected = date == state.selectedDate
                val summary = daySummaries[date]
                val tone = summary?.dominantStatus?.calendarStatusTone() ?: CalendarStatusTone.NEUTRAL
                val statusColor = tone.argb?.let { Color(it) } ?: palette.surfaceSecondary
                val foreground = when (tone) {
                    CalendarStatusTone.DEADLINE_TODAY, CalendarStatusTone.COMPLETED -> Color(0xFF191C1A)
                    CalendarStatusTone.NEUTRAL -> if (YearMonth.from(date) == state.month) palette.textPrimary else palette.textMuted
                    else -> Color.White
                }
                val action = actionRunCallback<SelectWidgetDateAction>(actionParametersOf(WidgetStateKeys.DATE_PARAMETER to date.toString()))
                val outline = if (selected) palette.accent else if (date == today) palette.textSecondary else Color.Transparent
                Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(1.dp).background(ColorProvider(outline)).cornerRadius(7.dp).clickable(action)) {
                    Row(GlanceModifier.fillMaxSize().padding(if (selected || date == today) 1.dp else 0.dp).background(ColorProvider(statusColor)).cornerRadius(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(foreground), fontSize = if (selected) 17.sp else 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium))
                        if (showCounts && summary != null && summary.totalOccurrences > 1) Text("·${summary.totalOccurrences}", style = TextStyle(color = ColorProvider(foreground), fontSize = 9.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
    val selectedTasks = remember(tasksByDate, state.selectedDate, showCompleted) {
        tasksByDate[state.selectedDate].orEmpty().distinctBy { it.id }.filter { showCompleted || !it.isCompleted }
    }
    val selectedTaskIds = remember(selectedTasks) { selectedTasks.map { it.id }.toSet() }
    val upcoming = remember(tasks, today, selectedTaskIds) {
        tasks.filter { !it.isCompleted && listOfNotNull(it.plannedDate, it.deadlineDate).any { date -> date >= today } }
            .sortedBy { listOfNotNull(it.deadlineDate, it.plannedDate).minOrNull() }
            .filterNot { it.id in selectedTaskIds }
            .take(2)
    }
    val effort = remember(tasksByDate, state.selectedDate) {
        tasksByDate[state.selectedDate].orEmpty().filter { !it.isCompleted }.sumOf { it.estimatedDurationMinutes ?: 0 }
    }
    if (showAgenda) {
        Spacer(GlanceModifier.height(4.dp))
        Text("${state.selectedDate.formatForDaymark(dateFormat)}${if (showCounts) " · ${selectedTasks.size} ${if (selectedTasks.size == 1) "task" else "tasks"}" else ""}", style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 16.sp, fontWeight = FontWeight.Medium))
        if (selectedTasks.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("No tasks planned for this day.", style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 13.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text("+ Quick Add", quickAddInline, style = TextStyle(color = ColorProvider(palette.accent), fontSize = 13.sp))
        }
        selectedTasks.take(3).forEach { WidgetTaskLine(it, palette, use24HourTime) }
        if (showCounts && selectedTasks.size > 3) Text("+${selectedTasks.size - 3} more", style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 12.sp))
    }
    if (showUpcoming && (!showAgenda || selectedTasks.size < 3 || showUpcomingWithFullAgenda)) {
        if (effort > 0) Text("Planned effort · ${effort / 60}h ${effort % 60}m", style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 12.sp))
        if (upcoming.isNotEmpty()) {
            Text("UP NEXT", style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 12.sp, fontWeight = FontWeight.Medium))
            upcoming.forEach { WidgetTaskLine(it, palette, use24HourTime) }
        }
    }
}

@Composable
private fun WidgetTaskLine(task: TaskEntity, palette: DaymarkPalette, use24HourTime: Boolean) {
    val completeAction = actionRunCallback<CompleteWidgetTaskAction>(actionParametersOf(
        WidgetStateKeys.TASK_PARAMETER to task.id,
        WidgetStateKeys.COMPLETION_PARAMETER to !task.isCompleted
    ))
    Row(GlanceModifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (task.isCompleted) "✓" else "○", GlanceModifier.clickable(completeAction).padding(end = 5.dp), style = TextStyle(color = ColorProvider(if (task.isCompleted) palette.completed else palette.textMuted), fontSize = 14.sp))
        Text(task.title, GlanceModifier.defaultWeight(), maxLines = 1, style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 14.sp))
        task.deadlineTime?.let { Text(it.format(DateTimeFormatter.ofPattern(if (use24HourTime) "HH:mm" else "h:mm a")), style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 12.sp)) }
    }
}

class SelectWidgetDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val started = SystemClock.elapsedRealtime()
        debugWidgetLog("action=date-select received id=$glanceId")
        val date = parameters[WidgetStateKeys.DATE_PARAMETER]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        debugWidgetLog("action=date-select parameters processed id=$glanceId date=$date")
        val stateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("state update started id=$glanceId date=$date")
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.DISPLAY_MONTH] = YearMonth.from(date).toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = date.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = false
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = LocalDate.now().toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("state persisted id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - stateStarted}")
        val updateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("glance update requested id=$glanceId")
        MonthWidget().update(context, glanceId)
        debugWidgetLog("glance update returned id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - updateStarted}; launcher render is not measured")
        debugWidgetLog("action=date-select callbackElapsedMs=${SystemClock.elapsedRealtime() - started}")
    }
}

class GoToWidgetTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val started = SystemClock.elapsedRealtime()
        val today = LocalDate.now()
        val stateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("state update started id=$glanceId action=today")
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.DISPLAY_MONTH] = YearMonth.from(today).toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = today.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = true
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = today.toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("state update completed id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - stateStarted}")
        val updateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("glance update requested id=$glanceId action=today")
        MonthWidget().update(context, glanceId)
        debugWidgetLog("glance update returned id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - updateStarted}; launcher render is not measured")
        debugWidgetLog("action=today callbackElapsedMs=${SystemClock.elapsedRealtime() - started}")
    }
}

abstract class ChangeWidgetMonthAction(private val delta: Long) : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val started = SystemClock.elapsedRealtime()
        val stateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("state update started id=$glanceId delta=$delta")
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE])
                .moveMonth(delta)
            prefs[WidgetStateKeys.DISPLAY_MONTH] = current.month.toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = current.selectedDate.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = false
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = LocalDate.now().toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("state persisted id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - stateStarted}")
        val updateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("glance update requested id=$glanceId")
        MonthWidget().update(context, glanceId)
        debugWidgetLog("glance update returned id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - updateStarted}; launcher render is not measured")
        debugWidgetLog("action=month-nav callbackElapsedMs=${SystemClock.elapsedRealtime() - started}")
    }
}

class PreviousWidgetMonthAction : ChangeWidgetMonthAction(-1)
class NextWidgetMonthAction : ChangeWidgetMonthAction(1)

class CompleteWidgetTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val started = SystemClock.elapsedRealtime()
        debugWidgetLog("action=complete received id=$glanceId")
        val taskId = parameters[WidgetStateKeys.TASK_PARAMETER] ?: return
        val completed = parameters[WidgetStateKeys.COMPLETION_PARAMETER] ?: return
        val app = context.applicationContext as DaymarkApp
        val queryStarted = SystemClock.elapsedRealtime()
        val task = app.repository.complete(taskId, completed) ?: return
        debugWidgetLog("task update complete id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - queryStarted}")
        if (completed) app.reminderScheduler.cancel(taskId)
        else if (app.settings.settings.first().notificationsEnabled && task.reminderEnabled) app.reminderScheduler.schedule(taskId, task.title, task.reminderAt)
        val updateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("glance update requested id=$glanceId")
        WidgetRefresh.update(context)
        debugWidgetLog("glance update returned id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - updateStarted}; launcher render is not measured")
        debugWidgetLog("action=complete callbackElapsedMs=${SystemClock.elapsedRealtime() - started}")
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthWidget() }
object WidgetRefresh {
    suspend fun update(context: Context) {
        WidgetCalendarDataCache.invalidate()
        MonthWidget().updateAll(context)
    }
}
