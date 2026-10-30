package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
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
            assertTrue(EventColors.indexFor(it) in EventColors.palette.indices)
        }
    }

    @Test fun differentTitlesUseSeveralColors() {
        val used = (1..200).map { EventColors.indexFor("Event $it") }.toSet()
        assertTrue("only ${used.size} colors used", used.size >= EventColors.palette.size - 1)
    }

    @Test fun everyPairIsReadable() {
        EventColors.palette.forEach {
            val c = EventColors.contrast(it.bar, it.text)
            assertTrue("contrast %.2f for %06X".format(c, it.bar and 0xFFFFFF), c >= 4.5)
        }
    }
}
