package io.github.snow34.calendarwidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.data.CalendarRepository
import io.github.snow34.calendarwidget.data.IcsFetcher
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.sync.SyncOutcome
import io.github.snow34.calendarwidget.sync.SyncWorker
import io.github.snow34.calendarwidget.sync.Syncer
import io.github.snow34.calendarwidget.widget.Widgets
import io.github.snow34.calendarwidget.core.groupByDay
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** One request to show a day. Identity equality on purpose: tapping the same day twice must still re-trigger. */
class DayFocus(val day: LocalDate)

private enum class Tab(val label: String) { Agenda("Agenda"), Month("Month"), Settings("Settings") }

@Composable
fun CalendarApp(store: SecureStore, focus: DayFocus? = null) {
    val context = LocalContext.current
    val repo = remember { CalendarRepository(store) }
    var tab by remember { mutableStateOf(if (store.hasUrl()) Tab.Agenda else Tab.Settings) }
    var refresh by remember { mutableIntStateOf(0) }
    // Opened from a day in the month widget: jump to the Month tab.
    LaunchedEffect(focus) { if (focus != null) tab = Tab.Month }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = {}, label = { Text(t.label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Agenda -> AgendaScreen(repo, refresh)
                Tab.Month -> MonthScreen(repo, refresh, focus)
                Tab.Settings -> SettingsScreen(store, context, onChanged = { refresh++ })
            }
        }
    }
}

@Composable
private fun loadOccurrences(repo: CalendarRepository, refresh: Int, from: LocalDate, to: LocalDate): List<Occurrence> {
    val zone = ZoneId.systemDefault()
    val state = produceState(emptyList(), repo, refresh, from, to) {
        value = withContext(Dispatchers.Default) {
            repo.occurrences(from.atStartOfDay(zone).toInstant(), to.atStartOfDay(zone).toInstant())
        }
    }
    return state.value
}

@Composable
private fun AgendaScreen(repo: CalendarRepository, refresh: Int) {
    val today = LocalDate.now()
    val items = loadOccurrences(repo, refresh, today, today.plusDays(60))
    val zone = ZoneId.systemDefault()
    val byDay = items.groupBy { it.start.atZone(zone).toLocalDate().coerceAtLeast(today) }.toSortedMap()
    if (byDay.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No events in the next 60 days") }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        byDay.forEach { (day, events) ->
            item(key = day.toString()) {
                Text(Format.date(day), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            }
            items(events, key = { "${it.uid}-${it.start}" }) { EventRow(it) }
        }
    }
}

@Composable
private fun EventRow(o: Occurrence) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(12.dp)) {
            Text(o.summary.ifBlank { "(no title)" }, style = MaterialTheme.typography.bodyLarge)
            Text(Format.timeRange(o), style = MaterialTheme.typography.bodySmall)
            o.location?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun MonthScreen(repo: CalendarRepository, refresh: Int, focus: DayFocus?) {
    var month by remember { mutableStateOf(YearMonth.from(focus?.day ?: LocalDate.now())) }
    var selected by remember { mutableStateOf(focus?.day ?: LocalDate.now()) }
    LaunchedEffect(focus) { if (focus != null) { month = YearMonth.from(focus.day); selected = focus.day } }
    val zone = ZoneId.systemDefault()
    val items = loadOccurrences(repo, refresh, month.atDay(1), month.plusMonths(1).atDay(1))
    // An event covers every day it overlaps (multi-day / all-day events).
    val byDay: Map<LocalDate, List<Occurrence>> = groupByDay(items, zone)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
            Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
        }
        val lead = month.atDay(1).dayOfWeek.value - 1 // Monday-first grid
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    Box(
                        Modifier.weight(1f).size(48.dp).let { if (d != null) it.clickable { selected = d } else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (d != null) {
                            val has = byDay.containsKey(d)
                            Text(
                                text = if (has) "${d.dayOfMonth}•" else "${d.dayOfMonth}",
                                style = if (d == selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (d == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Text(Format.date(selected), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        val dayItems = byDay[selected].orEmpty()
        if (dayItems.isEmpty()) Text("No events", style = MaterialTheme.typography.bodySmall)
        LazyColumn { items(dayItems, key = { "${it.uid}-${it.start}" }) { EventRow(it) } }
    }
}

@Composable
private fun SettingsScreen(store: SecureStore, context: android.content.Context, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var hasUrl by remember { mutableStateOf(store.hasUrl()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var interval by remember { mutableIntStateOf(store.syncIntervalHours) }
    var titles by remember { mutableStateOf(store.widgetShowTitles) }
    var lock by remember { mutableStateOf(store.appLockEnabled) }

    fun runSync() {
        busy = true
        scope.launch {
            status = when (val o = Syncer.run(context)) {
                SyncOutcome.Updated -> "Synced"
                SyncOutcome.NotModified -> "Already up to date"
                SyncOutcome.NoUrl -> "No feed URL saved"
                is SyncOutcome.Failed -> o.reason
            }
            busy = false
            onChanged()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Google Calendar secret iCal URL", style = MaterialTheme.typography.titleMedium)
            Text(
                "Google Calendar → Settings → your calendar → Integrate calendar → \"Secret address in iCal format\". " +
                    "Anyone with this URL can read the calendar, so it is stored encrypted and never shown again.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(if (hasUrl) "Replace saved URL (hidden)" else "https://calendar.google.com/…/basic.ics") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    enabled = !busy && url.isNotBlank(),
                    onClick = {
                        if (IcsFetcher.parseHttpsUrl(url) == null) {
                            status = "Enter a valid https:// URL"
                        } else {
                            store.saveUrl(url.trim()); store.etag = null
                            url = ""; hasUrl = true
                            SyncWorker.schedule(context, interval)
                            runSync()
                        }
                    },
                ) { Text("Save & sync") }
                TextButton(enabled = !busy && hasUrl, onClick = { runSync() }) { Text("Sync now") }
                TextButton(
                    enabled = hasUrl,
                    onClick = {
                        store.wipe(); SyncWorker.cancel(context); hasUrl = false; status = "URL and cached events deleted"
                        scope.launch { Widgets.updateAll(context) }
                        onChanged()
                    },
                ) { Text("Remove") }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        }
        item {
            Text("Sync every", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 6, 12).forEach { h ->
                    FilterChip(
                        selected = interval == h,
                        onClick = { interval = h; store.syncIntervalHours = h; if (hasUrl) SyncWorker.schedule(context, h) },
                        label = { Text("${h}h") },
                    )
                }
            }
        }
        item {
            ToggleRow("Show event titles on the widget", titles) {
                titles = it; store.widgetShowTitles = it
                scope.launch { Widgets.updateAll(context) }
            }
            ToggleRow("Require fingerprint / screen lock to open the app", lock) { want ->
                val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                if (want && !ok) status = "Set up a screen lock on the device first" else { lock = want; store.appLockEnabled = want }
            }
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
