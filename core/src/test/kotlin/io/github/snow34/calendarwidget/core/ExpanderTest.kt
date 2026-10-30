package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExpanderTest {
    private val tallinn = ZoneId.of("Europe/Tallinn")
    private val parser = IcsParser(tallinn)

    private fun ics(vararg events: String) =
        "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n" + events.joinToString("") { "BEGIN:VEVENT\r\n$it\r\nEND:VEVENT\r\n" } + "END:VCALENDAR\r\n"

    private fun window(a: String, b: String) =
        LocalDate.parse(a).atStartOfDay(tallinn).toInstant() to LocalDate.parse(b).atStartOfDay(tallinn).toInstant()

    private fun expand(text: String, a: String, b: String): List<Occurrence> {
        val (from, to) = window(a, b)
        return RecurrenceExpander.expand(parser.parse(text), from, to)
    }

    private fun Occurrence.localDate() = start.atZone(tallinn).toLocalDate().toString()
    private fun Occurrence.localTime() = start.atZone(tallinn).toLocalTime().toString()

    @Test fun parsesUtcTimedEventWithEscapesAndFolding() {
        val text = ics(
            "UID:a\r\nDTSTART:20250310T080000Z\r\nDTEND:20250310T090000Z\r\n" +
                "SUMMARY:Lunch\\, with\\nteam \r\n  (folded)\r\nLOCATION:Caf\\; 1",
        )
        val e = parser.parse(text).single()
        assertEquals("Lunch, with\nteam  (folded)", e.summary)
        assertEquals("Caf; 1", e.location)
        assertEquals(Instant.parse("2025-03-10T08:00:00Z"), e.start.toInstant())
        assertTrue(!e.allDay)
    }

    @Test fun allDayEventSpansDefaultsToOneDay() {
        val occ = expand(ics("UID:b\r\nDTSTART;VALUE=DATE:20250601\r\nSUMMARY:Holiday"), "2025-06-01", "2025-06-02")
        assertEquals(1, occ.size)
        assertTrue(occ[0].allDay)
        assertEquals("2025-06-01", occ[0].localDate())
    }

    @Test fun weeklyByDayWithCountAndExdate() {
        val text = ics(
            "UID:c\r\nDTSTART;TZID=Europe/Tallinn:20250106T100000\r\nDTEND;TZID=Europe/Tallinn:20250106T110000\r\n" +
                "RRULE:FREQ=WEEKLY;COUNT=5;BYDAY=MO,WE\r\nEXDATE;TZID=Europe/Tallinn:20250108T100000\r\nSUMMARY:Standup",
        )
        val dates = expand(text, "2025-01-01", "2025-03-01").map { it.localDate() }
        // Mon 6, (Wed 8 excluded), Mon 13, Wed 15, Mon 20 -> COUNT=5 counts the excluded one too
        assertEquals(listOf("2025-01-06", "2025-01-13", "2025-01-15", "2025-01-20"), dates)
    }

    @Test fun dailyKeepsWallClockAcrossDst() {
        // Tallinn switches to summer time on 2025-03-30 (UTC+2 -> UTC+3)
        val text = ics(
            "UID:d\r\nDTSTART;TZID=Europe/Tallinn:20250328T090000\r\nDTEND;TZID=Europe/Tallinn:20250328T100000\r\n" +
                "RRULE:FREQ=DAILY;COUNT=5\r\nSUMMARY:Daily",
        )
        val occ = expand(text, "2025-03-01", "2025-04-30")
        assertEquals(5, occ.size)
        assertTrue(occ.all { it.localTime() == "09:00" })
        assertEquals(Instant.parse("2025-03-28T07:00:00Z"), occ[0].start)
        assertEquals(Instant.parse("2025-03-31T06:00:00Z"), occ[3].start)
    }

    @Test fun monthlyNthWeekdayAndLastDay() {
        val second = ics("UID:e\r\nDTSTART;TZID=Europe/Tallinn:20250114T180000\r\nRRULE:FREQ=MONTHLY;BYDAY=2TU\r\nSUMMARY:Club")
        assertEquals(
            listOf("2025-01-14", "2025-02-11", "2025-03-11"),
            expand(second, "2025-01-01", "2025-04-01").map { it.localDate() },
        )
        val last = ics("UID:f\r\nDTSTART;TZID=Europe/Tallinn:20250131T120000\r\nRRULE:FREQ=MONTHLY;BYMONTHDAY=-1\r\nSUMMARY:EOM")
        assertEquals(
            listOf("2025-01-31", "2025-02-28", "2025-03-31"),
            expand(last, "2025-01-01", "2025-04-01").map { it.localDate() },
        )
    }

    @Test fun monthlyOn31stSkipsShortMonths() {
        val text = ics("UID:g\r\nDTSTART;TZID=Europe/Tallinn:20250131T120000\r\nRRULE:FREQ=MONTHLY\r\nSUMMARY:31st")
        assertEquals(
            listOf("2025-01-31", "2025-03-31", "2025-05-31"),
            expand(text, "2025-01-01", "2025-06-01").map { it.localDate() },
        )
    }

    @Test fun yearlyAndUntil() {
        val text = ics(
            "UID:h\r\nDTSTART;VALUE=DATE:20240229\r\nRRULE:FREQ=YEARLY;UNTIL=20280229\r\nSUMMARY:Leap",
        )
        assertEquals(
            listOf("2024-02-29", "2028-02-29"),
            expand(text, "2024-01-01", "2032-01-01").map { it.localDate() },
        )
    }

    @Test fun overrideReplacesOccurrenceAndCancelledIsDropped() {
        val text = ics(
            "UID:i\r\nDTSTART;TZID=Europe/Tallinn:20250106T100000\r\nDTEND;TZID=Europe/Tallinn:20250106T110000\r\n" +
                "RRULE:FREQ=WEEKLY;COUNT=3\r\nSUMMARY:Series",
            "UID:i\r\nRECURRENCE-ID;TZID=Europe/Tallinn:20250113T100000\r\nDTSTART;TZID=Europe/Tallinn:20250113T150000\r\n" +
                "DTEND;TZID=Europe/Tallinn:20250113T160000\r\nSUMMARY:Moved",
            "UID:i\r\nRECURRENCE-ID;TZID=Europe/Tallinn:20250120T100000\r\nDTSTART;TZID=Europe/Tallinn:20250120T100000\r\n" +
                "STATUS:CANCELLED\r\nSUMMARY:Series",
        )
        val occ = expand(text, "2025-01-01", "2025-02-01")
        assertEquals(listOf("Series", "Moved"), occ.map { it.summary })
        assertEquals("15:00", occ[1].localTime())
    }

    @Test fun malformedInputIsSkippedNotThrown() {
        val text = ics(
            "UID:bad\r\nDTSTART:garbage\r\nSUMMARY:x",
            "DTSTART:20250101T100000Z\r\nSUMMARY:no uid",
            "UID:ok\r\nDTSTART:20250101T100000Z\r\nRRULE:FREQ=SOMETIMES\r\nSUMMARY:ok",
        )
        val parsed = parser.parse(text)
        assertEquals(listOf("ok"), parsed.map { it.uid })
        assertTrue(parsed[0].rrule == null)
    }

    @Test fun windowFiltersByOverlap() {
        val text = ics("UID:j\r\nDTSTART;TZID=Europe/Tallinn:20250105T230000\r\nDTEND;TZID=Europe/Tallinn:20250106T010000\r\nSUMMARY:Night")
        assertEquals(1, expand(text, "2025-01-06", "2025-01-07").size) // spills into the 6th
        assertEquals(0, expand(text, "2025-01-07", "2025-01-08").size)
    }
}
