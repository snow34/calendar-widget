package io.github.snow34.calendarwidget.core

import java.time.LocalDate
import java.time.ZoneId

/**
 * Places each occurrence on every local day it overlaps, so multi-day and all-day events show on all their days.
 * An event ending exactly at midnight does not spill onto the next day.
 * Within a day: all-day and multi-day events first, then by start time, then title.
 */
fun groupByDay(occurrences: List<Occurrence>, zone: ZoneId, maxSpanDays: Long = 366): Map<LocalDate, List<Occurrence>> {
    val out = LinkedHashMap<LocalDate, MutableList<Occurrence>>()
    for (o in occurrences) {
        val first = o.start.atZone(zone).toLocalDate()
        val lastInclusive = o.end.minusMillis(1).atZone(zone).toLocalDate()
        val last = if (lastInclusive.isBefore(first)) first else minOf(lastInclusive, first.plusDays(maxSpanDays))
        var d = first
        while (!d.isAfter(last)) {
            out.getOrPut(d) { mutableListOf() }.add(o)
            d = d.plusDays(1)
        }
    }
    val spans = { o: Occurrence -> o.allDay || o.end.toEpochMilli() - o.start.toEpochMilli() >= DAY_MS }
    return out.mapValues { (_, list) ->
        list.sortedWith(compareByDescending<Occurrence> { spans(it) }.thenBy { it.start }.thenBy { it.summary })
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000
