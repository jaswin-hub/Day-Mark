package com.daymark.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.daymark.app.ui.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class SettingsRepositoryTest {
    @Test fun userSettingsPersistAcrossRepositoryInstances() = runBlocking {
        val file = Files.createTempFile("daymark-settings", ".preferences_pb")
        Files.deleteIfExists(file)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file.toFile() })
            val firstRepository = SettingsRepository.forTesting(store)
            firstRepository.update {
                it.copy(
                    theme = ThemeMode.DARK,
                    reduceMotion = true,
                    hapticsEnabled = false,
                    weekStart = WeekStart.MONDAY,
                    showWeekends = false,
                    defaultCalendarView = CalendarViewMode.AGENDA,
                    use24HourTime = true,
                    dateFormat = DateFormatMode.ISO,
                    notificationsEnabled = false,
                    defaultReminderMinutes = 30
                )
            }
            val reopened = SettingsRepository.forTesting(store)
            val restored = reopened.settings.first()
            assertEquals(ThemeMode.DARK, restored.theme)
            assertEquals(true, restored.reduceMotion)
            assertEquals(false, restored.hapticsEnabled)
            assertEquals(WeekStart.MONDAY, restored.weekStart)
            assertEquals(false, restored.showWeekends)
            assertEquals(CalendarViewMode.AGENDA, restored.defaultCalendarView)
            assertEquals(true, restored.use24HourTime)
            assertEquals(DateFormatMode.ISO, restored.dateFormat)
            assertEquals(false, restored.notificationsEnabled)
            assertEquals(30, restored.defaultReminderMinutes)
        } finally {
            scope.cancel()
            Files.deleteIfExists(file)
        }
    }
}
