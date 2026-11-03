package io.github.snow34.calendarwidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.AppearancePresets
import io.github.snow34.calendarwidget.core.BarHeight
import io.github.snow34.calendarwidget.core.BarMetrics
import io.github.snow34.calendarwidget.core.ColorHex
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.core.FontChoice
import io.github.snow34.calendarwidget.core.TextScale
import io.github.snow34.calendarwidget.core.ThemeMode
import kotlin.math.roundToInt

/** Settings block for theme, text size, font, bar height and the 8 event colors. [onChange] receives the new settings. */
@Composable
internal fun AppearanceSection(appearance: Appearance, onChange: (Appearance) -> Unit) {
    var editing by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Preview(appearance)

        Text("Theme", style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            listOf(ThemeMode.SYSTEM to "Follow phone", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
            appearance.themeMode,
        ) { onChange(appearance.copy(themeMode = it)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Use wallpaper colors in the app", Modifier.weight(1f))
            Switch(checked = appearance.wallpaperColors, onCheckedChange = { onChange(appearance.copy(wallpaperColors = it)) })
        }

        Text("Text size (app and widgets)", style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            listOf(TextScale.SMALL to "Small", TextScale.NORMAL to "Normal", TextScale.LARGE to "Large", TextScale.XLARGE to "Extra large"),
            appearance.textScale,
        ) { onChange(appearance.copy(textScale = it)) }

        Text("Font", style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            listOf(FontChoice.SANS to "Sans-serif", FontChoice.SERIF to "Serif", FontChoice.MONOSPACE to "Monospace"),
            appearance.font,
        ) { onChange(appearance.copy(font = it)) }

        Text("Event bar height", style = MaterialTheme.typography.titleSmall)
        ChoiceRow(
            listOf(BarHeight.COMPACT to "Compact", BarHeight.NORMAL to "Normal", BarHeight.TALL to "Tall", BarHeight.XTALL to "Extra tall"),
            appearance.barHeight,
        ) { onChange(appearance.copy(barHeight = it)) }

        Text("Event colors", style = MaterialTheme.typography.titleSmall)
        val matching = AppearancePresets.matching(appearance.palette)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppearancePresets.all.forEach { preset ->
                FilterChip(
                    selected = matching == preset,
                    onClick = { onChange(appearance.copy(palette = preset.colors)) },
                    label = { Text(preset.name) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            appearance.palette.forEachIndexed { index, rgb ->
                Box(
                    Modifier.weight(1f).height(40.dp).background(opaqueColor(rgb), RoundedCornerShape(6.dp)).clickable { editing = index },
                )
            }
        }
        Text(
            (if (matching == null) "Custom colors. " else "") +
                "Tap a color to change it. Calendars and event names both pick from these 8 colors.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { onChange(Appearance.DEFAULT) }) { Text("Reset appearance") }
    }

    editing?.let { index ->
        ColorEditDialog(
            initial = appearance.palette[index],
            default = EventColors.DEFAULT_PALETTE[index],
            onDismiss = { editing = null },
            onSave = { rgb ->
                onChange(appearance.copy(palette = appearance.palette.toMutableList().also { it[index] = rgb }))
                editing = null
            },
        )
    }
}

/** A few sample event bars drawn with the current settings, so changes can be judged at a glance. */
@Composable
private fun Preview(appearance: Appearance) {
    val barH = BarMetrics.barHeightDp(appearance.barHeight, appearance.textScale).dp
    Column(verticalArrangement = Arrangement.spacedBy(BarMetrics.GAP_DP.dp)) {
        listOf("Team meeting", "Lunch with Anna", "Dentist").forEachIndexed { i, label ->
            val tint = tintAt(i, appearance.palette)
            Box(
                Modifier.fillMaxWidth().height(barH).background(tint.bar, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(label, fontSize = BarMetrics.BASE_TEXT_SP.sp, maxLines = 1, color = tint.text) }
        }
    }
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun ColorEditDialog(initial: Int, default: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var rgb by remember { mutableIntStateOf(initial) }
    var hex by remember { mutableStateOf(ColorHex.format(initial)) }
    fun setColor(newRgb: Int) { rgb = newRgb; hex = ColorHex.format(newRgb) }
    val hexIsValid = ColorHex.parse(hex) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change color") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(44.dp).background(opaqueColor(rgb), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { Text("Sample event", color = opaqueColor(EventColors.readableTextOn(rgb))) }
                ChannelSlider("Red", (rgb shr 16) and 0xFF) { setColor((rgb and 0x00FFFF) or (it shl 16)) }
                ChannelSlider("Green", (rgb shr 8) and 0xFF) { setColor((rgb and 0xFF00FF) or (it shl 8)) }
                ChannelSlider("Blue", rgb and 0xFF) { setColor((rgb and 0xFFFF00) or it) }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { text -> hex = text; ColorHex.parse(text)?.let { rgb = it } },
                    label = { Text("Hex code, like #D95D77") },
                    singleLine = true,
                    isError = !hexIsValid,
                )
                TextButton(onClick = { setColor(default) }) { Text("Reset this color") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(rgb) }, enabled = hexIsValid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value", style = MaterialTheme.typography.bodySmall)
        Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = 0f..255f)
    }
}
