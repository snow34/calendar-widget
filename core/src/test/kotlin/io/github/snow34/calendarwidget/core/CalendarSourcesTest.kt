package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSourcesTest {
    private fun src(id: String, color: Int) = CalendarSource(id, id, "https://example.com/$id.ics", color)

    @Test fun webcalAddressesBecomeHttps() {
        assertEquals("https://example.com/a.ics", CalendarSources.normalizeFeedUrl("webcal://example.com/a.ics"))
        assertEquals("https://example.com/a.ics", CalendarSources.normalizeFeedUrl("  WEBCAL://example.com/a.ics "))
        assertEquals("https://example.com/a.ics", CalendarSources.normalizeFeedUrl("webcals://example.com/a.ics"))
        assertEquals("https://example.com/a.ics", CalendarSources.normalizeFeedUrl("https://example.com/a.ics"))
        assertEquals("http://example.com/a.ics", CalendarSources.normalizeFeedUrl("http://example.com/a.ics"))
    }

    @Test fun nextColorIsFirstUnusedThenWraps() {
        assertEquals(0, CalendarSources.nextColorIndex(emptyList()))
        assertEquals(1, CalendarSources.nextColorIndex(listOf(src("a", 0))))
        assertEquals(0, CalendarSources.nextColorIndex(listOf(src("a", 3), src("b", 1))))
        val all = EventColors.palette.indices.map { src("c$it", it) }
        assertEquals(all.size % EventColors.palette.size, CalendarSources.nextColorIndex(all))
        assertEquals(2, CalendarSources.nextColorIndex(listOf(src("a", 0), src("b", 1), src("c", 8)))) // 8 wraps to 0
    }

    @Test fun nameComesFromGoogleIdOrHost() {
        val google = "https://calendar.google.com/calendar/ical/jane%40gmail.com/private-abc/basic.ics"
        assertEquals("jane@gmail.com", CalendarSources.suggestName(google, emptyList()))
        assertEquals("example.org", CalendarSources.suggestName("https://example.org/cal/feed.ics", emptyList()))
        assertEquals("Calendar", CalendarSources.suggestName("nonsense", emptyList()))
    }

    @Test fun suggestedNamesAreUnique() {
        val url = "https://example.org/cal.ics"
        assertEquals("example.org 2", CalendarSources.suggestName(url, listOf("example.org")))
        assertEquals("example.org 3", CalendarSources.suggestName(url, listOf("Example.org", "example.org 2")))
    }

    @Test fun cleanNameTrimsLimitsAndFallsBack() {
        assertEquals("Work", CalendarSources.cleanName("  Work ", "x"))
        assertEquals("x", CalendarSources.cleanName("   ", "x"))
        assertEquals(CalendarSources.MAX_NAME_LENGTH, CalendarSources.cleanName("a".repeat(200), "x").length)
    }
}
