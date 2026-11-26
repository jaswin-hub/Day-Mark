package com.daymark.app

import android.app.Application
import androidx.room.Room
import com.daymark.app.data.AppDatabase
import com.daymark.app.data.TaskRepository
import com.daymark.app.data.SettingsRepository

class DaymarkApp : Application() {
    lateinit var repository: TaskRepository
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var reminderScheduler: com.daymark.app.reminders.ReminderScheduler
        private set
    lateinit var settings: SettingsRepository
        private set
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "daymark.db").build()
        repository = TaskRepository(database.taskDao())
        settings = SettingsRepository(this)
        reminderScheduler = com.daymark.app.reminders.ReminderScheduler(this, repository)
        reminderScheduler.rescheduleAll()
    }
}
