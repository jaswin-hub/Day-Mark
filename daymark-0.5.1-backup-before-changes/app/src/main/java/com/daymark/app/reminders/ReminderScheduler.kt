package com.daymark.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daymark.app.DaymarkApp
import com.daymark.app.MainActivity
import com.daymark.app.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class ReminderScheduler(private val context: Context, private val repository: TaskRepository) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Reminders for your Daymark tasks" })
    }
    fun schedule(taskId: String, title: String, at: Long?) {
        cancel(taskId)
        if (at == null || at <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderReceiver::class.java).setData(Uri.parse("daymark://reminder/$taskId")).putExtra(EXTRA_ID, taskId).putExtra(EXTRA_TITLE, title)
        val pi = PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
    fun cancel(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).setData(Uri.parse("daymark://reminder/$taskId"))
        PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarmManager?.cancel(it); it.cancel() }
    }
    fun rescheduleAll() { CoroutineScope(Dispatchers.IO).launch {
        val app = context.applicationContext as? DaymarkApp
        if (app?.settings?.settings?.first()?.notificationsEnabled == false) return@launch
        repository.reminders().forEach { schedule(it.id, it.title, it.reminderAt) }
    } }
    companion object { const val CHANNEL = "daymark_reminders"; const val EXTRA_ID = "task_id"; const val EXTRA_TITLE = "task_title" }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_DATE_CHANGED) {
            (context.applicationContext as? DaymarkApp)?.reminderScheduler?.rescheduleAll()
            CoroutineScope(Dispatchers.IO).launch { com.daymark.app.widget.WidgetRefresh.update(context) }
            return
        }
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Task reminder"
        val tapIntent = Intent(context, MainActivity::class.java).setData(Uri.parse("daymark://task/$id")).putExtra(ReminderScheduler.EXTRA_ID, id)
        val tap = PendingIntent.getActivity(context, id.hashCode(), tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText("Your task is due today.").setContentIntent(tap).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) } catch (_: SecurityException) { }
    }
}
