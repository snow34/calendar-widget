package io.github.snow34.calendarwidget.core

/** What decides an event's color: its calendar (like Google Calendar) or its title. */
enum class ColorMode {
    CALENDAR, TITLE;

    companion object {
        fun parse(value: String?): ColorMode? = entries.firstOrNull { it.name == value }
    }
}

/** Chooses a palette index for an event, from the calendar's own color or from the event title. */
class EventColoring(private val calendarColors: Map<String, Int>, private val mode: ColorMode) {
    fun indexFor(o: Occurrence): Int = indexFor(o.summary, o.calendarId)

    fun indexFor(title: String, calendarId: String): Int {
        if (mode == ColorMode.CALENDAR) {
            calendarColors[calendarId]?.let { return Math.floorMod(it, EventColors.PALETTE_SIZE) }
        }
        return EventColors.indexFor(title)
    }

    companion object {
        fun from(sources: List<CalendarSource>, mode: ColorMode) =
            EventColoring(sources.associate { it.id to it.colorIndex }, mode)
    }
}
