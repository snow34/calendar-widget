package io.github.snow34.calendarwidget.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.CalendarSources
import io.github.snow34.calendarwidget.core.ColorMode
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.data.IcsFetcher
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.sync.SyncWorker
import io.github.snow34.calendarwidget.sync.Syncer
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
internal fun SettingsScreen(store: SecureStore, context: Context, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf(store.loadCalendars()) }
    var tick by remember { mutableIntStateOf(0) } // bumped after every change so rows re-read their sync time
    var newUrl by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var interval by remember { mutableIntStateOf(store.syncIntervalHours) }
    var colorMode by remember { mutableStateOf(store.colorMode) }
    var titles by remember { mutableStateOf(store.widgetShowTitles) }
    var opacity by remember { mutableFloatStateOf(store.widgetOpacityPercent.toFloat()) }
    var lock by remember { mutableStateOf(store.appLockEnabled) }
    var renaming by remember { mutableStateOf<CalendarSource?>(null) }
    var removing by remember { mutableStateOf<CalendarSource?>(null) }

    fun reload() { calendars = store.loadCalendars(); tick++; onChanged() }
    fun refreshWidgets() { scope.launch { Widgets.updateAll(context) } }
    fun runSync() {
        busy = true
        scope.launch {
            status = Syncer.run(context).summary()
            busy = false
            reload()
        }
    }

    renaming?.let { cal ->
        var text by remember(cal.id) { mutableStateOf(cal.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename calendar") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it.take(CalendarSources.MAX_NAME_LENGTH) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { store.renameCalendar(cal.id, text); renaming = null; reload() }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    removing?.let { cal ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${cal.name}?") },
            text = { Text("The calendar itself is not touched. Its address and its events are deleted from this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.removeCalendar(cal.id)
                        if (!store.hasCalendars()) SyncWorker.cancel(context)
                        removing = null
                        status = "Removed ${cal.name}"
                        reload(); refreshWidgets()
                    },
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Calendars", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add each calendar by its private iCal address. For a Google calendar: Google Calendar on the web → Settings → " +
                    "pick the calendar → Integrate calendar → \"Secret address in iCal format\". Other iCal or webcal:// feeds work too. " +
                    "Anyone with an address can read that calendar, so addresses are stored encrypted and never shown again. " +
                    "Tap a calendar's color dot to change its color.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (calendars.isEmpty()) {
            item { Text("No calendars yet. Add one below.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(calendars, key = { it.id }) { cal ->
            val last = remember(tick, cal.id) { store.lastSyncMillis(cal.id) }
            CalendarRow(
                cal = cal,
                lastSyncMillis = last,
                onToggle = { on ->
                    store.updateCalendar(cal.id) { it.copy(enabled = on) }
                    reload(); refreshWidgets()
                    if (on && store.loadIcs(cal.id) == null) runSync()
                },
                onNextColor = {
                    store.updateCalendar(cal.id) { it.copy(colorIndex = (it.colorIndex + 1) % EventColors.palette.size) }
                    reload(); refreshWidgets()
                },
                onRename = { renaming = cal },
                onRemove = { removing = cal },
            )
        }
        item {
            Text("Add a calendar", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                label = { Text("https://… or webcal://… address") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it.take(CalendarSources.MAX_NAME_LENGTH) },
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    enabled = !busy && newUrl.isNotBlank(),
                    onClick = {
                        when {
                            IcsFetcher.parseHttpsUrl(newUrl) == null -> status = "Enter a valid https:// or webcal:// address"
                            store.addCalendar(newName, newUrl) == null ->
                                status = if (calendars.size >= CalendarSources.MAX_CALENDARS) {
                                    "You can add up to ${CalendarSources.MAX_CALENDARS} calendars"
                                } else {
                                    "That calendar is already in the list"
                                }
                            else -> {
                                newUrl = ""; newName = ""
                                SyncWorker.schedule(context, interval)
                                reload()
                                runSync()
                            }
                        }
                    },
                ) { Text("Add & sync") }
                TextButton(enabled = !busy && calendars.any { it.enabled }, onClick = { runSync() }) { Text("Sync now") }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        }
        item {
            Text("Color events by", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = colorMode == ColorMode.CALENDAR,
                    onClick = { colorMode = ColorMode.CALENDAR; store.colorMode = colorMode; onChanged(); refreshWidgets() },
                    label = { Text("Calendar") },
                )
                FilterChip(
                    selected = colorMode == ColorMode.TITLE,
                    onClick = { colorMode = ColorMode.TITLE; store.colorMode = colorMode; onChanged(); refreshWidgets() },
                    label = { Text("Event name") },
                )
            }
            Text(
                "Calendar: every event gets its calendar's color. Event name: the color is picked from the event's name, " +
                    "so the same event always looks the same.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text("Sync every", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 6, 12).forEach { h ->
                    FilterChip(
                        selected = interval == h,
                        onClick = { interval = h; store.syncIntervalHours = h; if (calendars.isNotEmpty()) SyncWorker.schedule(context, h) },
                        label = { Text("${h}h") },
                    )
                }
            }
        }
        item {
            ToggleRow("Show event titles on the widget", titles) {
                titles = it; store.widgetShowTitles = it
                refreshWidgets()
            }
            Text("Widget background: ${opacity.roundToInt()}%", modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0f..100f,
                steps = 19, // 5% steps
                // Save and redraw once when the finger lifts, not on every tick of the drag.
                onValueChangeFinished = {
                    store.widgetOpacityPercent = opacity.roundToInt()
                    refreshWidgets()
                },
            )
            Text("0% is see-through, 100% is solid. Event bars and text always stay solid.", style = MaterialTheme.typography.bodySmall)
            ToggleRow("Require fingerprint / screen lock to open the app", lock) { want ->
                val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                if (want && !ok) status = "Set up a screen lock on the device first" else { lock = want; store.appLockEnabled = want }
            }
        }
        item {
            Text(
                "Adding and editing events opens Google Calendar in your web browser; this app only shows your calendars. " +
                    "Google refreshes the private calendar address slowly, so a change can take a few hours to appear here.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CalendarRow(
    cal: CalendarSource,
    lastSyncMillis: Long,
    onToggle: (Boolean) -> Unit,
    onNextColor: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val tint = tintAt(cal.colorIndex)
    val synced = if (lastSyncMillis > 0) {
        "Synced " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSyncMillis))
    } else {
        "Not synced yet"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).background(tint.bar, CircleShape).clickable(onClick = onNextColor))
            Column(Modifier.weight(1f)) {
                Text(cal.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(synced, style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = onRename) { Text("Rename") }
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
            Switch(checked = cal.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
