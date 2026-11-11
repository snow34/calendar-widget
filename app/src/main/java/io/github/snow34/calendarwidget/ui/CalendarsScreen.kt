package io.github.snow34.calendarwidget.ui

import android.content.Context
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.core.SettingsSummary
import io.github.snow34.calendarwidget.data.IcsFetcher
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.sync.SyncWorker
import io.github.snow34.calendarwidget.sync.Syncer
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The list of the user's calendars, each with an on/off switch, plus adding, renaming and removing calendars.
 * A calendar that is switched off is hidden everywhere: Agenda, Month and both widgets.
 */
@Composable
internal fun CalendarsScreen(store: SecureStore, context: Context, palette: List<Int>, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf(store.loadCalendars()) }
    var tick by remember { mutableIntStateOf(0) } // bumped after every change so rows re-read their sync time
    var newUrl by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
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
        if (status != null) {
            item(key = "status") {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(status.orEmpty(), Modifier.weight(1f).padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { status = null }) { Text("OK") }
                    }
                }
            }
        }

        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your calendars", style = MaterialTheme.typography.titleLarge)
                    Text(
                        SettingsSummary.calendars(calendars.size, calendars.count { it.enabled }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(enabled = !busy && calendars.any { it.enabled }, onClick = { runSync() }) {
                    Text(if (busy) "Syncing…" else "Sync now")
                }
            }
            if (calendars.isNotEmpty()) {
                Text(
                    "Switch a calendar off to hide it everywhere, including the widgets. Tap its color dot to change its color.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (calendars.isEmpty()) {
            item(key = "empty") { Text("No calendars yet. Add your first one below.", style = MaterialTheme.typography.bodyMedium) }
        }

        items(calendars, key = { it.id }) { cal ->
            val last = remember(tick, cal.id) { store.lastSyncMillis(cal.id) }
            CalendarRow(
                cal = cal,
                lastSyncMillis = last,
                palette = palette,
                onToggle = { on ->
                    store.updateCalendar(cal.id) { it.copy(enabled = on) }
                    reload(); refreshWidgets()
                    if (on && store.loadIcs(cal.id) == null) runSync()
                },
                onNextColor = {
                    store.updateCalendar(cal.id) { it.copy(colorIndex = (it.colorIndex + 1) % EventColors.PALETTE_SIZE) }
                    reload(); refreshWidgets()
                },
                onRename = { renaming = cal },
                onRemove = { removing = cal },
            )
        }

        item(key = "add") {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a calendar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Paste the calendar's private iCal address. For a Google calendar: Google Calendar on the web → Settings → " +
                            "pick the calendar → Integrate calendar → \"Secret address in iCal format\". Other iCal or webcal:// feeds work too. " +
                            "Anyone with an address can read that calendar, so addresses are stored encrypted and never shown again.",
                        style = MaterialTheme.typography.bodySmall,
                    )
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
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                                    SyncWorker.schedule(context, store.syncIntervalHours)
                                    reload()
                                    runSync()
                                }
                            }
                        },
                    ) { Text("Add & sync") }
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(
    cal: CalendarSource,
    lastSyncMillis: Long,
    palette: List<Int>,
    onToggle: (Boolean) -> Unit,
    onNextColor: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val tint = tintAt(cal.colorIndex, palette)
    val synced = when {
        !cal.enabled -> "Off: hidden in the app and widgets"
        lastSyncMillis > 0 -> "Synced " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSyncMillis))
        else -> "Not synced yet"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).background(tint.bar, CircleShape).clickable(onClickLabel = "Change color", onClick = onNextColor))
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
