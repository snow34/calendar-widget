package io.github.snow34.calendarwidget.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.FontChoice
import io.github.snow34.calendarwidget.core.ThemeMode

/**
 * The app's look, driven by the user's [Appearance]: light/dark (or follow the phone), wallpaper colors or plain
 * Material colors, text size and font family. To bundle a custom font for the app, see `typographyFor`.
 */
@Composable
fun AppTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        appearance.wallpaperColors -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    // Every sp in the app is multiplied by the chosen text size (on top of the phone's own font size setting).
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * appearance.textScale.factor)) {
        MaterialTheme(colorScheme = colors, typography = typographyFor(appearance.font), content = content)
    }
}

/**
 * Material text styles with the chosen font family. To use your own font in the app: put the .ttf file in
 * `app/src/main/res/font/` (lowercase letters, digits and underscores only, e.g. `my_font.ttf`) and return
 * `FontFamily(Font(R.font.my_font))` for the choice you want to replace.
 */
private fun typographyFor(font: FontChoice): Typography {
    val family = when (font) {
        FontChoice.SANS -> FontFamily.Default
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.MONOSPACE -> FontFamily.Monospace
    }
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
