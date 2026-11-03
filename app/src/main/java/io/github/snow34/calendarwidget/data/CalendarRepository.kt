package io.github.snow34.calendarwidget.data

import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.IcsParser
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.core.RecurrenceExpander
import java.time.Instant
import java.time.ZoneId

class CalendarRepository(private val store: SecureStore) {
    /**
     * Occurrences overlapping `[from, to)` from the encrypted caches of every switched-on calendar, merged and
     * sorted. CPU-bound; call off the main thread. Pass [sources] if you already loaded the list.
     */
    fun occurrences(from: Instant, to: Instant, sources: List<CalendarSource> = store.loadCalendars()): List<Occurrence> {
        val zone = ZoneId.systemDefault()
        return sources.filter { it.enabled }
            .flatMap { cal ->
                val text = store.loadIcs(cal.id) ?: return@flatMap emptyList()
                RecurrenceExpander.expand(IcsParser(zone).parse(text), from, to, cal.id)
            }
            .sortedWith(compareBy({ it.start }, { it.summary }))
    }
}
