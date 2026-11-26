package com.daymark.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.daymark.app.DaymarkApp
import com.daymark.app.MainActivity
import com.daymark.app.data.*
import com.daymark.app.ui.DaymarkPalette
import com.daymark.app.ui.DaymarkPalettes
import com.daymark.app.ui.ThemeMode
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private object WidgetStateKeys {
    val DISPLAY_MONTH = stringPreferencesKey("display_month")
    val SELECTED_DATE = stringPreferencesKey("selected_date")
    val DATE_PARAMETER = ActionParameters.Key<String>("selected_date")
}

class MonthWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DaymarkApp
        val tasks = app.database.taskDao().observeActive().first()
        val today = LocalDate.now()
        val occurrences = CalendarLogic.occurrences(tasks, today).groupBy { it.date }
        val mode = app.settings.theme.first()
        val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = when (mode) { ThemeMode.SYSTEM -> systemDark; ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
        val palette = if (dark) DaymarkPalettes.Dark else DaymarkPalettes.Light
        provideContent { WidgetContent(today, occurrences, tasks.associate { it.id to it.title }, palette) }
    }
}

@Composable
private fun WidgetContent(
    today: LocalDate,
    statuses: Map<LocalDate, List<TaskCalendarOccurrence>>,
    taskTitles: Map<String, String>,
    palette: DaymarkPalette
) {
    val prefs = currentState<Preferences>()
    val state = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE], today)
    val monthDates = CalendarLogic.monthDates(state.month)
    Column(GlanceModifier.fillMaxSize().background(ColorProvider(palette.surface)).padding(10.dp)) {
        Row(GlanceModifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                GlanceModifier.clickable(actionRunCallback<PreviousWidgetMonthAction>()).padding(horizontal = 7.dp, vertical = 3.dp),
                style = TextStyle(color = ColorProvider(palette.accent))
            )
            Text(
                state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = TextStyle(color = ColorProvider(palette.textPrimary))
            )
            Spacer(GlanceModifier.defaultWeight())
            val quickAdd = Intent().setClassName("com.daymark.app", MainActivity::class.java.name)
                .setData(Uri.parse("daymark://quick-add/${state.selectedDate}"))
                .putExtra("quick_add_date", state.selectedDate.toString())
            Text(
                "+",
                GlanceModifier.clickable(actionStartActivity(quickAdd)).padding(horizontal = 7.dp, vertical = 3.dp),
                style = TextStyle(color = ColorProvider(palette.accent))
            )
            Text(
                "›",
                GlanceModifier.clickable(actionRunCallback<NextWidgetMonthAction>()).padding(horizontal = 7.dp, vertical = 3.dp),
                style = TextStyle(color = ColorProvider(palette.accent))
            )
        }

        Row(GlanceModifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(day, GlanceModifier.defaultWeight(), style = TextStyle(color = ColorProvider(palette.textMuted)))
            }
        }

        monthDates.chunked(7).forEach { week ->
            Row(GlanceModifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                week.forEach { date ->
                    val selected = date == state.selectedDate
                    val dayStates = statuses[date].orEmpty().map { it.calculatedStatus }.distinct().take(3)
                    val fill = if (selected) palette.selected else Color.Transparent
                    Column(
                        GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 1.dp, vertical = 1.dp)
                            .background(ColorProvider(fill)).cornerRadius(12.dp)
                            .clickable(actionRunCallback<SelectWidgetDateAction>(actionParametersOf(WidgetStateKeys.DATE_PARAMETER to date.toString()))),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = TextStyle(color = ColorProvider(if (selected) palette.accent else palette.textPrimary))
                        )
                        Row(horizontalAlignment = Alignment.CenterHorizontally) {
                            dayStates.forEach { status ->
                                Box(
                                    GlanceModifier.size(3.dp).padding(horizontal = 1.dp)
                                        .background(ColorProvider(statusColor(palette, status))).cornerRadius(2.dp)
                                ) { }
                            }
                        }
                    }
                }
            }
        }

        val selectedTasks = statuses[state.selectedDate].orEmpty().distinctBy { it.taskId }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")) +
                "  ·  ${selectedTasks.size} ${if (selectedTasks.size == 1) "task" else "tasks"}",
            style = TextStyle(color = ColorProvider(palette.textSecondary))
        )
        selectedTasks.take(2).forEach { occurrence ->
            taskTitles[occurrence.taskId]?.let { title -> Text(
                "•  $title",
                GlanceModifier.fillMaxWidth().padding(top = 2.dp),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(palette.textPrimary))
            ) }
        }
        if (selectedTasks.size > 2) Text("+${selectedTasks.size - 2} more",style=TextStyle(color=ColorProvider(palette.textMuted)))
    }
}

private fun statusColor(palette: DaymarkPalette, status: TaskStatus): Color = when (status) {
    TaskStatus.UPCOMING, TaskStatus.PLANNED_TODAY -> palette.upcoming
    TaskStatus.MISSED_PLAN, TaskStatus.OVERDUE -> palette.missed
    TaskStatus.COMPLETED -> palette.completed
    TaskStatus.DUE_TODAY -> palette.dueToday
    TaskStatus.UNSCHEDULED -> palette.neutral
}

class SelectWidgetDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val date = parameters[WidgetStateKeys.DATE_PARAMETER]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.DISPLAY_MONTH] = YearMonth.from(date).toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = date.toString()
        }
        MonthWidget().update(context, glanceId)
    }
}

abstract class ChangeWidgetMonthAction(private val delta: Long) : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = WidgetCalendarState.fromStored(prefs[WidgetStateKeys.DISPLAY_MONTH], prefs[WidgetStateKeys.SELECTED_DATE])
                .moveMonth(delta)
            prefs[WidgetStateKeys.DISPLAY_MONTH] = current.month.toString()
            prefs[WidgetStateKeys.SELECTED_DATE] = current.selectedDate.toString()
        }
        MonthWidget().update(context, glanceId)
    }
}

class PreviousWidgetMonthAction : ChangeWidgetMonthAction(-1)
class NextWidgetMonthAction : ChangeWidgetMonthAction(1)

class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthWidget() }

object WidgetRefresh { suspend fun update(context: Context) { MonthWidget().updateAll(context) } }
