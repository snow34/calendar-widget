package io.github.snow34.calendarwidget.core

import java.time.Instant

/**
 * An event the user just added in the app. Google refreshes the private iCal feed slowly (hours), so the app shows it
 * from here until it appears in that calendar's feed, or until [PendingEvents.MAX_AGE_MILLIS] passes.
 */
data class PendingEvent(
    /** [CalendarSource.id] of the calendar it was added to. */
    val calendarId: String,
    /** Google's iCal UID for the new event (`<id>@google.com`), the same UID the feed will later contain. */
    val uid: String,
    val title: String,
    val location: String?,
    val allDay: Boolean,
    val start: Instant,
    val end: Instant,
    val createdAtMillis: Long,
)

object PendingEvents {
    /** After this, a pending event is dropped even if the feed never showed it (e.g. it was deleted in Google). */
    const val MAX_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000

    fun toOccurrence(p: PendingEvent) = Occurrence(
        uid = p.uid, summary = p.title, location = p.location, allDay = p.allDay,
        start = p.start, end = p.end, recurring = false, calendarId = p.calendarId,
    )

    /**
     * [occurrences] from the feeds plus pending events of [enabledCalendarIds] that overlap `[from, to)` and are not in the
     * feed yet, sorted by start then title.
     */
    fun overlay(
        occurrences: List<Occurrence>,
        pending: List<PendingEvent>,
        enabledCalendarIds: Set<String>,
        from: Instant,
        to: Instant,
    ): List<Occurrence> {
        if (pending.isEmpty()) return occurrences
        val inFeed = occurrences.map { it.calendarId to it.uid }.toSet()
        val extra = pending.asSequence()
            .filter { it.calendarId in enabledCalendarIds && (it.calendarId to it.uid) !in inFeed }
            .filter { if (it.start == it.end) it.start >= from && it.start < to else it.start < to && it.end > from }
            .map(::toOccurrence)
            .toList()
        if (extra.isEmpty()) return occurrences
        return (occurrences + extra).sortedWith(compareBy({ it.start }, { it.summary }))
    }

    /**
     * Keeps only pending events that are still needed: not yet in their calendar's feed ([feedUids] = UIDs per calendar id),
     * not older than [MAX_AGE_MILLIS], and whose calendar still exists (a key in [feedUids]).
     */
    fun prune(pending: List<PendingEvent>, feedUids: Map<String, Set<String>>, nowMillis: Long): List<PendingEvent> =
        pending.filter { p ->
            val uids = feedUids[p.calendarId] ?: return@filter false
            p.uid !in uids && nowMillis - p.createdAtMillis < MAX_AGE_MILLIS
        }
}
