package io.github.snow34.calendarwidget.core

/**
 * Event/calendar colors. Google's private iCal feed carries no per-event colors, so each event (or calendar) gets one of
 * [PALETTE_SIZE] palette colors. The user can edit the palette (see [Appearance.palette]); all colors here are opaque
 * `0xRRGGBB` ints, and the text color on a bar is derived with [readableTextOn].
 */
object EventColors {
    const val PALETTE_SIZE = 8

    /** The built-in palette: rose, orange, yellow, green, teal, blue, purple, grey. */
    val DEFAULT_PALETTE: List<Int> = listOf(
        0xD95D77, 0xF2A65A, 0xE9CF5E, 0x7CC38F, 0x5FB8C9, 0x7FA6F0, 0xA992E8, 0xC4C9D1,
    )

    private const val BLACK = 0x000000
    private const val WHITE = 0xFFFFFF

    /** Palette index for an event title: the same title always maps to the same index (FNV-1a over the normalised title). */
    fun indexFor(title: String): Int {
        var h = 0x811C9DC5.toInt()
        for (c in title.trim().lowercase()) {
            h = (h xor c.code) * 0x01000193
        }
        return Math.floorMod(h, PALETTE_SIZE)
    }

    /** Black or white, whichever is easier to read on [barRgb] (always at least 4.5:1 contrast). */
    fun readableTextOn(barRgb: Int): Int =
        if (contrast(barRgb, BLACK) >= contrast(barRgb, WHITE)) BLACK else WHITE

    /** WCAG 2.x contrast ratio between two opaque `0xRRGGBB` colors (1.0 .. 21.0). */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel((rgb shr 16) and 0xFF) +
            0.7152 * channel((rgb shr 8) and 0xFF) +
            0.0722 * channel(rgb and 0xFF)
    }
}

/** `#RRGGBB` text for opaque `0xRRGGBB` ints. */
object ColorHex {
    fun format(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)

    /** Accepts `RRGGBB` or `#RRGGBB` (any case, surrounding spaces ignored); anything else is null. */
    fun parse(text: String): Int? {
        val t = text.trim().removePrefix("#")
        if (t.length != 6 || !t.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return t.toInt(16)
    }
}
