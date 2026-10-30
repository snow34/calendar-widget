package io.github.snow34.calendarwidget.core

/** An opaque `0xRRGGBB` bar color and the text color drawn on it. */
data class BarColor(val bar: Int, val text: Int)

/**
 * Google's private iCal feed carries no per-event colors, so each event gets one from this palette,
 * chosen from its title: the same title (and every repeat of it) always gets the same color.
 *
 * Keep in sync with `app/src/main/res/drawable/bar_0.xml` .. `bar_7.xml` (same order, same colors).
 */
object EventColors {
    val palette: List<BarColor> = listOf(
        BarColor(0xD95D77.toInt(), 0x2B0A12), // rose
        BarColor(0xF2A65A.toInt(), 0x2E1A00), // orange
        BarColor(0xE9CF5E.toInt(), 0x2B2400), // yellow
        BarColor(0x7CC38F.toInt(), 0x06240F), // green
        BarColor(0x5FB8C9.toInt(), 0x05242B), // teal
        BarColor(0x7FA6F0.toInt(), 0x0A1F4D), // blue
        BarColor(0xA992E8.toInt(), 0x1F0F4D), // purple
        BarColor(0xC4C9D1.toInt(), 0x1B1F26), // grey
    )

    fun indexFor(title: String): Int {
        // FNV-1a over the normalised title: stable across runs, devices and Kotlin versions.
        var h = 0x811C9DC5.toInt()
        for (c in title.trim().lowercase()) {
            h = (h xor c.code) * 0x01000193
        }
        return Math.floorMod(h, palette.size)
    }

    fun colorFor(title: String): BarColor = palette[indexFor(title)]

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
