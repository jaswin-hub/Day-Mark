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
import androidx.datastore.preferences.core.MutablePreferences
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
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
    val LAST_INTERACTION_ID = stringPreferencesKey("widget_last_interaction_id")
    val DATE_PARAMETER = ActionParameters.Key<String>("selected_date")
    val TASK_PARAMETER = ActionParameters.Key<String>("task_id")
    val COMPLETION_PARAMETER = ActionParameters.Key<Boolean>("completed")
}

private object WidgetInteractionLock {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withLock(id: GlanceId, action: suspend () -> T): T =
        locks.computeIfAbsent(id.toString()) { Mutex() }.withLock { action() }
}

private suspend fun persistWidgetInteraction(
    context: Context,
    glanceId: GlanceId,
    eventId: String,
    actionName: String,
    mutation: (MutablePreferences) -> Unit
) = WidgetInteractionLock.withLock(glanceId) {
    val beforePrefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
    val before = WidgetCalendarState.fromStored(beforePrefs[WidgetStateKeys.DISPLAY_MONTH], beforePrefs[WidgetStateKeys.SELECTED_DATE])
    debugWidgetLog("event=$eventId action=$actionName callback-entered id=$glanceId selectedBefore=${before.selectedDate}")
    updateAppWidgetState(context, glanceId) { prefs ->
        mutation(prefs)
        prefs[WidgetStateKeys.LAST_INTERACTION_ID] = eventId
    }
    val afterPrefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
    val after = WidgetCalendarState.fromStored(afterPrefs[WidgetStateKeys.DISPLAY_MONTH], afterPrefs[WidgetStateKeys.SELECTED_DATE])
    debugWidgetLog("event=$eventId action=$actionName state-persisted id=$glanceId selectedAfter=${after.selectedDate}")
    debugWidgetLog("event=$eventId action=$actionName widget-update-requested id=$glanceId")
    MonthWidget().update(context, glanceId)
}

