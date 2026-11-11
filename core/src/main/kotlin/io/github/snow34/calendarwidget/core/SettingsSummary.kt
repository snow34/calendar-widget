package io.github.snow34.calendarwidget.core

/** One-line descriptions shown on a collapsed Settings section, so you can see the current state without opening it. */
object SettingsSummary {
    private fun calendarCount(n: Int) = if (n == 1) "1 calendar" else "$n calendars"

    fun calendars(total: Int, enabled: Int): String = when {
        total <= 0 -> "None added yet"
        enabled >= total -> calendarCount(total)
        else -> "${calendarCount(total)}, ${total - enabled} turned off"
    }

    fun appearance(theme: ThemeMode, textScale: TextScale, presetName: String?): String {
        val themeText = when (theme) {
            ThemeMode.SYSTEM -> "Follows phone"
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
        }
        val textText = when (textScale) {
            TextScale.SMALL -> "small"
            TextScale.NORMAL -> "normal"
            TextScale.LARGE -> "large"
            TextScale.XLARGE -> "extra large"
        }
        return "$themeText, $textText text, ${presetName ?: "custom"} colors"
    }

    fun widgets(showTitles: Boolean, opacityPercent: Int): String =
        (if (showTitles) "Titles shown" else "Titles hidden") + ", background ${WidgetOpacity.clampPercent(opacityPercent)}%"

    fun sync(hours: Int): String = if (hours == 1) "Every hour" else "Every $hours hours"

    fun security(appLockEnabled: Boolean): String = if (appLockEnabled) "App lock on" else "App lock off"

    fun googleAccount(hasClientId: Boolean, signedIn: Boolean): String = when {
        signedIn -> "Signed in: new events are saved in the app"
        hasClientId -> "Not signed in"
        else -> "Not set up: Add event opens Google Calendar"
    }
}
