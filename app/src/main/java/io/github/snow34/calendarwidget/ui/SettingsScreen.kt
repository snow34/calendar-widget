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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.AppearancePresets
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
import kotlin.math.roundToInt

private const val SECTION_CALENDARS = "calendars"
private const val SECTION_APPEARANCE = "appearance"
private const val SECTION_WIDGETS = "widgets"
private const val SECTION_SYNC = "sync"
private const val SECTION_SECURITY = "security"

@Composable
internal fun SettingsScreen(
    store: SecureStore, context: Context, appearance: Appearance, onAppearanceChange: (Appearance) -> Unit, onChanged: () -> Unit,
) {
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
    // Open sections: all closed, except Calendars on a fresh install (the app opens here then and needs a first calendar).
    var openSections by rememberSaveable { mutableStateOf(if (calendars.isEmpty()) setOf(SECTION_CALENDARS) else emptySet()) }
    var renaming by remember { mutableStateOf<CalendarSource?>(null) }
    var removing by remember { mutableStateOf<CalendarSource?>(null) }

    fun toggleSection(id: String) { openSections = if (id in openSections) openSections - id else openSections + id }
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
        // Feedback stays visible at the top even when every section is closed.
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

        item(key = SECTION_CALENDARS) {
            SettingsSection(
                title = "Calendars",
                summary = SettingsSummary.calendars(calendars.size, calendars.count { it.enabled }),
                expanded = SECTION_CALENDARS in openSections,
                onToggle = { toggleSection(SECTION_CALENDARS) },
            ) {
                Text(
                    "Add each calendar by its private iCal address. For a Google calendar: Google Calendar on the web → Settings → " +
                        "pick the calendar → Integrate calendar → \"Secret address in iCal format\". Other iCal or webcal:// feeds work too. " +
                        "Anyone with an address can read that calendar, so addresses are stored encrypted and never shown again. " +
                        "Tap a calendar's color dot to change its color.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (calendars.isEmpty()) Text("No calendars yet. Add one below.", style = MaterialTheme.typography.bodyMedium)
                calendars.forEach { cal ->
                    key(cal.id) {
                        val last = remember(tick, cal.id) { store.lastSyncMillis(cal.id) }
                        CalendarRow(
                            cal = cal,
                            lastSyncMillis = last,
                            palette = appearance.palette,
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
                }
                Text("Add a calendar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        }

        item(key = SECTION_APPEARANCE) {
            SettingsSection(
                title = "Appearance",
                summary = SettingsSummary.appearance(appearance.themeMode, appearance.textScale, AppearancePresets.matching(appearance.palette)?.name),
                expanded = SECTION_APPEARANCE in openSections,
                onToggle = { toggleSection(SECTION_APPEARANCE) },
            ) {
                AppearanceSection(
                    appearance = appearance,
                    colorMode = colorMode,
                    onColorModeChange = { mode ->
                        colorMode = mode; store.colorMode = mode
                        onChanged(); refreshWidgets()
                    },
                ) { updated ->
                    store.appearance = updated
                    onAppearanceChange(updated)
                    onChanged()
                    refreshWidgets()
                }
            }
        }

        item(key = SECTION_WIDGETS) {
            SettingsSection(
                title = "Widgets",
                summary = SettingsSummary.widgets(titles, opacity.roundToInt()),
                expanded = SECTION_WIDGETS in openSections,
                onToggle = { toggleSection(SECTION_WIDGETS) },
            ) {
                ToggleRow("Show event titles on the widget", titles) {
                    titles = it; store.widgetShowTitles = it
                    refreshWidgets()
                }
                Text("Widget background: ${opacity.roundToInt()}%")
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
            }
        }

        item(key = SECTION_SYNC) {
            SettingsSection(
                title = "Sync",
                summary = SettingsSummary.sync(interval),
                expanded = SECTION_SYNC in openSections,
                onToggle = { toggleSection(SECTION_SYNC) },
            ) {
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
        }

        item(key = SECTION_SECURITY) {
            SettingsSection(
                title = "Security",
                summary = SettingsSummary.security(lock),
                expanded = SECTION_SECURITY in openSections,
                onToggle = { toggleSection(SECTION_SECURITY) },
            ) {
                ToggleRow("Require fingerprint / screen lock to open the app", lock) { want ->
                    val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    if (want && !ok) status = "Set up a screen lock on the device first" else { lock = want; store.appLockEnabled = want }
                }
            }
        }

        item(key = "note") {
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
    palette: List<Int>,
    onToggle: (Boolean) -> Unit,
    onNextColor: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val tint = tintAt(cal.colorIndex, palette)
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
