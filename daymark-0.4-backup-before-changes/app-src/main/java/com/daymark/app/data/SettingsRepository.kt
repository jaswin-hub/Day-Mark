package com.daymark.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daymark.app.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.daymarkPreferences by preferencesDataStore(name = "daymark_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.daymarkPreferences
    val theme: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_KEY]?.let { value -> runCatching { ThemeMode.valueOf(value) }.getOrNull() } ?: ThemeMode.SYSTEM
    }.catch { emit(ThemeMode.SYSTEM) }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[THEME_KEY] = mode.name }
    }

    private companion object { val THEME_KEY = stringPreferencesKey("theme_mode") }
}
