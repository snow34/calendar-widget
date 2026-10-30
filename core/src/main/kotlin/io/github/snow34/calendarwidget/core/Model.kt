package io.github.snow34.calendarwidget.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime

/** One VEVENT as found in the feed, before recurrence expansion. */
data class RawEvent(
    val uid: String,
    val summary: String,
    val location: String?,
    val allDay: Boolean,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val rrule: RecurrenceRule?,
    val exDates: Set<Instant>,
    /** Set when this VEVENT overrides a single occurrence of the series with the same [uid]. */
    val recurrenceId: Instant?,
    val cancelled: Boolean,
)

enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/** A BYDAY entry; [ordinal] is e.g. 2 for "2TU" or -1 for "-1FR", null for plain "TU". */
data class ByDay(val day: DayOfWeek, val ordinal: Int?)

data class RecurrenceRule(
    val freq: Frequency,
    val interval: Int = 1,
    val count: Int? = null,
    val until: Instant? = null,
    val byDay: List<ByDay> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val byMonth: List<Int> = emptyList(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
)

/** A concrete, displayable occurrence. */
data class Occurrence(
    val uid: String,
    val summary: String,
    val location: String?,
    val allDay: Boolean,
    val start: Instant,
    val end: Instant,
)
