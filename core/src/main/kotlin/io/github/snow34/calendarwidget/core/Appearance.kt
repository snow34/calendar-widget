package io.github.snow34.calendarwidget.core

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Multiplies every text size in the app and the widgets. */
enum class TextScale(val factor: Float) { SMALL(0.85f), NORMAL(1f), LARGE(1.15f), XLARGE(1.3f) }

/** System font families only (widgets cannot load font files). */
enum class FontChoice { SANS, SERIF, MONOSPACE }

/** Height in dp of an event bar at normal text size (see [BarMetrics] for the size actually used). */
enum class BarHeight(val dp: Int) { COMPACT(11), NORMAL(14), TALL(17), XTALL(20) }

/**
 * Everything the user can change about how the app and the widgets look. Defaults reproduce the original look.
 * Stored as one plain string ([encode]); [parse] never throws and falls back to defaults for anything it does not understand.
 */
data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Use the wallpaper's colors for the app (Material You) instead of the plain light/dark palettes. */
    val wallpaperColors: Boolean = true,
    val textScale: TextScale = TextScale.NORMAL,
    val font: FontChoice = FontChoice.SANS,
    val barHeight: BarHeight = BarHeight.NORMAL,
    /** [EventColors.PALETTE_SIZE] opaque `0xRRGGBB` colors. */
    val palette: List<Int> = EventColors.DEFAULT_PALETTE,
) {
    fun encode(): String = listOf(
        "theme=$themeMode",
        "wallpaper=${if (wallpaperColors) 1 else 0}",
        "text=$textScale",
        "font=$font",
        "bar=$barHeight",
        "palette=" + palette.joinToString(",") { ColorHex.format(it).removePrefix("#") },
    ).joinToString(";")

    companion object {
        val DEFAULT = Appearance()

        fun parse(text: String?): Appearance {
            if (text.isNullOrBlank()) return DEFAULT
            val fields = text.split(';').mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq > 0) part.substring(0, eq).trim() to part.substring(eq + 1).trim() else null
            }.toMap()

            fun <T : Enum<T>> pick(key: String, all: Array<T>, fallback: T): T =
                all.firstOrNull { it.name == fields[key] } ?: fallback

            val colors = fields["palette"]?.split(',')?.map { ColorHex.parse(it) }
            return Appearance(
                themeMode = pick("theme", ThemeMode.entries.toTypedArray(), DEFAULT.themeMode),
                wallpaperColors = fields["wallpaper"]?.let { it != "0" } ?: DEFAULT.wallpaperColors,
                textScale = pick("text", TextScale.entries.toTypedArray(), DEFAULT.textScale),
                font = pick("font", FontChoice.entries.toTypedArray(), DEFAULT.font),
                barHeight = pick("bar", BarHeight.entries.toTypedArray(), DEFAULT.barHeight),
                palette = if (colors != null && colors.size == EventColors.PALETTE_SIZE && colors.all { it != null }) colors.filterNotNull() else DEFAULT.palette,
            )
        }
    }
}

/** Ready-made color sets for the event palette. */
object AppearancePresets {
    class Preset(val name: String, val colors: List<Int>)

    val all: List<Preset> = listOf(
        Preset("Default", EventColors.DEFAULT_PALETTE),
        Preset("Pastel", listOf(0xF4A9B8, 0xF9CFA0, 0xF5E6A0, 0xB7E0C2, 0xA9DCE5, 0xB9CDF7, 0xD2C4F2, 0xD9DCE1)),
        Preset("Vivid", listOf(0xE53560, 0xFF8A00, 0xFFC800, 0x2EB872, 0x00A6C8, 0x2F6FED, 0x8E4DEB, 0x7A8594)),
        Preset("Earth", listOf(0xC1694F, 0xD9A441, 0xA8A85B, 0x6B9080, 0x4F7C8A, 0x7E7AA3, 0x9C6B8E, 0x8A7F72)),
    )

    /** The preset whose colors are exactly [palette], or null for a custom palette. */
    fun matching(palette: List<Int>): Preset? = all.firstOrNull { it.colors == palette }
}

/**
 * Sizes shared by the Month widget and the app's Month grid, so text never gets clipped when the text size or bar height changes.
 * All values are dp except where noted. Defaults (normal text, normal bars) give the original 14 dp bars and 68 dp cells.
 */
object BarMetrics {
    const val BASE_TEXT_SP = 9f
    const val GAP_DP = 2f
    const val BARS_PER_MONTH_CELL = 3
    private const val DAY_NUMBER_BASE_DP = 18f
    private const val CELL_PADDING_DP = 2f

    /** Smallest bar that still fits a line of text at this text size. */
    fun minBarHeightDp(scale: TextScale): Float = BASE_TEXT_SP * scale.factor * 1.25f + 2f

    /** The chosen bar height, but never smaller than the text needs. */
    fun barHeightDp(bar: BarHeight, scale: TextScale): Float = maxOf(bar.dp.toFloat(), minBarHeightDp(scale))

    fun dayNumberHeightDp(scale: TextScale): Float = DAY_NUMBER_BASE_DP * scale.factor

    /** Height of one day cell in the app's Month grid: day number plus [BARS_PER_MONTH_CELL] bars. */
    fun monthCellHeightDp(bar: BarHeight, scale: TextScale): Float =
        dayNumberHeightDp(scale) + BARS_PER_MONTH_CELL * (barHeightDp(bar, scale) + GAP_DP) + CELL_PADDING_DP

    /** How many bars of [barDp] fit in a cell of [cellDp] below a day number of [dayNumberDp]. */
    fun slots(cellDp: Float, dayNumberDp: Float, barDp: Float): Int =
        ((cellDp - dayNumberDp + GAP_DP) / (barDp + GAP_DP) + 0.0001f).toInt().coerceAtLeast(0)
}
