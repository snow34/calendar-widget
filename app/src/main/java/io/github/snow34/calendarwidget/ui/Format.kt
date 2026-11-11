package io.github.snow34.calendarwidget.ui

import io.github.snow34.calendarwidget.core.Occurrence
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDate

object Format {
    private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val day = DateTimeFormatter.ofPattern("EEE d MMM")

    fun date(d: LocalDate): String = day.format(d)

    fun timeRange(o: Occurrence, zone: ZoneId = ZoneId.systemDefault()): String =
        if (o.allDay) "All day"
        else "${time.format(o.start.atZone(zone))} – ${time.format(o.end.atZone(zone))}"

    fun widgetWhen(o: Occurrence, zone: ZoneId = ZoneId.systemDefault()): String {
        val s = o.start.atZone(zone)
        val d = if (s.toLocalDate() == LocalDate.now(zone)) "Today" else day.format(s)
        return if (o.allDay) d else "$d ${time.format(s)}"
    }
}
