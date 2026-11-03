package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetOpacityTest {
    @Test fun endpoints() {
        assertEquals(0f, WidgetOpacity.alphaFor(0), 0f)
        assertEquals(1f, WidgetOpacity.alphaFor(100), 0f)
        assertEquals(0.5f, WidgetOpacity.alphaFor(50), 0f)
    }

    @Test fun outOfRangeIsClamped() {
        assertEquals(0, WidgetOpacity.clampPercent(-20))
        assertEquals(100, WidgetOpacity.clampPercent(250))
        assertEquals(0f, WidgetOpacity.alphaFor(-1), 0f)
        assertEquals(1f, WidgetOpacity.alphaFor(101), 0f)
    }

    @Test fun defaultIsSolid() {
        assertEquals(1f, WidgetOpacity.alphaFor(WidgetOpacity.DEFAULT_PERCENT), 0f)
    }
}
