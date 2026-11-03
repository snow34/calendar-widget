package io.github.snow34.calendarwidget.core

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Links into Google Calendar's website. The app itself stays read-only: adding and editing happens in the
 * user's browser, where they are signed in to Google, so no Google sign-in or write access lives in the app.
 */
object GoogleCalendarLinks {
    private const val BASE = "https://calendar.google.com/calendar"
    private const val GOOGLE_UID_SUFFIX = "@google.com"
    private val DATE = DateTimeFormatter.BASIC_ISO_DATE
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE_TIME_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    /** Google's "new event" form on [date], prefilled 09:00-10:00 (everything can be changed there). */
    fun addEventUrl(date: LocalDate): String =
        "$BASE/render?action=TEMPLATE&dates=${DATE_TIME.format(date.atTime(9, 0))}/${DATE_TIME.format(date.atTime(10, 0))}"

    /** Google's day view; the fallback when a single event can't be linked directly. */
    fun dayUrl(date: LocalDate): String = "$BASE/r/day/${date.year}/${date.monthValue}/${date.dayOfMonth}"

    /**
     * The calendar id (normally the account's e-mail address) from a secret feed URL like
     * `https://calendar.google.com/calendar/ical/<id>/private-<token>/basic.ics`, or null if it isn't one.
     */
    fun calendarIdFromFeedUrl(feedUrl: String): String? {
        val uri = try { URI(feedUrl.trim()) } catch (_: URISyntaxException) { return null }
        if (uri.host != "calendar.google.com") return null
        val segments = (uri.rawPath ?: return null).split('/').filter { it.isNotEmpty() }
        val at = segments.indexOf("ical")
        if (at < 0) return null
        val raw = segments.getOrNull(at + 1) ?: return null
        return try {
            URLDecoder.decode(raw, Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Opens one event in Google Calendar. Google builds the link's `eid` from the event id and calendar id; events made in
     * Google Calendar have the iCal UID `<eventId>@google.com`. Anything else (imported events, unknown calendar) falls
     * back to that day's page. Best effort: the link format is not an official API.
     */
    fun eventUrl(occurrence: Occurrence, calendarId: String?, zone: ZoneId): String {
        val day = occurrence.start.atZone(zone).toLocalDate()
        if (calendarId == null || !occurrence.uid.endsWith(GOOGLE_UID_SUFFIX)) return dayUrl(day)
        var id = occurrence.uid.removeSuffix(GOOGLE_UID_SUFFIX)
        if (id.isEmpty()) return dayUrl(day)
        if (occurrence.recurring) {
            // One instance of a series is addressed as <eventId>_<original start>.
            id += "_" + if (occurrence.allDay) DATE.format(day) else DATE_TIME_UTC.format(occurrence.start.atZone(ZoneOffset.UTC))
        }
        val eid = Base64.getUrlEncoder().withoutPadding().encodeToString("$id $calendarId".toByteArray(Charsets.UTF_8))
        return "$BASE/event?eid=$eid"
    }
}
