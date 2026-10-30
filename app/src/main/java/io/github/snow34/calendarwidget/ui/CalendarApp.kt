package io.github.snow34.calendarwidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt
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
    val state = produceState<List<Occurrence>>(emptyList(), repo, refresh, from, to) {
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
    val tint = eventTint(o.summary)
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = tint.bar, contentColor = tint.text),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(o.summary.ifBlank { "(no title)" }, style = MaterialTheme.typography.bodyLarge)
            Text(Format.timeRange(o), style = MaterialTheme.typography.bodySmall)
            o.location?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private val DAY_NUM_H = 18.dp
private val BAR_H = 14.dp
private val BAR_GAP = 2.dp
private val CELL_H = 68.dp
private val TODAY_BG = Color(0xFFA8C7FA)
private val TODAY_TEXT = Color(0xFF062E6F)

@Composable
private fun MonthScreen(repo: CalendarRepository, refresh: Int, focus: DayFocus?) {
    var month by remember { mutableStateOf(YearMonth.from(focus?.day ?: LocalDate.now())) }
    var selected by remember { mutableStateOf(focus?.day ?: LocalDate.now()) }
    LaunchedEffect(focus) { if (focus != null) { month = YearMonth.from(focus.day); selected = focus.day } }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val items = loadOccurrences(repo, refresh, month.atDay(1), month.plusMonths(1).atDay(1))
    // An event covers every day it overlaps (multi-day / all-day events).
    val byDay: Map<LocalDate, List<Occurrence>> = groupByDay(items, zone)
    val dayItems = byDay[selected].orEmpty()

    // One scrolling list: the grid is taller now, so the selected day's events sit below it.
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item(key = "grid") {
            Column {
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
                            Box(Modifier.weight(1f)) {
                                if (d == null) {
                                    Spacer(Modifier.height(CELL_H))
                                } else {
                                    MonthDayCell(d, d == today, d == selected, byDay[d].orEmpty()) { selected = d }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "selected-title") {
            Text(Format.date(selected), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp))
        }
        if (dayItems.isEmpty()) item(key = "no-events") { Text("No events", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp)) }
        items(dayItems, key = { "${it.uid}-${it.start}" }) { EventRow(it) }
    }
}

/** Day number (today in a light circle) with one colored, titled bar per event, as in the Month widget. */
@Composable
private fun MonthDayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, events: List<Occurrence>, onClick: () -> Unit) {
    val slots = ((CELL_H - DAY_NUM_H + BAR_GAP) / (BAR_H + BAR_GAP)).toInt()
    val overflow = events.size > slots
    // When there is not room for everything, the last slot becomes a "+N" line.
    val shown = if (overflow && slots >= 2) slots - 1 else slots.coerceAtMost(events.size)

    Column(
        Modifier.fillMaxWidth().height(CELL_H).padding(1.dp)
            .let { if (isSelected) it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else it }
            .clickable(onClick = onClick)
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(BAR_GAP),
    ) {
        Box(Modifier.fillMaxWidth().height(DAY_NUM_H), contentAlignment = Alignment.Center) {
            if (isToday) {
                Box(Modifier.size(DAY_NUM_H).background(TODAY_BG, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TODAY_TEXT)
                }
            } else {
                Text("${date.dayOfMonth}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        events.take(shown).forEach { e ->
            val tint = eventTint(e.summary)
            Box(
                Modifier.fillMaxWidth().height(BAR_H).background(tint.bar, RoundedCornerShape(4.dp)).padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    e.summary.ifBlank { "(no title)" },
                    fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tint.text,
                )
            }
        }
        if (overflow && slots >= 2) {
            Text(
                "+${events.size - shown}", fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
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
    var opacity by remember { mutableFloatStateOf(store.widgetOpacityPercent.toFloat()) }
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
            Text("Widget background: ${opacity.roundToInt()}%", modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0f..100f,
                steps = 19, // 5% steps
                // Save and redraw once when the finger lifts, not on every tick of the drag.
                onValueChangeFinished = {
                    store.widgetOpacityPercent = opacity.roundToInt()
                    scope.launch { Widgets.updateAll(context) }
                },
            )
            Text("0% is see-through, 100% is solid. Event bars and text always stay solid.", style = MaterialTheme.typography.bodySmall)
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
