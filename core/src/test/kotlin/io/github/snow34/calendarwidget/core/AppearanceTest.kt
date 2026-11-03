package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    private val custom = Appearance(
        themeMode = ThemeMode.DARK,
        wallpaperColors = false,
        textScale = TextScale.XLARGE,
        font = FontChoice.SERIF,
        barHeight = BarHeight.TALL,
        palette = AppearancePresets.all[2].colors,
    )

    @Test fun roundTrip() {
        assertEquals(custom, Appearance.parse(custom.encode()))
        assertEquals(Appearance.DEFAULT, Appearance.parse(Appearance.DEFAULT.encode()))
    }

    @Test fun missingOrBlankGivesDefaults() {
        assertEquals(Appearance.DEFAULT, Appearance.parse(null))
        assertEquals(Appearance.DEFAULT, Appearance.parse(""))
        assertEquals(Appearance.DEFAULT, Appearance.parse("   "))
    }

    @Test fun garbageNeverThrowsAndFallsBack() {
        listOf("nonsense", ";;;", "=", "theme=", "theme=PURPLE;text=HUGE;font=COMIC;bar=GIANT;palette=1,2,3", "palette=" + "ZZZZZZ,".repeat(8))
            .forEach { assertEquals(it, Appearance.DEFAULT, Appearance.parse(it)) }
    }

    @Test fun oneBadFieldDoesNotSpoilTheOthers() {
        val parsed = Appearance.parse("theme=LIGHT;text=HUGE;font=MONOSPACE")
        assertEquals(ThemeMode.LIGHT, parsed.themeMode)
        assertEquals(TextScale.NORMAL, parsed.textScale)
        assertEquals(FontChoice.MONOSPACE, parsed.font)
    }

    @Test fun paletteMustHaveExactlyEightValidColors() {
        val seven = custom.palette.take(7).joinToString(",") { ColorHex.format(it).removePrefix("#") }
        assertEquals(Appearance.DEFAULT.palette, Appearance.parse("palette=$seven").palette)
        val nine = (custom.palette + 0x123456).joinToString(",") { ColorHex.format(it).removePrefix("#") }
        assertEquals(Appearance.DEFAULT.palette, Appearance.parse("palette=$nine").palette)
    }

    @Test fun defaultsReproduceTheOriginalLook() {
        val d = Appearance.DEFAULT
        assertEquals(ThemeMode.SYSTEM, d.themeMode)
        assertEquals(TextScale.NORMAL, d.textScale)
        assertEquals(EventColors.DEFAULT_PALETTE, d.palette)
        assertTrue(d.wallpaperColors)
    }

    @Test fun presetsAreValidAndDistinct() {
        AppearancePresets.all.forEach { assertEquals(it.name, EventColors.PALETTE_SIZE, it.colors.size) }
        assertEquals(AppearancePresets.all.size, AppearancePresets.all.map { it.colors }.toSet().size)
        assertEquals("Default", AppearancePresets.all.first().name)
        assertEquals(EventColors.DEFAULT_PALETTE, AppearancePresets.all.first().colors)
    }

    @Test fun matchingPresetByColors() {
        assertEquals("Pastel", AppearancePresets.matching(AppearancePresets.all[1].colors)?.name)
        assertNull(AppearancePresets.matching(EventColors.DEFAULT_PALETTE.mapIndexed { i, c -> if (i == 0) 0x123456 else c }))
        assertNotNull(AppearancePresets.matching(EventColors.DEFAULT_PALETTE))
    }
}
