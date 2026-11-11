package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayGroupingTest {
    private val zone = ZoneId.of("Europe/Tallinn")

    private fun occ(title: String, start: String, end: String, allDay: Boolean = false) = Occurrence(
        uid = title, summary = title, location = null, allDay = allDay,
        start = Instant.parse(start), end = Instant.parse(end),
    )

    private fun local(date: String) = LocalDate.parse(date)

    @Test fun singleDayEvent() {
        val g = groupByDay(listOf(occ("a", "2025-01-10T08:00:00Z", "2025-01-10T09:00:00Z")), zone)
        assertEquals(setOf(local("2025-01-10")), g.keys)
    }

    @Test fun multiDayEventAppearsOnEveryDay() {
        // 2025-01-10 22:00 local .. 2025-01-12 10:00 local
        val g = groupByDay(listOf(occ("trip", "2025-01-10T20:00:00Z", "2025-01-12T08:00:00Z")), zone)
        assertEquals(listOf("2025-01-10", "2025-01-11", "2025-01-12").map(::local), g.keys.toList())
    }

    @Test fun allDayEventEndingAtMidnightDoesNotSpill() {
        // all-day on the 5th: local midnight 5th (22:00Z on 4th) to local midnight 6th
        val g = groupByDay(listOf(occ("hol", "2025-01-04T22:00:00Z", "2025-01-05T22:00:00Z", allDay = true)), zone)
        assertEquals(setOf(local("2025-01-05")), g.keys)
    }

    @Test fun zeroLengthEventStaysOnItsDay() {
        val g = groupByDay(listOf(occ("ping", "2025-01-10T08:00:00Z", "2025-01-10T08:00:00Z")), zone)
        assertEquals(setOf(local("2025-01-10")), g.keys)
    }

    @Test fun dayListIsSortedAllDayFirstThenStartThenTitle() {
        val late = occ("late", "2025-01-10T15:00:00Z", "2025-01-10T16:00:00Z")
        val early = occ("early", "2025-01-10T07:00:00Z", "2025-01-10T08:00:00Z")
        val holiday = occ("holiday", "2025-01-09T22:00:00Z", "2025-01-10T22:00:00Z", allDay = true)
        val sameStartB = occ("b", "2025-01-10T09:00:00Z", "2025-01-10T10:00:00Z")
        val sameStartA = occ("a", "2025-01-10T09:00:00Z", "2025-01-10T10:00:00Z")
        val day = groupByDay(listOf(late, sameStartB, early, holiday, sameStartA), zone).getValue(local("2025-01-10"))
        assertEquals(listOf("holiday", "early", "a", "b", "late"), day.map { it.summary })
    }

    @Test fun absurdSpanIsCapped() {
        val g = groupByDay(listOf(occ("forever", "2025-01-01T00:00:00Z", "2125-01-01T00:00:00Z")), zone, maxSpanDays = 10)
        assertEquals(11, g.size)
    }
}
