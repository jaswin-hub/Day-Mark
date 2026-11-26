package com.daymark.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskPriority
import com.daymark.app.data.DateFormatMode
import com.daymark.app.ui.LocalDaymarkPalette
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    existing: TaskEntity?,
    initialDate: LocalDate?,
    defaultReminderMinutes: Int,
    use24HourTime: Boolean,
    dateFormat: DateFormatMode,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit
) {
    val palette = LocalDaymarkPalette.current
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var plannedDate by remember(existing?.id) { mutableStateOf(existing?.plannedDate ?: if (existing == null) initialDate else null) }
    var deadlineDate by remember(existing?.id) { mutableStateOf(existing?.deadlineDate) }
    var deadlineTime by remember(existing?.id) { mutableStateOf(existing?.deadlineTime) }
    var priority by remember(existing?.id) { mutableStateOf(existing?.priority ?: TaskPriority.MEDIUM) }
    var category by remember(existing?.id) { mutableStateOf(existing?.category.orEmpty()) }
    var estimate by remember(existing?.id) { mutableStateOf(existing?.estimatedDurationMinutes?.toString().orEmpty()) }
    var reminder by remember(existing?.id) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var datePickerFor by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 14.dp),
            color = palette.background,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 15.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(if (existing == null) "New task" else "Edit task", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Plan date and deadline separately.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(title, { title = it; titleError = false }, Modifier.fillMaxWidth(), label = { Text("Task title") }, isError = titleError, supportingText = { if (titleError) Text("A title is required") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2, maxLines = 4)
                    Text("PRIORITY", color = palette.textMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { value ->
                            FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) })
                        }
                    }
                    OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                    OutlinedTextField(estimate, { estimate = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Estimated minutes") }, singleLine = true)
                    DateField("Planned date", plannedDate, dateFormat, onSet = { datePickerFor = "planned" }, onClear = { plannedDate = null })
                    DateField("Deadline", deadlineDate, dateFormat, onSet = { datePickerFor = "deadline" }, onClear = { deadlineDate = null; deadlineTime = null })
                    if (deadlineDate != null) {
                        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            val timeFormat = if (use24HourTime) "HH:mm" else "h:mm a"
                            Text("Deadline time · ${deadlineTime?.format(DateTimeFormatter.ofPattern(timeFormat)) ?: "Not set"}")
                        }
                        if (deadlineTime != null) TextButton(onClick = { deadlineTime = null }) { Text("Clear deadline time") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Reminder", color = palette.textPrimary, fontWeight = FontWeight.Medium)
                            Text("Remind me on the deadline or planned date.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = reminder, onCheckedChange = { reminder = it })
                    }
                }
                HorizontalDivider(color = palette.border)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (existing != null) TextButton(onClick = { onDelete(existing) }) { Text("Delete", color = palette.missed) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (title.isBlank()) { titleError = true; return@Button }
                        val reminderDate = deadlineDate ?: plannedDate
                        val reminderAt = if (reminder && reminderDate != null) {
                            reminderDate.atTime(deadlineTime ?: LocalTime.of(9, 0)).minusMinutes(defaultReminderMinutes.toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else null
                        onSave((existing ?: TaskEntity(title = title.trim())).copy(
                            title = title.trim(), description = description.trim().ifBlank { null },
                            plannedDate = plannedDate, deadlineDate = deadlineDate, deadlineTime = deadlineTime,
                            priority = priority, category = category.trim().ifBlank { null },
                            estimatedDurationMinutes = estimate.toIntOrNull()?.takeIf { it > 0 },
                            reminderEnabled = reminder && reminderAt != null, reminderAt = reminderAt
                        ))
                    }) { Text("Save task") }
                }
            }
        }
    }

    datePickerFor?.let { target ->
        val current = if (target == "planned") plannedDate else deadlineDate
        val picker = rememberDatePickerState(initialSelectedDateMillis = current?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = { TextButton(onClick = {
                val selected = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
                if (target == "planned") plannedDate = selected else deadlineDate = selected
                datePickerFor = null
            }) { Text("Set date") } },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = deadlineTime?.hour ?: 9, initialMinute = deadlineTime?.minute ?: 0)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Deadline time") },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { deadlineTime = LocalTime.of(state.hour, state.minute); showTimePicker = false }) { Text("Set time") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DateField(label: String, date: LocalDate?, format: DateFormatMode, onSet: () -> Unit, onClear: () -> Unit) {
    val palette = LocalDaymarkPalette.current
    Column {
        Text(label.uppercase(), color = palette.textMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSet, modifier = Modifier.weight(1f)) {
                Text(date?.format(DateTimeFormatter.ofPattern(format.pattern())) ?: "Choose date")
            }
            if (date != null) TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

private fun DateFormatMode.pattern() = when (this) {
    DateFormatMode.WEEKDAY_MONTH_DAY -> "EEE, MMM d, yyyy"
    DateFormatMode.MONTH_DAY_YEAR -> "MMM d, yyyy"
    DateFormatMode.DAY_MONTH_YEAR -> "d MMM yyyy"
    DateFormatMode.ISO -> "yyyy-MM-dd"
}
