package io.github.snow34.calendarwidget.widget

import androidx.compose.ui.unit.sp
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.FontChoice

/** Widgets can only use system font families (no font files). */
internal fun glanceFont(font: FontChoice): FontFamily = when (font) {
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONOSPACE -> FontFamily.Monospace
}

/** Widget text in the user's font and text size: [baseSp] is the size at "Normal". */
internal fun widgetTextStyle(
    appearance: Appearance,
    color: ColorProvider,
    baseSp: Float,
    bold: Boolean = false,
    align: TextAlign? = null,
): TextStyle = TextStyle(
    color = color,
    fontSize = (baseSp * appearance.textScale.factor).sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    textAlign = align,
    fontFamily = glanceFont(appearance.font),
)
