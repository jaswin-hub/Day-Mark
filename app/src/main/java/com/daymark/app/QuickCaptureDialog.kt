package com.daymark.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daymark.app.data.TaskEntity
import com.daymark.app.ui.LocalDaymarkPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureDialog(onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    val palette = LocalDaymarkPalette.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick capture", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task title") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showDatePicker = true }) { Text(date?.toString() ?: "Add date") }
                    if (date != null) TextButton(onClick = { date = null }) { Text("Clear") }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(TaskEntity(title = title.trim(), plannedDate = date)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = palette.surface
    )
    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}
