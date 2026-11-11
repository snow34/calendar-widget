package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EventDraftTest {
    private val tallinn = ZoneId.of("Europe/Tallinn")
    private val day = LocalDate.of(2026, 11, 12)
    private fun draft(title: String = "Lunch", allDay: Boolean = false, endDate: LocalDate = day, endTime: LocalTime = LocalTime.of(10, 0)) =
        EventDraft(title = title, allDay = allDay, startDate = day, endDate = endDate, endTime = endTime, zone = tallinn)

    @Test fun timedEventJson() {
        val json = EventDrafts.toGoogleJson(draft().copy(location = " Café ", notes = "Bring \"cake\"\nand forks"))
        assertEquals(
            "{\"summary\":\"Lunch\",\"location\":\"Café\",\"description\":\"Bring \\\"cake\\\"\\nand forks\"," +
                "\"start\":{\"dateTime\":\"2026-11-12T09:00:00+02:00\",\"timeZone\":\"Europe/Tallinn\"}," +
                "\"end\":{\"dateTime\":\"2026-11-12T10:00:00+02:00\",\"timeZone\":\"Europe/Tallinn\"}}",
            json,
        )
    }

    @Test fun allDayEndIsExclusiveInGoogleFormat() {
        val json = EventDrafts.toGoogleJson(draft(allDay = true, endDate = day.plusDays(1)))
        assertEquals("{\"summary\":\"Lunch\",\"start\":{\"date\":\"2026-11-12\"},\"end\":{\"date\":\"2026-11-14\"}}", json)
    }

    @Test fun summerTimeOffset() {
        val json = EventDrafts.toGoogleJson(draft().copy(startDate = LocalDate.of(2026, 7, 1), endDate = LocalDate.of(2026, 7, 1)))
        assertTrue(json, json.contains("2026-07-01T09:00:00+03:00"))
    }

    @Test fun instants() {
        assertEquals(Instant.parse("2026-11-12T07:00:00Z"), draft().startInstant())
        assertEquals(Instant.parse("2026-11-12T08:00:00Z"), draft().endInstant())
        val allDay = draft(allDay = true)
        assertEquals(Instant.parse("2026-11-11T22:00:00Z"), allDay.startInstant())
        assertEquals(Instant.parse("2026-11-12T22:00:00Z"), allDay.endInstant())
    }

    @Test fun validation() {
        assertNull(EventDrafts.validate(draft()))
        assertNotNull(EventDrafts.validate(draft(title = "  ")))
        assertNotNull(EventDrafts.validate(draft(title = "x".repeat(301))))
        assertNotNull(EventDrafts.validate(draft(endTime = LocalTime.of(9, 0))))
        assertNotNull(EventDrafts.validate(draft(endTime = LocalTime.of(8, 0))))
        assertNull(EventDrafts.validate(draft(endDate = day.plusDays(1), endTime = LocalTime.of(8, 0))))
        assertNull(EventDrafts.validate(draft(allDay = true)))
        assertNotNull(EventDrafts.validate(draft(allDay = true, endDate = day.minusDays(1))))
    }

    @Test fun jsonEscaping() {
        assertEquals("\"a\\\\b\"", Json.string("a\\b"))
        assertEquals("\"\\t\\r\\u0001\"", Json.string("\t\r\u0001"))
        assertEquals("\"Läti 😀\"", Json.string("Läti 😀"))
        assertEquals("\"\\u2028\"", Json.string("\u2028"))
    }
}
