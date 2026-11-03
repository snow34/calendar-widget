package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WindowsTimeZonesTest {
    @Test fun everyMappedZoneIsAValidIanaId() {
        WindowsTimeZones.all.forEach { (windows, iana) -> ZoneId.of(iana); assertEquals(windows, windows.trim()) }
    }

    @Test fun unknownNamesAreNull() {
        assertNull(WindowsTimeZones.toIana("Mars Standard Time"))
        assertEquals("Europe/Berlin", WindowsTimeZones.toIana(" W. Europe Standard Time "))
    }

    private val tallinn = ZoneId.of("Europe/Tallinn")
    private fun ics(tzid: String) =
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:x\r\nDTSTART;TZID=$tzid:20260110T090000\r\nDTEND;TZID=$tzid:20260110T100000\r\nSUMMARY:t\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"

    @Test fun outlookStyleTzidIsUnderstood() {
        // 09:00 in Berlin in January is 08:00Z (the phone's zone, Tallinn, would give 07:00Z)
        val e = IcsParser(tallinn).parse(ics("W. Europe Standard Time")).single()
        assertEquals(Instant.parse("2026-01-10T08:00:00Z"), e.start.toInstant())
    }

    @Test fun unknownTzidFallsBackToThePhoneZone() {
        val e = IcsParser(tallinn).parse(ics("Mars Standard Time")).single()
        assertEquals(Instant.parse("2026-01-10T07:00:00Z"), e.start.toInstant())
    }

    @Test fun calendarIdIsCarriedOntoOccurrences() {
        val raw = IcsParser(tallinn).parse(ics("Europe/Tallinn"))
        val from = Instant.parse("2026-01-01T00:00:00Z")
        val occ = RecurrenceExpander.expand(raw, from, from.plusSeconds(30L * 86400), calendarId = "cal-7")
        assertEquals(listOf("cal-7"), occ.map { it.calendarId })
        assertEquals("", RecurrenceExpander.expand(raw, from, from.plusSeconds(30L * 86400)).single().calendarId)
    }
}