internal data class WidgetDataSnapshot(val tasks: List<TaskEntity>, val upcomingTasks: List<TaskEntity>, val overdueCount: Int)

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
            coroutineScope {
                val taskQuery = async {
                    val taskStarted = SystemClock.elapsedRealtime()
                    debugWidgetLog("db-calendar-query started id=$id range=$start..$end")
                    app.repository.widgetTasksInRange(start, end).also {
                        debugWidgetLog("db-calendar-query completed id=$id rows=${it.size} elapsedMs=${SystemClock.elapsedRealtime() - taskStarted}")
                    }
                }
                val upcomingQuery = async {
                    val upcomingStarted = SystemClock.elapsedRealtime()
                    debugWidgetLog("db-upcoming-query started id=$id range=$today..${today.plusDays(365)} limit=50")
                    app.repository.widgetUpcomingTasks(today, today.plusDays(365), 50).also {
                        debugWidgetLog("db-upcoming-query completed id=$id rows=${it.size} elapsedMs=${SystemClock.elapsedRealtime() - upcomingStarted}")
                    }
                }
                val overdueQuery = async {
                    val overdueStarted = SystemClock.elapsedRealtime()
                    debugWidgetLog("db-overdue-query started id=$id")
                    app.repository.widgetOverdueCount(today).also {
                        debugWidgetLog("db-overdue-query completed id=$id value=$it elapsedMs=${SystemClock.elapsedRealtime() - overdueStarted}")
                    }
                }
                WidgetDataSnapshot(taskQuery.await(), upcomingQuery.await(), overdueQuery.await())
            }
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
        setOf(DpSize(220.dp, 190.dp), DpSize(280.dp, 255.dp), DpSize(300.dp, 320.dp), DpSize(360.dp, 390.dp), DpSize(420.dp, 400.dp))
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
        val monthDates = widgetMonthDates(state.month, firstDay)
        val snapshot = WidgetCalendarDataCache.load(app, id, state.month, monthDates.first(), monthDates.last(), today)
        val interactionId = stored[WidgetStateKeys.LAST_INTERACTION_ID] ?: "initial"
        debugWidgetLog("event=$interactionId data-retrieval-complete id=$id month=${state.month} selected=${state.selectedDate} tasks=${snapshot.tasks.size}")
        val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = when (settings.theme) { ThemeMode.SYSTEM -> systemDark; ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
        val palette = if (dark) DaymarkPalettes.Dark else DaymarkPalettes.Light
        debugWidgetLog("event=$interactionId render id=$id month=${state.month} selected=${state.selectedDate} today=$today followsToday=${stored[WidgetStateKeys.FOLLOWS_TODAY]} theme=${settings.theme} systemDark=$systemDark dark=$dark")
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val configIntent = Intent(context, WidgetConfigurationActivity::class.java)
            .setData(Uri.parse("daymark://widget-config/$appWidgetId"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        provideContent {
            WidgetContent(
                today = today,
                state = state,
                monthDates = monthDates,
                tasks = snapshot.tasks,
                upcomingTasks = snapshot.upcomingTasks,
                overdueCount = snapshot.overdueCount,
                palette = palette,
                weekStart = firstDay,
                use24HourTime = settings.use24HourTime,
                dateFormat = settings.dateFormat,
                displayMode = stored[WidgetStateKeys.DISPLAY_MODE]?.let { runCatching { WidgetDisplayMode.valueOf(it) }.getOrNull() } ?: WidgetDisplayMode.AUTO,
                showCompleted = stored[WidgetStateKeys.SHOW_COMPLETED] ?: false,
                showAgenda = stored[WidgetStateKeys.SHOW_AGENDA] ?: true,
                showCounts = stored[WidgetStateKeys.SHOW_COUNTS] ?: true,
                configIntent = configIntent,
                fontScale = context.resources.configuration.fontScale
            )
        }
    }
}

@Composable
private fun WidgetContent(
    today: LocalDate,
    state: WidgetCalendarState,
    monthDates: List<LocalDate>,
    tasks: List<TaskEntity>,
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
    configIntent: Intent,
    fontScale: Float
) {
    // Keep the selection and database snapshot from the same provideGlance pass.
    // Re-reading state here could pair a newer selected date with an older month's
    // task snapshot if a widget action arrives while this content is composed.
    val liveState = state
    val allTasks = tasks
    val summary = remember(allTasks, today) { buildWidgetCalendarSummary(allTasks, today) }
    val tasksByDate = summary.tasksByDate
    val size = LocalSize.current
    val layout = widgetLayoutForSize(size.width.value, size.height.value, displayMode, monthDates.size / 7, fontScale)
    val quickAddAction = actionRunCallback<QuickAddWidgetAction>(actionParametersOf(
        WidgetStateKeys.DATE_PARAMETER to liveState.selectedDate.toString()
    ))
    val quickAdd = GlanceModifier.clickable(quickAddAction)
        .padding(horizontal = 9.dp, vertical = 5.dp).background(ColorProvider(palette.accent)).cornerRadius(8.dp)
    val quickAddInline = GlanceModifier.clickable(quickAddAction).padding(horizontal = 5.dp, vertical = 4.dp)
    val configure = GlanceModifier.clickable(actionStartActivity(configIntent)).padding(horizontal = 5.dp, vertical = 4.dp)
    Box(GlanceModifier.fillMaxSize().background(ColorProvider(palette.background))) {
      Column(GlanceModifier.fillMaxSize().background(ColorProvider(palette.surface)).cornerRadius(16.dp).padding(horizontal = if (layout == WidgetLayout.MINI) 9.dp else 10.dp, vertical = if (layout == WidgetLayout.MONTHLY) 8.dp else 10.dp)) {
        if (layout == WidgetLayout.MINI) {
            MiniLayout(today, upcomingTasks, overdueCount, quickAdd, configure, palette, use24HourTime, dateFormat, showCounts)
        } else if (layout == WidgetLayout.WEEKLY) {
            WeeklyLayout(today, liveState, summary.days, tasksByDate, upcomingTasks, quickAdd, quickAddInline, configure, palette, weekStart, dateFormat, showCompleted, showAgenda, showCounts)
        } else {
            MonthlyLayout(today, liveState, monthDates, summary.days, tasksByDate, upcomingTasks, quickAdd, quickAddInline, configure, palette, weekStart, showAgenda, showCompleted, showCounts, showConfigureAction = size.width.value >= 245f, widgetHeightDp = size.height.value, fontScale = fontScale)
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
private fun WeeklyLayout(today: LocalDate, state: WidgetCalendarState, daySummaries: Map<LocalDate, CalendarDaySummary>, tasksByDate: Map<LocalDate, List<TaskEntity>>, upcomingTasks: List<TaskEntity>, quickAdd: GlanceModifier, quickAddInline: GlanceModifier, configure: GlanceModifier, palette: DaymarkPalette, weekStart: DayOfWeek, dateFormat: DateFormatMode, showCompleted: Boolean, showAgenda: Boolean, showCounts: Boolean) {
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
        val selectedTasks = tasksByDate[state.selectedDate].orEmpty().distinctBy { it.id }
        Text("${state.selectedDate.formatForDaymark(dateFormat)} · ${selectedTasks.size} ${if (selectedTasks.size == 1) "task" else "tasks"}", style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 12.sp))
        selectedTasks.take(2).forEach { task ->
            val status = CalendarLogic.dominantStatus(daySummaries[state.selectedDate]?.occurrences.orEmpty().filter { it.taskId == task.id })
            WidgetTaskLine(task, status, palette)
        }
        if (selectedTasks.isEmpty()) SelectedDateEmptyState(state.selectedDate, quickAddInline, palette, 30f)
    }
}

@Composable
private fun MonthlyLayout(
    today: LocalDate,
    state: WidgetCalendarState,
    monthDates: List<LocalDate>,
    daySummaries: Map<LocalDate, CalendarDaySummary>,
    tasksByDate: Map<LocalDate, List<TaskEntity>>,
    upcomingTasks: List<TaskEntity>,
    quickAdd: GlanceModifier,
    quickAddInline: GlanceModifier,
    configure: GlanceModifier,
    palette: DaymarkPalette,
    weekStart: DayOfWeek,
    showAgendaPreference: Boolean,
    showCompleted: Boolean,
    showCounts: Boolean,
    showConfigureAction: Boolean,
    widgetHeightDp: Float,
    fontScale: Float
) {
    val rows = monthDates.chunked(7)
    val metrics = monthlyWidgetMetrics(fontScale)
    val agendaAvailable = monthlyAgendaAvailableHeight(widgetHeightDp, rows.size, fontScale)
    val agendaLimit = monthlyAgendaTaskLimit(widgetHeightDp, rows.size, fontScale)
    val scale = fontScale.coerceAtLeast(1f)
    val agendaHeadingHeight = maxOf(22f, 20f * scale + 2f)
    val agendaTaskHeight = maxOf(42f, 36f * scale + 6f)
    val agendaEmptyHeight = maxOf(30f, 20f * scale + 8f)
    val visibleTasks = remember(tasksByDate, state.selectedDate, showCompleted) {
        // Completed tasks remain part of the selected-date agenda; their state is shown below.
        tasksByDate[state.selectedDate].orEmpty().distinctBy { it.id }
    }
    val requestedTaskLimit = agendaLimit.coerceAtMost(3)
    val needsRemainingCount = visibleTasks.size > requestedTaskLimit
    val taskSpaceLimit = ((agendaAvailable - agendaHeadingHeight - 4f - if (needsRemainingCount) 18f else 0f) / agendaTaskHeight)
        .toInt().coerceIn(0, 3)
    val taskLimit = if (requestedTaskLimit > 0) minOf(requestedTaskLimit, taskSpaceLimit.coerceAtLeast(1)) else 0
    val showRemainingCount = visibleTasks.size > taskLimit &&
        agendaAvailable >= agendaHeadingHeight + 4f + taskLimit * agendaTaskHeight + 18f
    // Monthly mode is only selected when the size bucket fits the grid plus at least
    // one selected-date agenda row. Never silently omit the agenda within monthly mode.
    val showAgenda = showAgendaPreference
    debugWidgetLog("monthly-layout size=${LocalSize.current} selected=${state.selectedDate} tasks=${visibleTasks.size} agendaEnabled=$showAgenda availableDp=$agendaAvailable emptyHeightDp=$agendaEmptyHeight")

    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth().height(metrics.monthHeaderHeight.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", GlanceModifier.width(34.dp).fillMaxHeight()
                .semantics { contentDescription = "Previous month" }
                .clickable(actionRunCallback<PreviousWidgetMonthAction>()),
                style = TextStyle(color = ColorProvider(palette.accent), fontSize = 18.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), GlanceModifier.defaultWeight()
                .semantics { contentDescription = state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")) },
                style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 20.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text("›", GlanceModifier.width(34.dp).fillMaxHeight()
                .semantics { contentDescription = "Next month" }
                .clickable(actionRunCallback<NextWidgetMonthAction>()),
                style = TextStyle(color = ColorProvider(palette.accent), fontSize = 18.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        Row(GlanceModifier.fillMaxWidth().height(metrics.actionHeaderHeight.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Today", GlanceModifier.clickable(actionRunCallback<GoToWidgetTodayAction>())
                .padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = "Select today" },
                style = TextStyle(color = ColorProvider(palette.accent), fontSize = 14.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.defaultWeight())
            Text("+ Add task", quickAdd, style = TextStyle(color = ColorProvider(palette.onAccent), fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            if (showConfigureAction) Text("⚙", configure.semantics { contentDescription = "Widget settings" }, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 16.sp))
        }
        Row(GlanceModifier.fillMaxWidth().height(metrics.weekdayHeight.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..6).forEach { index ->
                val day = weekStart.plus(index.toLong()).name.take(1)
                Text(day, GlanceModifier.defaultWeight().fillMaxHeight().semantics { contentDescription = weekStart.plus(index.toLong()).name },
                    style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
        rows.forEach { week ->
            Row(GlanceModifier.fillMaxWidth().height(metrics.weekRowHeight.dp), verticalAlignment = Alignment.CenterVertically) {
                week.forEach { date ->
                    val summary = daySummaries[date]
                    val cellState = widgetDateCellState(date, state.selectedDate, today, summary)
                    val selected = cellState.selected
                    val isToday = cellState.today
                    val tone = cellState.tone
                    val statusColor = tone.argb?.let { Color(it) } ?: palette.surfaceSecondary
                    val foreground = when (tone) {
                        CalendarStatusTone.DEADLINE_TODAY, CalendarStatusTone.COMPLETED -> Color(0xFF191C1A)
                        CalendarStatusTone.NEUTRAL -> if (YearMonth.from(date) == state.month) palette.textPrimary else palette.textMuted
                        else -> Color.White
                    }
                    val action = actionRunCallback<SelectWidgetDateAction>(actionParametersOf(WidgetStateKeys.DATE_PARAMETER to date.toString()))
                    val statusLabel = summary?.dominantStatus?.widgetStatusLabel() ?: "No tasks"
                    val outline = when {
                        selected -> palette.accent
                        isToday && tone == CalendarStatusTone.UPCOMING || isToday && tone == CalendarStatusTone.MISSED -> Color.White
                        isToday && tone == CalendarStatusTone.DEADLINE_TODAY || isToday && tone == CalendarStatusTone.COMPLETED -> Color(0xFF191C1A)
                        isToday -> palette.accent
                        else -> Color.Transparent
                    }
                    val description = buildString {
                        append(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
                        if (isToday) append(", today")
                        if (selected) append(", selected")
                        append(", $statusLabel")
                        if (summary != null) append(", ${summary.totalOccurrences} ${if (summary.totalOccurrences == 1) "task occurrence" else "task occurrences"}")
                    }
                    Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 1.dp, vertical = 1.dp)
                        .background(ColorProvider(outline)).cornerRadius(7.dp)
                        .semantics { contentDescription = description }.clickable(action)) {
                        Box(GlanceModifier.fillMaxSize().padding(if (selected || isToday) 1.dp else 0.dp)
                            .background(ColorProvider(statusColor)).cornerRadius(6.dp), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(foreground),
                                fontSize = if (selected) 19.sp else 18.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
                        }
                    }
                }
            }
        }
        if (showAgenda) {
            Spacer(GlanceModifier.height(4.dp))
            val dateLabel = state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")).uppercase()
            val todayPrefix = if (state.selectedDate == today) "TODAY · " else ""
            Row(GlanceModifier.fillMaxWidth().height(agendaHeadingHeight.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$todayPrefix$dateLabel", GlanceModifier.defaultWeight().semantics {
                    contentDescription = "Selected date, ${state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d yyyy"))}"
                },
                    style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                if (showCounts) Text("${visibleTasks.size} ${if (visibleTasks.size == 1) "task" else "tasks"}",
                    GlanceModifier.semantics { contentDescription = "${visibleTasks.size} tasks for ${state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}" },
                    style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp), maxLines = 1)
            }
            if (visibleTasks.isEmpty()) {
                SelectedDateEmptyState(state.selectedDate, quickAddInline, palette, agendaEmptyHeight)
            } else {
                visibleTasks.take(taskLimit).forEach { task ->
                    val status = CalendarLogic.dominantStatus(daySummaries[state.selectedDate]?.occurrences.orEmpty().filter { it.taskId == task.id })
                    WidgetTaskLine(task, status, palette, agendaTaskHeight)
                }
                val remaining = (visibleTasks.size - taskLimit).coerceAtLeast(0)
                if (remaining > 0 && showRemainingCount) Text("+$remaining more ${if (remaining == 1) "task" else "tasks"}",
                    GlanceModifier.fillMaxWidth().height(18.dp),
                    style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp), maxLines = 1)
            }
            val upcoming = upcomingTasks.asSequence()
                .filterNot { task -> visibleTasks.any { it.id == task.id } }
                .mapNotNull { task ->
                    val nextDate = listOfNotNull(task.plannedDate, task.deadlineDate).filter { it >= today }.minOrNull()
                    nextDate?.let { it to task }
                }
                .sortedBy { it.first }
                .toList()
            val usedAgendaHeight = agendaHeadingHeight + 4f + if (visibleTasks.isEmpty()) agendaEmptyHeight
                else taskLimit * agendaTaskHeight + if (showRemainingCount) 18f else 0f
            val upcomingSpace = agendaAvailable - usedAgendaHeight
            val upcomingLineHeight = maxOf(42f, 36f * scale + 6f)
            val upcomingLimit = ((upcomingSpace - 20f) / upcomingLineHeight).toInt().coerceIn(0, 3)
            if (upcomingLimit > 0) {
                Text("UP NEXT", GlanceModifier.fillMaxWidth().height(20.dp).semantics {
                    contentDescription = "Upcoming tasks after ${state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}"
                }, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                if (upcoming.isEmpty()) {
                    Text("Nothing else scheduled.", GlanceModifier.fillMaxWidth().height(upcomingLineHeight.dp),
                        style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 13.sp), maxLines = 1)
                } else {
                    upcoming.take(upcomingLimit).forEach { (date, task) ->
                        val status = CalendarLogic.dominantStatus(daySummaries[date]?.occurrences.orEmpty().filter { it.taskId == task.id })
                        WidgetTaskLine(task, status, palette, upcomingLineHeight, scheduledDate = date)
                    }
                }
            }
        }
    }
}

private fun TaskStatus.widgetStatusLabel(): String = when (this) {
    TaskStatus.UPCOMING -> "Upcoming"
    TaskStatus.PLANNED_TODAY -> "Planned today"
    TaskStatus.MISSED_PLAN -> "Plan missed"
    TaskStatus.OVERDUE -> "Overdue"
    TaskStatus.DUE_TODAY -> "Due today"
    TaskStatus.COMPLETED -> "Completed"
    TaskStatus.UNSCHEDULED -> "Unscheduled"
}

@Composable
private fun SelectedDateEmptyState(
    selectedDate: LocalDate,
    quickAdd: GlanceModifier,
    palette: DaymarkPalette,
    heightDp: Float
) {
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))
    Row(
        GlanceModifier.fillMaxWidth().height(heightDp.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "No tasks planned for this day.",
            GlanceModifier.semantics { contentDescription = "No tasks planned for $dateLabel" },
            style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 13.sp),
            maxLines = 1
        )
        Text(
            "+ Quick Add",
            quickAdd.padding(horizontal = 4.dp).semantics { contentDescription = "Quick Add task for $dateLabel" },
            style = TextStyle(color = ColorProvider(palette.accent), fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
private fun WidgetTaskLine(
    task: TaskEntity,
    status: TaskStatus?,
    palette: DaymarkPalette,
    heightDp: Float = 42f,
    scheduledDate: LocalDate? = null
) {
    val completeAction = actionRunCallback<CompleteWidgetTaskAction>(actionParametersOf(
        WidgetStateKeys.TASK_PARAMETER to task.id,
        WidgetStateKeys.COMPLETION_PARAMETER to !task.isCompleted
    ))
    val label = status?.widgetStatusLabel() ?: if (task.isCompleted) "Completed" else "Upcoming"
    val labelColor = status?.calendarStatusTone()?.argb?.let { Color(it) } ?: palette.textSecondary
    val priority = task.priority.name.lowercase().replaceFirstChar { it.uppercase() }
    val due = task.deadlineDate?.format(DateTimeFormatter.ofPattern("MMM d"))
    val details = buildList {
        add(if (task.isCompleted) "Completed" else label)
        add("$priority priority")
        scheduledDate?.let { add("${it.format(DateTimeFormatter.ofPattern("MMM d"))} planned") }
        due?.let { add("Due $it") }
    }.joinToString(" · ")
    Column(GlanceModifier.fillMaxWidth().height(heightDp.dp).semantics {
        contentDescription = "${task.title}, $details"
    }) {
        Row(GlanceModifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (task.isCompleted) "✓" else "○", GlanceModifier.width(28.dp).fillMaxHeight().clickable(completeAction)
                .semantics { contentDescription = if (task.isCompleted) "Reopen ${task.title}" else "Complete ${task.title}" },
                style = TextStyle(color = ColorProvider(if (task.isCompleted) palette.completed else palette.textMuted), fontSize = 16.sp))
            Text(task.title, GlanceModifier.defaultWeight(), maxLines = 1,
                style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 15.sp))
            Text(priority, style = TextStyle(color = ColorProvider(palette.textSecondary), fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        Text(details, GlanceModifier.fillMaxWidth().padding(start = 28.dp), maxLines = 1,
            style = TextStyle(color = ColorProvider(labelColor), fontSize = 12.sp))
    }
}

class SelectWidgetDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = UUID.randomUUID().toString()
        debugWidgetLog("event=$eventId interaction-received action=date-select id=$glanceId params=$parameters")
        val date = parameters[WidgetStateKeys.DATE_PARAMETER]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (date == null) {
            debugWidgetLog("event=$eventId action=date-select rejected=invalid-date")
            return
        }
        persistWidgetInteraction(context, glanceId, eventId, "date-select:$date") { prefs ->
            prefs[WidgetStateKeys.DISPLAY_MONTH] = YearMonth.from(date).toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = date.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = false
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = LocalDate.now().toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("event=$eventId action=date-select update-returned id=$glanceId")
    }
}

class QuickAddWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = UUID.randomUUID().toString()
        val date = parameters[WidgetStateKeys.DATE_PARAMETER]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        debugWidgetLog("event=$eventId interaction-received action=quick-add id=$glanceId date=$date params=$parameters")
        val intent = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("daymark://quick-add/$date"))
            .putExtra("quick_add_date", date.toString())
            .putExtra("widget_interaction_id", eventId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        debugWidgetLog("event=$eventId action=quick-add activity-started id=$glanceId date=$date")
    }
}

class GoToWidgetTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = UUID.randomUUID().toString()
        debugWidgetLog("event=$eventId interaction-received action=today id=$glanceId params=$parameters")
        val today = LocalDate.now()
        persistWidgetInteraction(context, glanceId, eventId, "today:$today") { prefs ->
            prefs[WidgetStateKeys.DISPLAY_MONTH] = YearMonth.from(today).toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = today.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = true
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = today.toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("event=$eventId action=today update-returned id=$glanceId")
    }
}

abstract class ChangeWidgetMonthAction(private val delta: Long) : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = UUID.randomUUID().toString()
        debugWidgetLog("event=$eventId interaction-received action=month-nav id=$glanceId delta=$delta params=$parameters")
        persistWidgetInteraction(context, glanceId, eventId, "month-nav:$delta") { prefs ->
            val current = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE])
                .moveMonth(delta)
            prefs[WidgetStateKeys.DISPLAY_MONTH] = current.month.toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = current.selectedDate.toString()
            prefs[WidgetStateKeys.FOLLOWS_TODAY] = false
            prefs[WidgetStateKeys.LAST_SEEN_TODAY] = LocalDate.now().toString()
            prefs[WidgetStateKeys.STATE_FORMAT] = 2
        }
        debugWidgetLog("event=$eventId action=month-nav update-returned id=$glanceId")
    }
}

