package com.daymark.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.daymark.app.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val Context.daymarkPreferences by preferencesDataStore(name = "daymark_settings")

class SettingsRepository private constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.daymarkPreferences)
    val settings: Flow<DaymarkSettings> = store.data.map(::decode).catch { emit(DaymarkSettings()) }.distinctUntilChanged()
    val theme: Flow<ThemeMode> = settings.map { it.theme }.distinctUntilChanged()

    suspend fun setTheme(mode: ThemeMode) {
        update { it.copy(theme = mode) }
    }

    suspend fun update(transform: (DaymarkSettings) -> DaymarkSettings) {
        store.edit { prefs ->
            val next = transform(decode(prefs))
            prefs[THEME_KEY] = next.theme.name
            prefs[REDUCE_MOTION_KEY] = next.reduceMotion
            prefs[HAPTICS_KEY] = next.hapticsEnabled
            prefs[WEEK_START_KEY] = next.weekStart.name
            prefs[SHOW_WEEKENDS_KEY] = next.showWeekends
            prefs[CALENDAR_VIEW_KEY] = next.defaultCalendarView.name
            prefs[TIME_24H_KEY] = next.use24HourTime
            prefs[DATE_FORMAT_KEY] = next.dateFormat.name
            prefs[NOTIFICATIONS_KEY] = next.notificationsEnabled
            prefs[REMINDER_MINUTES_KEY] = next.defaultReminderMinutes.coerceAtLeast(0)
        }
    }

    suspend fun reset() { store.edit { it.clear() } }

    companion object TestFactory {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val REDUCE_MOTION_KEY = booleanPreferencesKey("reduce_motion")
        val HAPTICS_KEY = booleanPreferencesKey("haptics_enabled")
        val WEEK_START_KEY = stringPreferencesKey("week_start")
        val SHOW_WEEKENDS_KEY = booleanPreferencesKey("show_weekends")
        val CALENDAR_VIEW_KEY = stringPreferencesKey("calendar_view")
        val TIME_24H_KEY = booleanPreferencesKey("time_24_hour")
        val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        val REMINDER_MINUTES_KEY = intPreferencesKey("default_reminder_minutes")

        fun decode(prefs: androidx.datastore.preferences.core.Preferences) = DaymarkSettings(
            theme = prefs[THEME_KEY].enumOr(ThemeMode.SYSTEM),
            reduceMotion = prefs[REDUCE_MOTION_KEY] ?: false,
            hapticsEnabled = prefs[HAPTICS_KEY] ?: true,
            weekStart = prefs[WEEK_START_KEY].enumOr(WeekStart.SUNDAY),
            showWeekends = prefs[SHOW_WEEKENDS_KEY] ?: true,
            defaultCalendarView = prefs[CALENDAR_VIEW_KEY].enumOr(CalendarViewMode.MONTH),
            use24HourTime = prefs[TIME_24H_KEY] ?: false,
            dateFormat = prefs[DATE_FORMAT_KEY].enumOr(DateFormatMode.WEEKDAY_MONTH_DAY),
            notificationsEnabled = prefs[NOTIFICATIONS_KEY] ?: true,
            defaultReminderMinutes = prefs[REMINDER_MINUTES_KEY] ?: 15
        )

        inline fun <reified T : Enum<T>> String?.enumOr(default: T): T = this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
        internal fun forTesting(store: DataStore<Preferences>) = SettingsRepository(store)
    }
}
