package io.github.snow34.calendarwidget.ui

import androidx.compose.ui.graphics.Color
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.EventColoring
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.core.Occurrence

/** Bar/card color and the readable text color for an event, shared by the app and the widgets. */
data class EventTint(val bar: Color, val text: Color)

/** The palette color at [index] (wraps around, so any int is safe) with a black or white text color that is easy to read on it. */
fun tintAt(index: Int, palette: List<Int>): EventTint {
    val rgb = palette[Math.floorMod(index, palette.size)]
    return EventTint(opaqueColor(rgb), opaqueColor(EventColors.readableTextOn(rgb)))
}

/** The calendars, the coloring rule and the palette currently in effect; used by every screen that draws events. */
class CalendarContext(val sources: List<CalendarSource>, val coloring: EventColoring, val palette: List<Int>) {
    fun tint(o: Occurrence): EventTint = tintAt(coloring.indexFor(o), palette)

    /** The calendar's name, only worth showing when there is more than one calendar. */
    fun nameOf(o: Occurrence): String? =
        if (sources.size > 1) sources.firstOrNull { it.id == o.calendarId }?.name else null
}

/** Palette entries are plain 0xRRGGBB; Compose wants an opaque ARGB int. */
fun opaqueColor(rgb: Int): Color = Color(0xFF000000.toInt() or rgb)
