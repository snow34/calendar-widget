package io.github.snow34.calendarwidget.data

import io.github.snow34.calendarwidget.core.IcsParser
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.core.RecurrenceExpander
import java.time.Instant
import java.time.ZoneId

class CalendarRepository(private val store: SecureStore) {
    /** Occurrences overlapping `[from, to)` from the encrypted cache. CPU-bound; call off the main thread. */
    fun occurrences(from: Instant, to: Instant): List<Occurrence> {
        val text = store.loadIcs() ?: return emptyList()
        val events = IcsParser(ZoneId.systemDefault()).parse(text)
        return RecurrenceExpander.expand(events, from, to)
    }
}
