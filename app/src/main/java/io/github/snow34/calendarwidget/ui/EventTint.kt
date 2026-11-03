package io.github.snow34.calendarwidget.ui

import androidx.compose.ui.graphics.Color
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.EventColoring
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.core.Occurrence

/** Bar/card color and the readable text color for an event, shared by the app and the widgets. */
data class EventTint(val bar: Color, val text: Color)

/** The palette entry at [index] (wraps around, so any int is safe). */
fun tintAt(index: Int): EventTint {
    val c = EventColors.palette[Math.floorMod(index, EventColors.palette.size)]
    return EventTint(opaque(c.bar), opaque(c.text))
}

/** The calendars and the coloring rule currently in effect; used by every screen that draws events. */
class CalendarContext(val sources: List<CalendarSource>, val coloring: EventColoring) {
    fun tint(o: Occurrence): EventTint = tintAt(coloring.indexFor(o))

    /** The calendar's name, only worth showing when there is more than one calendar. */
    fun nameOf(o: Occurrence): String? =
        if (sources.size > 1) sources.firstOrNull { it.id == o.calendarId }?.name else null
}

/** Palette entries are plain 0xRRGGBB; Compose wants an opaque ARGB int. */
private fun opaque(rgb: Int) = Color(0xFF000000.toInt() or rgb)
