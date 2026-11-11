package io.github.snow34.calendarwidget.ui

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.EventDraft
import io.github.snow34.calendarwidget.core.EventDrafts
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Form for a new event, saved straight to Google Calendar. [onSave] returns null on success or an error message;
 * the dialog stays open on errors so nothing typed is lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEventDialog(
    initialDate: LocalDate,
    calendars: List<CalendarSource>,
    onDismiss: () -> Unit,
    onSave: suspend (CalendarSource, EventDraft) -> String?,
) {
    val scope = rememberCoroutineScope()
    var draft by remember {
        mutableStateOf(EventDraft(title = "", startDate = initialDate, endDate = initialDate, zone = ZoneId.systemDefault()))
    }
    var calendar by remember { mutableStateOf(calendars.first()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<Picker?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("New event") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.title, onValueChange = { draft = draft.copy(title = it.take(EventDrafts.MAX_TITLE)) },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (calendars.size > 1) {
                    Text("Calendar", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        calendars.forEach { c ->
                            FilterChip(selected = c.id == calendar.id, onClick = { calendar = c }, label = { Text(c.name) })
                        }
                    }
                } else {
                    Text("Calendar: ${calendar.name}", style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("All day", Modifier.weight(1f))
                    Switch(checked = draft.allDay, onCheckedChange = { draft = draft.copy(allDay = it) })
                }
                DateTimeRow(
                    label = if (draft.allDay) "First day" else "Starts",
                    date = draft.startDate, time = draft.startTime, showTime = !draft.allDay,
                    onPickDate = { picking = Picker.StartDate }, onPickTime = { picking = Picker.StartTime },
                )
                DateTimeRow(
                    label = if (draft.allDay) "Last day" else "Ends",
                    date = draft.endDate, time = draft.endTime, showTime = !draft.allDay,
                    onPickDate = { picking = Picker.EndDate }, onPickTime = { picking = Picker.EndTime },
                )
                OutlinedTextField(
                    value = draft.location, onValueChange = { draft = draft.copy(location = it.take(EventDrafts.MAX_TEXT)) },
                    label = { Text("Place (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.notes, onValueChange = { draft = draft.copy(notes = it.take(EventDrafts.MAX_TEXT)) },
                    label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
                (error ?: EventDrafts.validate(draft)?.takeIf { draft.title.isNotEmpty() })?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && EventDrafts.validate(draft) == null,
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        val problem = onSave(calendar, draft)
                        saving = false
                        if (problem == null) onDismiss() else error = problem
                    }
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )

    when (val p = picking) {
        Picker.StartDate, Picker.EndDate -> DatePick(
            initial = if (p == Picker.StartDate) draft.startDate else draft.endDate,
            onDismiss = { picking = null },
        ) { picked ->
            draft = if (p == Picker.StartDate) {
                // Moving the start keeps the length of the event.
                val shift = ChronoUnit.DAYS.between(draft.startDate, picked)
                draft.copy(startDate = picked, endDate = draft.endDate.plusDays(shift))
            } else {
                draft.copy(endDate = picked)
            }
            picking = null
        }
        Picker.StartTime, Picker.EndTime -> TimePick(
            initial = if (p == Picker.StartTime) draft.startTime else draft.endTime,
            onDismiss = { picking = null },
        ) { picked ->
            draft = if (p == Picker.StartTime) {
                val length = java.time.Duration.between(draft.startDate.atTime(draft.startTime), draft.endDate.atTime(draft.endTime))
                val newEnd = draft.startDate.atTime(picked).plus(length)
                draft.copy(startTime = picked, endDate = newEnd.toLocalDate(), endTime = newEnd.toLocalTime())
            } else {
                draft.copy(endTime = picked)
            }
            picking = null
        }
        null -> Unit
    }
}

private enum class Picker { StartDate, StartTime, EndDate, EndTime }

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun DateTimeRow(
    label: String, date: LocalDate, time: LocalTime, showTime: Boolean,
    onPickDate: () -> Unit, onPickTime: () -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickDate) { Text(DATE_FORMAT.format(date)) }
            if (showTime) OutlinedButton(onClick = onPickTime) { Text(TIME_FORMAT.format(time)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePick(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    // The date picker works in UTC milliseconds.
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePick(initial: LocalTime, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initial.hour, initial.minute, AndroidDateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
