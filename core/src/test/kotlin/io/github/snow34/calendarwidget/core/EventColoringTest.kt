package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class EventColoringTest {
    private val sources = listOf(
        CalendarSource("work", "Work", "https://e.com/w.ics", colorIndex = 5),
        CalendarSource("home", "Home", "https://e.com/h.ics", colorIndex = 2),
    )

    private fun occ(title: String, calendarId: String) = Occurrence(
        uid = "u", summary = title, location = null, allDay = false,
        start = Instant.parse("2026-01-01T10:00:00Z"), end = Instant.parse("2026-01-01T11:00:00Z"), calendarId = calendarId,
    )

    @Test fun calendarModeUsesTheCalendarsColor() {
        val coloring = EventColoring.from(sources, ColorMode.CALENDAR)
        assertEquals(5, coloring.indexFor(occ("anything", "work")))
        assertEquals(5, coloring.indexFor(occ("something else", "work")))
        assertEquals(2, coloring.indexFor(occ("anything", "home")))
    }

    @Test fun titleModeIgnoresTheCalendar() {
        val coloring = EventColoring.from(sources, ColorMode.TITLE)
        assertEquals(EventColors.indexFor("Lunch"), coloring.indexFor(occ("Lunch", "work")))
        assertEquals(EventColors.indexFor("Lunch"), coloring.indexFor(occ("Lunch", "home")))
    }

    @Test fun unknownCalendarFallsBackToTitleColor() {
        val coloring = EventColoring.from(sources, ColorMode.CALENDAR)
        assertEquals(EventColors.indexFor("Lunch"), coloring.indexFor(occ("Lunch", "deleted-calendar")))
    }

    @Test fun outOfRangeCalendarColorStaysInsidePalette() {
        val coloring = EventColoring.from(listOf(CalendarSource("x", "X", "u", colorIndex = 19)), ColorMode.CALENDAR)
        assertEquals(19 % EventColors.PALETTE_SIZE, coloring.indexFor(occ("t", "x")))
        val negative = EventColoring.from(listOf(CalendarSource("x", "X", "u", colorIndex = -1)), ColorMode.CALENDAR)
        assertEquals(EventColors.PALETTE_SIZE - 1, negative.indexFor(occ("t", "x")))
    }

    @Test fun parseModeNames() {
        assertEquals(ColorMode.CALENDAR, ColorMode.parse("CALENDAR"))
        assertEquals(ColorMode.TITLE, ColorMode.parse("TITLE"))
        assertNull(ColorMode.parse("nope"))
        assertNull(ColorMode.parse(null))
    }
}
