package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSummaryTest {
    @Test fun calendars() {
        assertEquals("None added yet", SettingsSummary.calendars(0, 0))
        assertEquals("1 calendar", SettingsSummary.calendars(1, 1))
        assertEquals("3 calendars", SettingsSummary.calendars(3, 3))
        assertEquals("2 calendars, 1 turned off", SettingsSummary.calendars(2, 1))
        assertEquals("1 calendar, 1 turned off", SettingsSummary.calendars(1, 0))
        assertEquals("4 calendars, 4 turned off", SettingsSummary.calendars(4, 0))
    }

    @Test fun appearance() {
        assertEquals("Follows phone, normal text, Default colors", SettingsSummary.appearance(ThemeMode.SYSTEM, TextScale.NORMAL, "Default"))
        assertEquals("Dark, extra large text, custom colors", SettingsSummary.appearance(ThemeMode.DARK, TextScale.XLARGE, null))
        assertEquals("Light, small text, Pastel colors", SettingsSummary.appearance(ThemeMode.LIGHT, TextScale.SMALL, "Pastel"))
    }

    @Test fun widgets() {
        assertEquals("Titles shown, background 100%", SettingsSummary.widgets(true, 100))
        assertEquals("Titles hidden, background 35%", SettingsSummary.widgets(false, 35))
        assertEquals("Titles shown, background 100%", SettingsSummary.widgets(true, 250)) // clamped
    }

    @Test fun syncAndSecurity() {
        assertEquals("Every hour", SettingsSummary.sync(1))
        assertEquals("Every 3 hours", SettingsSummary.sync(3))
        assertEquals("App lock on", SettingsSummary.security(true))
        assertEquals("App lock off", SettingsSummary.security(false))
    }
}
