package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventColorsTest {
    @Test fun sameTitleAlwaysGetsSameColor() {
        assertEquals(EventColors.indexFor("EmÜ mäng Tartus"), EventColors.indexFor("EmÜ mäng Tartus"))
    }

    @Test fun caseAndOuterSpacesDoNotMatter() {
        assertEquals(EventColors.indexFor("Team Standup"), EventColors.indexFor("  team standup "))
    }

    @Test fun indexIsAlwaysInsidePalette() {
        listOf("", " ", "a", "Läti poiste kobtsert", "😀", "x".repeat(10_000)).forEach {
            assertTrue(EventColors.indexFor(it) in 0 until EventColors.PALETTE_SIZE)
        }
    }

    @Test fun differentTitlesUseSeveralColors() {
        val used = (1..200).map { EventColors.indexFor("Event $it") }.toSet()
        assertTrue("only ${used.size} colors used", used.size >= EventColors.PALETTE_SIZE - 1)
    }

    @Test fun defaultPaletteHasTheRightSize() {
        assertEquals(EventColors.PALETTE_SIZE, EventColors.DEFAULT_PALETTE.size)
    }

    @Test fun textIsReadableOnEveryPresetColor() {
        AppearancePresets.all.flatMap { it.colors }.forEach { bar ->
            val c = EventColors.contrast(bar, EventColors.readableTextOn(bar))
            assertTrue("contrast %.2f for %06X".format(c, bar), c >= 4.5)
        }
    }

    @Test fun textIsReadableOnAnyColor() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val bar = (r shl 16) or (g shl 8) or b
            val c = EventColors.contrast(bar, EventColors.readableTextOn(bar))
            assertTrue("contrast %.2f for %06X".format(c, bar), c >= 4.5)
        }
    }

    @Test fun blackOnLightAndWhiteOnDark() {
        assertEquals(0x000000, EventColors.readableTextOn(0xFFFFFF))
        assertEquals(0xFFFFFF, EventColors.readableTextOn(0x000000))
        assertEquals(0xFFFFFF, EventColors.readableTextOn(0x1B3A8C))
    }

    @Test fun hexFormatAndParse() {
        assertEquals("#D95D77", ColorHex.format(0xD95D77))
        assertEquals("#00000A", ColorHex.format(0x00000A))
        assertEquals(0xD95D77, ColorHex.parse("#d95d77"))
        assertEquals(0xD95D77, ColorHex.parse(" D95D77 "))
        assertNull(ColorHex.parse("#D95D7"))
        assertNull(ColorHex.parse("#GGGGGG"))
        assertNull(ColorHex.parse(""))
        assertNull(ColorHex.parse("#D95D77FF"))
    }
}
