package com.daymark.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.daymark.app.DaymarkApp
import com.daymark.app.data.DaymarkSettings
import com.daymark.app.ui.DaymarkTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class WidgetConfigurationActivity : ComponentActivity() {
    private val appWidgetId: Int by lazy {
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        var settings by mutableStateOf(DaymarkSettings())
        var displayMode by mutableStateOf(WidgetDisplayMode.AUTO)
        var showCompleted by mutableStateOf(false)
        var showAgenda by mutableStateOf(true)
        var showCounts by mutableStateOf(true)
        var loaded by mutableStateOf(false)
        lifecycleScope.launch {
            val app = application as DaymarkApp
            settings = app.settings.settings.first()
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            val state = getAppWidgetState<Preferences>(this@WidgetConfigurationActivity, PreferencesGlanceStateDefinition, glanceId)
            displayMode = state[WidgetStateKeys.DISPLAY_MODE]?.let { runCatching { WidgetDisplayMode.valueOf(it) }.getOrNull() } ?: WidgetDisplayMode.AUTO
            showCompleted = state[WidgetStateKeys.SHOW_COMPLETED] ?: false
            showAgenda = state[WidgetStateKeys.SHOW_AGENDA] ?: true
            showCounts = state[WidgetStateKeys.SHOW_COUNTS] ?: true
            loaded = true
        }
        setContent {
            DaymarkTheme(settings) {
                WidgetConfigurationContent(
                    displayMode = displayMode,
                    onDisplayMode = { displayMode = it },
                    showCompleted = showCompleted,
                    onShowCompleted = { showCompleted = it },
                    showAgenda = showAgenda,
                    onShowAgenda = { showAgenda = it },
                    showCounts = showCounts,
                    onShowCounts = { showCounts = it },
                    loaded = loaded,
                    onSave = {
                        lifecycleScope.launch {
                            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(this@WidgetConfigurationActivity, glanceId) { state ->
                                state[WidgetStateKeys.DISPLAY_MODE] = displayMode.name
                                state[WidgetStateKeys.SHOW_COMPLETED] = showCompleted
                                state[WidgetStateKeys.SHOW_AGENDA] = showAgenda
                                state[WidgetStateKeys.SHOW_COUNTS] = showCounts
                            }
                            MonthWidget().update(this@WidgetConfigurationActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigurationContent(
    displayMode: WidgetDisplayMode,
    onDisplayMode: (WidgetDisplayMode) -> Unit,
    showCompleted: Boolean,
    onShowCompleted: (Boolean) -> Unit,
    showAgenda: Boolean,
    onShowAgenda: (Boolean) -> Unit,
    showCounts: Boolean,
    onShowCounts: (Boolean) -> Unit,
    loaded: Boolean,
    onSave: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Daymark widget", style = MaterialTheme.typography.headlineSmall)
            Text("Choose how this widget uses its available space.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            WidgetDisplayMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = displayMode == mode, onClick = { onDisplayMode(mode) })
                    Text(mode.name.lowercase().replaceFirstChar(Char::uppercase))
                }
            }
            ConfigurationSwitch("Show completed tasks", showCompleted, onShowCompleted)
            ConfigurationSwitch("Show selected-day agenda", showAgenda, onShowAgenda)
            ConfigurationSwitch("Show task counts", showCounts, onShowCounts)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSave, enabled = loaded, modifier = Modifier.fillMaxWidth()) {
                Text("Save widget")
            }
        }
    }
}

@Composable
private fun ConfigurationSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
