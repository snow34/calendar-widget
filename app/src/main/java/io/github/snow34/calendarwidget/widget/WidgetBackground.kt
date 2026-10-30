package io.github.snow34.calendarwidget.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.unit.ColorProvider
import io.github.snow34.calendarwidget.core.WidgetOpacity

/**
 * Background of a widget at the user's chosen opacity. At 100% this is the normal theme background; below that a
 * neutral light/dark surface with the matching alpha is used (the theme's colors can't be given an alpha directly).
 * Event bars and text are drawn separately and stay fully opaque.
 */
@Composable
fun widgetBackgroundColor(opacityPercent: Int): ColorProvider {
    if (WidgetOpacity.clampPercent(opacityPercent) >= 100) return GlanceTheme.colors.widgetBackground
    val alpha = WidgetOpacity.alphaFor(opacityPercent)
    return androidx.glance.color.ColorProvider(
        day = Color(0xFFFDFBFF).copy(alpha = alpha),
        night = Color(0xFF1B1B1F).copy(alpha = alpha),
    )
}
