package io.github.snow34.calendarwidget.core

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Minimal RFC 5545 reader covering what calendar feeds (Google Calendar in particular) emit.
 * Input is untrusted: malformed events are skipped, never thrown, and sizes are capped.
 */
class IcsParser(
    private val defaultZone: ZoneId,
    private val maxEvents: Int = 50_000,
) {
    private class Prop(val name: String, val params: Map<String, String>, val value: String)

    fun parse(text: String): List<RawEvent> {
        val events = ArrayList<RawEvent>()
        var current: MutableList<Prop>? = null
        for (line in unfold(text)) {
            val prop = parseProp(line) ?: continue
            when {
                prop.name == "BEGIN" && prop.value.equals("VEVENT", true) -> current = ArrayList()
                prop.name == "END" && prop.value.equals("VEVENT", true) -> {
                    current?.let { props -> toEvent(props)?.let(events::add) }
                    current = null
                    if (events.size >= maxEvents) return events
                }
                else -> current?.add(prop)
            }
        }
        return events
    }

    private fun unfold(text: String): Sequence<String> = sequence {
        val sb = StringBuilder()
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            if ((line.startsWith(" ") || line.startsWith("\t")) && sb.isNotEmpty()) {
                if (sb.length < MAX_LINE) sb.append(line, 1, line.length)
            } else {
                if (sb.isNotEmpty()) yield(sb.toString())
                sb.setLength(0)
                sb.append(line.take(MAX_LINE))
            }
        }
        if (sb.isNotEmpty()) yield(sb.toString())
    }

    private fun parseProp(line: String): Prop? {
        var colon = -1
        var quoted = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') quoted = !quoted
            else if (c == ':' && !quoted) { colon = i; break }
        }
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = HashMap<String, String>()
        for (p in head.drop(1)) {
            val eq = p.indexOf('=')
            if (eq > 0) params[p.substring(0, eq).uppercase()] = p.substring(eq + 1).trim('"')
        }
        return Prop(head[0].uppercase(), params, line.substring(colon + 1))
    }

    private fun toEvent(props: List<Prop>): RawEvent? {
        fun first(name: String) = props.firstOrNull { it.name == name }
        val dtStartProp = first("DTSTART") ?: return null
        val startVal = parseDateTime(dtStartProp) ?: return null
        val allDay = dtStartProp.params["VALUE"].equals("DATE", true) || dtStartProp.value.length == 8
        val start = startVal

        val end = first("DTEND")?.let { parseDateTime(it) }
            ?: first("DURATION")?.let { parseDuration(it.value) }?.let { start.plus(it) }
            ?: if (allDay) start.plusDays(1) else start
        if (end.isBefore(start)) return null

        val uid = first("UID")?.value ?: return null
        val exDates = props.filter { it.name == "EXDATE" }.flatMap { p ->
            p.value.split(',').mapNotNull { v ->
                parseDateTime(Prop(p.name, p.params, v.trim()))?.toInstant()
            }
        }.toSet()

        return RawEvent(
            uid = uid,
            summary = first("SUMMARY")?.value?.let(::unescape).orEmpty(),
            location = first("LOCATION")?.value?.let(::unescape)?.ifBlank { null },
            allDay = allDay,
            start = start,
            end = end,
            rrule = first("RRULE")?.let { parseRrule(it.value) },
            exDates = exDates,
            recurrenceId = first("RECURRENCE-ID")?.let { parseDateTime(it) }?.toInstant(),
            cancelled = first("STATUS")?.value.equals("CANCELLED", true),
        )
    }

    private fun parseDateTime(p: Prop): ZonedDateTime? = try {
        val v = p.value
        when {
            v.length == 8 -> LocalDate.parse(v, DATE).atStartOfDay(defaultZone)
            v.endsWith("Z") -> LocalDateTime.parse(v.dropLast(1), DATETIME).atZone(ZoneOffset.UTC)
            else -> LocalDateTime.parse(v, DATETIME).atZone(zoneOf(p.params["TZID"]))
        }
    } catch (_: DateTimeException) {
        null
    }

    private fun zoneOf(tzid: String?): ZoneId =
        if (tzid == null) defaultZone else try { ZoneId.of(tzid) } catch (_: DateTimeException) { defaultZone }

    private fun parseDuration(v: String): Duration? = try {
        // Calendar durations are day/week/time based; Duration.parse only takes P[n]DT...
        val s = v.trim().uppercase()
        val neg = s.startsWith("-")
        val body = s.removePrefix("-").removePrefix("+")
        val weeks = Regex("""(\d+)W""").find(body)?.groupValues?.get(1)?.toLong() ?: 0
        val norm = if (weeks > 0) "P${weeks * 7}D" else body
        val d = Duration.parse(norm)
        if (neg) d.negated() else d
    } catch (_: DateTimeException) {
        null
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }

    private fun parseRrule(v: String): RecurrenceRule? {
        val m = v.split(';').mapNotNull {
            val eq = it.indexOf('=')
            if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1) else null
        }.toMap()
        val freq = m["FREQ"]?.let { runCatching { Frequency.valueOf(it.uppercase()) }.getOrNull() } ?: return null
        return RecurrenceRule(
            freq = freq,
            interval = (m["INTERVAL"]?.toIntOrNull() ?: 1).coerceAtLeast(1),
            count = m["COUNT"]?.toIntOrNull(),
            until = m["UNTIL"]?.let { u ->
                parseDateTime(Prop("UNTIL", emptyMap(), u))?.let {
                    // A date-only UNTIL is inclusive of that whole day.
                    if (u.length == 8) it.plusDays(1).toInstant().minusMillis(1) else it.toInstant()
                }
            },
            byDay = m["BYDAY"]?.split(',')?.mapNotNull(::parseByDay).orEmpty(),
            byMonthDay = m["BYMONTHDAY"]?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty(),
            byMonth = m["BYMONTH"]?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty(),
            weekStart = m["WKST"]?.let(::dayOf) ?: DayOfWeek.MONDAY,
        )
    }

    private fun parseByDay(s: String): ByDay? {
        val t = s.trim().uppercase()
        if (t.length < 2) return null
        val day = dayOf(t.takeLast(2)) ?: return null
        val ord = t.dropLast(2).takeIf { it.isNotEmpty() }?.toIntOrNull()
        return ByDay(day, ord)
    }

    private fun dayOf(s: String): DayOfWeek? = when (s.uppercase()) {
        "MO" -> DayOfWeek.MONDAY; "TU" -> DayOfWeek.TUESDAY; "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY; "FR" -> DayOfWeek.FRIDAY; "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY; else -> null
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    else -> sb.append(n) // \, \; \\
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    private companion object {
        const val MAX_LINE = 64 * 1024
        val DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        val DATETIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    }
}
