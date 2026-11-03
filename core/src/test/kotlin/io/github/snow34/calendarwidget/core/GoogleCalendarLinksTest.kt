package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

class GoogleCalendarLinksTest {
    private val zone = ZoneId.of("Europe/Tallinn")

    private fun occ(uid: String, start: String, allDay: Boolean = false, recurring: Boolean = false) = Occurrence(
        uid = uid, summary = "x", location = null, allDay = allDay,
        start = Instant.parse(start), end = Instant.parse(start).plusSeconds(3600), recurring = recurring,
    )

    private fun decodeEid(url: String): String =
        String(Base64.getUrlDecoder().decode(url.substringAfter("eid=")), Charsets.UTF_8)

    @Test fun addEventPrefillsTheDay() {
        assertEquals(
            "https://calendar.google.com/calendar/render?action=TEMPLATE&dates=20261103T090000/20261103T100000",
            GoogleCalendarLinks.addEventUrl(LocalDate.of(2026, 11, 3)),
        )
    }

    @Test fun dayUrlHasNoLeadingZeros() {
        assertEquals("https://calendar.google.com/calendar/r/day/2026/3/7", GoogleCalendarLinks.dayUrl(LocalDate.of(2026, 3, 7)))
    }

    @Test fun calendarIdIsReadFromFeedUrl() {
        val url = "https://calendar.google.com/calendar/ical/jane.doe%40gmail.com/private-abc123/basic.ics"
        assertEquals("jane.doe@gmail.com", GoogleCalendarLinks.calendarIdFromFeedUrl(url))
    }

    @Test fun nonGoogleOrMalformedFeedUrlsGiveNull() {
        assertNull(GoogleCalendarLinks.calendarIdFromFeedUrl("https://example.com/calendar/ical/a%40b.c/private-x/basic.ics"))
        assertNull(GoogleCalendarLinks.calendarIdFromFeedUrl("https://calendar.google.com/calendar/other"))
        assertNull(GoogleCalendarLinks.calendarIdFromFeedUrl("not a url at all"))
    }

    @Test fun singleEventLinkEncodesEventAndCalendarId() {
        val url = GoogleCalendarLinks.eventUrl(occ("abc123@google.com", "2026-11-03T07:00:00Z"), "jane@gmail.com", zone)
        assertEquals("https://calendar.google.com/calendar/event?eid=", url.substringBefore("eid=") + "eid=")
        assertEquals("abc123 jane@gmail.com", decodeEid(url))
    }

    @Test fun recurringInstanceAddsItsStartTimeInUtc() {
        // 09:00 in Tallinn (UTC+2 in November) is 07:00Z
        val url = GoogleCalendarLinks.eventUrl(occ("abc123@google.com", "2026-11-03T07:00:00Z", recurring = true), "jane@gmail.com", zone)
        assertEquals("abc123_20261103T070000Z jane@gmail.com", decodeEid(url))
    }

    @Test fun recurringAllDayInstanceUsesTheDate() {
        // local midnight 3 Nov in Tallinn = 2 Nov 22:00Z
        val url = GoogleCalendarLinks.eventUrl(occ("abc123@google.com", "2026-11-02T22:00:00Z", allDay = true, recurring = true), "jane@gmail.com", zone)
        assertEquals("abc123_20261103 jane@gmail.com", decodeEid(url))
    }

    @Test fun eidIsUrlSafeWithoutPadding() {
        val url = GoogleCalendarLinks.eventUrl(occ("a?b>c@google.com", "2026-11-03T07:00:00Z"), "jane@gmail.com", zone)
        val eid = url.substringAfter("eid=")
        assertEquals(false, eid.contains('=') || eid.contains('+') || eid.contains('/'))
    }

    @Test fun otherUidsOrMissingCalendarFallBackToDayView() {
        val day = "https://calendar.google.com/calendar/r/day/2026/11/3"
        assertEquals(day, GoogleCalendarLinks.eventUrl(occ("imported-uid-42", "2026-11-03T07:00:00Z"), "jane@gmail.com", zone))
        assertEquals(day, GoogleCalendarLinks.eventUrl(occ("abc123@google.com", "2026-11-03T07:00:00Z"), null, zone))
        assertEquals(day, GoogleCalendarLinks.eventUrl(occ("@google.com", "2026-11-03T07:00:00Z"), "jane@gmail.com", zone))
    }
}
