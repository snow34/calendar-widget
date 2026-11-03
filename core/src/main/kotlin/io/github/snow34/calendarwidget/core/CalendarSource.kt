package io.github.snow34.calendarwidget.core

import java.net.URI
import java.net.URISyntaxException

/** One calendar the user added: a name, its (secret) iCal feed address, a palette color and an on/off switch. */
data class CalendarSource(
    val id: String,
    val name: String,
    val url: String,
    /** Index into [EventColors.palette]. */
    val colorIndex: Int,
    val enabled: Boolean = true,
)

object CalendarSources {
    const val MAX_CALENDARS = 20
    const val MAX_NAME_LENGTH = 40

    /** Users often paste `webcal://` addresses (Apple, others); these are plain https feeds. */
    fun normalizeFeedUrl(input: String): String {
        val t = input.trim()
        val lower = t.lowercase()
        return when {
            lower.startsWith("webcals://") -> "https://" + t.substring("webcals://".length)
            lower.startsWith("webcal://") -> "https://" + t.substring("webcal://".length)
            else -> t
        }
    }

    /** First palette color no calendar uses yet; wraps around once all are taken. */
    fun nextColorIndex(existing: List<CalendarSource>): Int {
        val used = existing.map { Math.floorMod(it.colorIndex, EventColors.palette.size) }.toSet()
        return EventColors.palette.indices.firstOrNull { it !in used } ?: (existing.size % EventColors.palette.size)
    }

    /** A readable default name: the Google calendar id, else the feed's host, else "Calendar". Made unique. */
    fun suggestName(feedUrl: String, existingNames: Collection<String>): String {
        val host = try { URI(feedUrl.trim()).host } catch (_: URISyntaxException) { null }
        val base = (GoogleCalendarLinks.calendarIdFromFeedUrl(feedUrl) ?: host)?.takeIf { it.isNotBlank() } ?: "Calendar"
        return uniqueName(base.take(MAX_NAME_LENGTH), existingNames)
    }

    /** [name] trimmed and limited in length; blank input becomes [fallback]. */
    fun cleanName(name: String, fallback: String): String =
        name.trim().take(MAX_NAME_LENGTH).ifBlank { fallback }

    fun uniqueName(base: String, existingNames: Collection<String>): String {
        val taken = existingNames.map { it.lowercase() }.toSet()
        if (base.lowercase() !in taken) return base
        var n = 2
        while ("$base $n".lowercase() in taken) n++
        return "$base $n"
    }
}
