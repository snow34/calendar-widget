package io.github.snow34.calendarwidget.ui

import androidx.compose.ui.graphics.Color
import io.github.snow34.calendarwidget.core.EventColors

/** Bar/card color and the readable text color for an event, shared by the app and the widgets. */
data class EventTint(val bar: Color, val text: Color)

fun eventTint(title: String): EventTint {
    val c = EventColors.colorFor(title)
    return EventTint(opaque(c.bar), opaque(c.text))
}

/** Palette entries are plain 0xRRGGBB; Compose wants an opaque ARGB int. */
private fun opaque(rgb: Int) = Color(0xFF000000.toInt() or rgb)
