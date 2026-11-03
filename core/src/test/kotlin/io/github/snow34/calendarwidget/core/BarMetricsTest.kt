package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarMetricsTest {
    @Test fun defaultsGiveTheOriginalSizes() {
        assertEquals(14f, BarMetrics.barHeightDp(BarHeight.NORMAL, TextScale.NORMAL), 0.001f)
        assertEquals(68f, BarMetrics.monthCellHeightDp(BarHeight.NORMAL, TextScale.NORMAL), 0.001f)
        assertEquals(18f, BarMetrics.dayNumberHeightDp(TextScale.NORMAL), 0.001f)
    }

    @Test fun barIsNeverSmallerThanTheTextNeeds() {
        for (bar in BarHeight.entries) for (scale in TextScale.entries) {
            assertTrue("$bar/$scale", BarMetrics.barHeightDp(bar, scale) >= BarMetrics.minBarHeightDp(scale))
            assertTrue("$bar/$scale", BarMetrics.barHeightDp(bar, scale) >= bar.dp)
        }
    }

    @Test fun biggerSettingsNeverShrinkAnything() {
        for (scale in TextScale.entries) {
            val heights = BarHeight.entries.map { BarMetrics.barHeightDp(it, scale) }
            assertEquals(heights.sorted(), heights)
        }
        for (bar in BarHeight.entries) {
            val heights = TextScale.entries.map { BarMetrics.barHeightDp(bar, it) }
            assertEquals(heights.sorted(), heights)
            val cells = TextScale.entries.map { BarMetrics.monthCellHeightDp(bar, it) }
            assertEquals(cells.sorted(), cells)
        }
    }

    @Test fun monthCellAlwaysFitsThreeBars() {
        for (bar in BarHeight.entries) for (scale in TextScale.entries) {
            val slots = BarMetrics.slots(
                BarMetrics.monthCellHeightDp(bar, scale), BarMetrics.dayNumberHeightDp(scale), BarMetrics.barHeightDp(bar, scale),
            )
            assertEquals("$bar/$scale", BarMetrics.BARS_PER_MONTH_CELL, slots)
        }
    }

    @Test fun slotsForWidgetCells() {
        assertEquals(3, BarMetrics.slots(68f, 18f, 14f))
        assertEquals(0, BarMetrics.slots(20f, 18f, 14f))
        assertEquals(0, BarMetrics.slots(10f, 18f, 14f)) // never negative
        assertEquals(4, BarMetrics.slots(18f + 4 * 7f - 2f, 18f, 5f)) // thin stripes
    }
}
