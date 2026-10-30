package io.github.snow34.calendarwidget.core

/** Widget background opacity setting: whole percent, 0 (fully see-through) .. 100 (solid). */
object WidgetOpacity {
    const val DEFAULT_PERCENT = 100

    fun clampPercent(percent: Int): Int = percent.coerceIn(0, 100)

    /** Alpha channel (0f..1f) for a percent setting; out-of-range values are clamped. */
    fun alphaFor(percent: Int): Float = clampPercent(percent) / 100f
}
