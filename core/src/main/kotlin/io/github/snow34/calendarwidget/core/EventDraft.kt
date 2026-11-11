package io.github.snow34.calendarwidget.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A new event as typed into the app's "Add event" form, before it is sent to Google Calendar. */
data class EventDraft(
    val title: String,
    val location: String = "",
    val notes: String = "",
    val allDay: Boolean = false,
    val startDate: LocalDate,
    val startTime: LocalTime = LocalTime.of(9, 0),
    /** For all-day events: the last day the event covers (inclusive), as people think of it. */
    val endDate: LocalDate = startDate,
    val endTime: LocalTime = LocalTime.of(10, 0),
    val zone: ZoneId,
) {
    fun startInstant(): Instant =
        if (allDay) startDate.atStartOfDay(zone).toInstant() else startDate.atTime(startTime).atZone(zone).toInstant()

    fun endInstant(): Instant =
        if (allDay) endDate.plusDays(1).atStartOfDay(zone).toInstant() else endDate.atTime(endTime).atZone(zone).toInstant()
}

object EventDrafts {
    const val MAX_TITLE = 300
    const val MAX_TEXT = 4000

    /** A short message explaining what is wrong, or null when the draft can be saved. */
    fun validate(d: EventDraft): String? = when {
        d.title.isBlank() -> "Give the event a name"
        d.title.length > MAX_TITLE -> "The name is too long"
        d.location.length > MAX_TEXT || d.notes.length > MAX_TEXT -> "The text is too long"
        d.allDay && d.endDate.isBefore(d.startDate) -> "The last day is before the first day"
        !d.allDay && !d.endInstant().isAfter(d.startInstant()) -> "The end must be after the start"
        else -> null
    }

    /** The JSON body for Google Calendar's `events.insert`. All-day end dates are exclusive in Google's format. */
    fun toGoogleJson(d: EventDraft): String {
        val fields = mutableListOf("\"summary\":" + Json.string(d.title.trim()))
        if (d.location.isNotBlank()) fields += "\"location\":" + Json.string(d.location.trim())
        if (d.notes.isNotBlank()) fields += "\"description\":" + Json.string(d.notes.trim())
        if (d.allDay) {
            fields += "\"start\":{\"date\":\"${d.startDate}\"}"
            fields += "\"end\":{\"date\":\"${d.endDate.plusDays(1)}\"}"
        } else {
            val tz = Json.string(d.zone.id)
            fields += "\"start\":{\"dateTime\":\"${OFFSET.format(d.startDate.atTime(d.startTime).atZone(d.zone))}\",\"timeZone\":$tz}"
            fields += "\"end\":{\"dateTime\":\"${OFFSET.format(d.endDate.atTime(d.endTime).atZone(d.zone))}\",\"timeZone\":$tz}"
        }
        return fields.joinToString(",", "{", "}")
    }

    private val OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
}

/** Minimal JSON string encoding (the core module has no JSON library). */
object Json {
    fun string(s: String): String {
        val sb = StringBuilder(s.length + 2).append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ' || c == '\u2028' || c == '\u2029') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}