class PreviousWidgetMonthAction : ChangeWidgetMonthAction(-1)
class NextWidgetMonthAction : ChangeWidgetMonthAction(1)

class CompleteWidgetTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = UUID.randomUUID().toString()
        val started = SystemClock.elapsedRealtime()
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val selectedBefore = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE]).selectedDate
        val taskId = parameters[WidgetStateKeys.TASK_PARAMETER]
        val completed = parameters[WidgetStateKeys.COMPLETION_PARAMETER]
        debugWidgetLog("event=$eventId interaction-received action=complete id=$glanceId params=$parameters selectedBefore=$selectedBefore")
        if (taskId == null || completed == null) {
            debugWidgetLog("event=$eventId action=complete rejected=missing-parameters")
            return
        }
        val app = context.applicationContext as DaymarkApp
        val queryStarted = SystemClock.elapsedRealtime()
        val task = app.repository.complete(taskId, completed)
        if (task == null) {
            debugWidgetLog("event=$eventId action=complete task-not-found taskId=$taskId")
            return
        }
        debugWidgetLog("event=$eventId action=complete database-updated id=$glanceId taskId=$taskId completed=$completed selectedAfter=$selectedBefore elapsedMs=${SystemClock.elapsedRealtime() - queryStarted}")
        if (completed) app.reminderScheduler.cancel(taskId)
        else if (app.settings.settings.first().notificationsEnabled && task.reminderEnabled) app.reminderScheduler.schedule(taskId, task.title, task.reminderAt)
        val updateStarted = SystemClock.elapsedRealtime()
        debugWidgetLog("event=$eventId action=complete widget-update-requested id=$glanceId")
        WidgetRefresh.update(context)
        debugWidgetLog("event=$eventId action=complete widget-update-returned id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - updateStarted}; launcher render is not measured")
        debugWidgetLog("event=$eventId action=complete callback-finished id=$glanceId elapsedMs=${SystemClock.elapsedRealtime() - started}")
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthWidget() }
object WidgetRefresh {
    suspend fun update(context: Context) {
        WidgetCalendarDataCache.invalidate()
        MonthWidget().updateAll(context)
    }
}
