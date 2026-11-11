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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.BarMetrics
import io.github.snow34.calendarwidget.core.EventColoring
import io.github.snow34.calendarwidget.core.GoogleCalendarLinks
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.data.CalendarRepository
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.core.groupByDay
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** One request to show a day. Identity equality on purpose: tapping the same day twice must still re-trigger. */
class DayFocus(val day: LocalDate)

private enum class Tab(val label: String) { Agenda("Agenda"), Month("Month"), Calendars("Calendars"), Settings("Settings") }

@Composable
fun CalendarApp(store: SecureStore, focus: DayFocus?, appearance: Appearance, onAppearanceChange: (Appearance) -> Unit) {
    val context = LocalContext.current
    val repo = remember { CalendarRepository(store) }
    var tab by remember { mutableStateOf(if (store.hasCalendars()) Tab.Agenda else Tab.Calendars) }
    var refresh by remember { mutableIntStateOf(0) }
    // The calendar list and the coloring rule are re-read after every change made in Settings.
    val cal = remember(refresh, appearance) {
        store.loadCalendars().let { CalendarContext(it, EventColoring.from(it, store.colorMode), appearance.palette) }
    }
    // Adding and editing happen in Google Calendar's website; the app itself stays read-only.
    val addEvent: (LocalDate) -> Unit = { day -> openInBrowser(context, GoogleCalendarLinks.addEventUrl(day)) }
    val openEvent: (Occurrence) -> Unit = { o ->
        val calendarId = cal.sources.firstOrNull { it.id == o.calendarId }?.let { GoogleCalendarLinks.calendarIdFromFeedUrl(it.url) }
        openInBrowser(context, GoogleCalendarLinks.eventUrl(o, calendarId, ZoneId.systemDefault()))
    }
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
                Tab.Agenda -> AgendaScreen(repo, refresh, cal, addEvent, openEvent, onOpenCalendars = { tab = Tab.Calendars })
                Tab.Month -> MonthScreen(repo, refresh, focus, cal, appearance, addEvent, openEvent)
                Tab.Calendars -> CalendarsScreen(store, context, appearance.palette, onChanged = { refresh++ })
                Tab.Settings -> SettingsScreen(store, context, appearance, onAppearanceChange, onChanged = { refresh++ })
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
private fun AgendaScreen(
    repo: CalendarRepository, refresh: Int, cal: CalendarContext,
    addEvent: (LocalDate) -> Unit, openEvent: (Occurrence) -> Unit, onOpenCalendars: () -> Unit,
) {
    val today = LocalDate.now()
    val items = loadOccurrences(repo, refresh, today, today.plusDays(60))
    val zone = ZoneId.systemDefault()
    val byDay = items.groupBy { it.start.atZone(zone).toLocalDate().coerceAtLeast(today) }.toSortedMap()
    if (byDay.isEmpty()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                cal.sources.isEmpty() -> {
                    Text("No calendars yet")
                    TextButton(onClick = onOpenCalendars) { Text("Add a calendar") }
                }
                cal.sources.none { it.enabled } -> {
                    Text("All calendars are switched off")
                    TextButton(onClick = onOpenCalendars) { Text("Open Calendars") }
                }
                else -> Text("No events in the next 60 days")
            }
            TextButton(onClick = { addEvent(today) }) { Text("+ Add event") }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item(key = "add-event") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { addEvent(today) }) { Text("+ Add event") }
            }
        }
        byDay.forEach { (day, events) ->
            item(key = day.toString()) {
                Text(Format.date(day), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            }
            items(events, key = { "${it.calendarId}-${it.uid}-${it.start}" }) { EventRow(it, cal, openEvent) }
        }
    }
}

@Composable
private fun EventRow(o: Occurrence, cal: CalendarContext, onOpen: (Occurrence) -> Unit) {
    val tint = cal.tint(o)
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = tint.bar, contentColor = tint.text),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(o.summary.ifBlank { "(no title)" }, style = MaterialTheme.typography.bodyLarge)
            Text(Format.timeRange(o), style = MaterialTheme.typography.bodySmall)
            cal.nameOf(o)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            o.location?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            TextButton(
                onClick = { onOpen(o) },
                colors = ButtonDefaults.textButtonColors(contentColor = tint.text),
            ) { Text("Edit in Google Calendar", fontSize = 12.sp) }
        }
    }
}

private val TODAY_BG = Color(0xFFA8C7FA)
private val TODAY_TEXT = Color(0xFF062E6F)

@Composable
private fun MonthScreen(
    repo: CalendarRepository, refresh: Int, focus: DayFocus?, cal: CalendarContext, appearance: Appearance,
    addEvent: (LocalDate) -> Unit, openEvent: (Occurrence) -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(focus?.day ?: LocalDate.now())) }
    var selected by remember { mutableStateOf(focus?.day ?: LocalDate.now()) }
    LaunchedEffect(focus) { if (focus != null) { month = YearMonth.from(focus.day); selected = focus.day } }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val items = loadOccurrences(repo, refresh, month.atDay(1), month.plusMonths(1).atDay(1))
    // An event covers every day it overlaps (multi-day / all-day events).
    val byDay: Map<LocalDate, List<Occurrence>> = groupByDay(items, zone)
    val dayItems = byDay[selected].orEmpty()
    // Sizes follow the user's text size and bar height so text never gets clipped (see BarMetrics).
    val dayNumH = BarMetrics.dayNumberHeightDp(appearance.textScale).dp
    val barH = BarMetrics.barHeightDp(appearance.barHeight, appearance.textScale).dp
    val cellH = BarMetrics.monthCellHeightDp(appearance.barHeight, appearance.textScale).dp

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
                                    Spacer(Modifier.height(cellH))
                                } else {
                                    MonthDayCell(d, d == today, d == selected, byDay[d].orEmpty(), cal, cellH, dayNumH, barH) { selected = d }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "selected-title") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Format.date(selected), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { addEvent(selected) }) { Text("+ Add event") }
            }
        }
        if (dayItems.isEmpty()) item(key = "no-events") { Text("No events", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp)) }
        items(dayItems, key = { "${it.calendarId}-${it.uid}-${it.start}" }) { EventRow(it, cal, openEvent) }
    }
}

/** Day number (today in a light circle) with one colored, titled bar per event, as in the Month widget. */
@Composable
private fun MonthDayCell(
    date: LocalDate, isToday: Boolean, isSelected: Boolean, events: List<Occurrence>, cal: CalendarContext,
    cellH: Dp, dayNumH: Dp, barH: Dp, onClick: () -> Unit,
) {
    val gap = BarMetrics.GAP_DP.dp
    val slots = BarMetrics.slots(cellH.value, dayNumH.value, barH.value)
    val overflow = events.size > slots
    // When there is not room for everything, the last slot becomes a "+N" line.
    val shown = if (overflow && slots >= 2) slots - 1 else slots.coerceAtMost(events.size)

    Column(
        Modifier.fillMaxWidth().height(cellH).padding(1.dp)
            .let { if (isSelected) it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else it }
            .clickable(onClick = onClick)
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Box(Modifier.fillMaxWidth().height(dayNumH), contentAlignment = Alignment.Center) {
            if (isToday) {
                Box(Modifier.size(dayNumH).background(TODAY_BG, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TODAY_TEXT)
                }
            } else {
                Text("${date.dayOfMonth}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        events.take(shown).forEach { e ->
            val tint = cal.tint(e)
            Box(
                Modifier.fillMaxWidth().height(barH).background(tint.bar, RoundedCornerShape(4.dp)).padding(horizontal = 2.dp),
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
