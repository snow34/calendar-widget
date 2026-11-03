package io.github.snow34.calendarwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.BarMetrics
import io.github.snow34.calendarwidget.core.EventColoring
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.core.groupByDay
import io.github.snow34.calendarwidget.data.CalendarRepository
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.ui.tintAt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.ceil

/** Whole-month grid. Each event is a colored bar with its title under the day number (stripes if titles are hidden). */
class MonthWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Re-read settings and events whenever Widgets.revision changes (see Widgets.revision for why).
            val revision by Widgets.revision.collectAsState()
            val data = remember(revision) { load(context) }
            GlanceTheme { Content(data) }
        }
    }

    private class MonthData(
        val configured: Boolean, val showTitles: Boolean, val opacity: Int,
        val month: YearMonth, val byDay: Map<LocalDate, List<Occurrence>>, val coloring: EventColoring,
        val appearance: Appearance,
    )

    private fun load(context: Context): MonthData {
        val store = SecureStore(context)
        val month = YearMonth.now().plusMonths(store.widgetMonthOffset.toLong())
        val zone = ZoneId.systemDefault()
        val sources = store.loadCalendars()
        val occ = CalendarRepository(store).occurrences(
            month.atDay(1).atStartOfDay(zone).toInstant(),
            month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(),
            sources,
        )
        return MonthData(
            sources.any { it.enabled }, store.widgetShowTitles, store.widgetOpacityPercent, month, groupByDay(occ, zone),
            EventColoring.from(sources, store.colorMode), store.appearance,
        )
    }

    @Composable
    private fun Content(data: MonthData) {
        val a = data.appearance
        val k = a.textScale.factor
        val size = LocalSize.current
        val locale = Locale.getDefault()
        val today = LocalDate.now()
        val month = data.month
        val onSurface = GlanceTheme.colors.onSurface

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(widgetBackgroundColor(data.opacity))
                .padding(PAD),
        ) {
            // Header: previous | month name (tap = back to this month) | next
            Row(modifier = GlanceModifier.fillMaxWidth().height(HEADER_H), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", modifier = GlanceModifier.width(36.dp).clickable(actionRunCallback<MonthNavCallback>(nav(-1))),
                    style = widgetTextStyle(a, GlanceTheme.colors.primary, 20f, align = TextAlign.Center),
                )
                Text(
                    "${month.month.getDisplayName(DateTextStyle.FULL, locale)} ${month.year}",
                    modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<MonthNavCallback>(nav(0))),
                    style = widgetTextStyle(a, onSurface, 15f, bold = true, align = TextAlign.Center),
                )
                Text(
                    "›", modifier = GlanceModifier.width(36.dp).clickable(actionRunCallback<MonthNavCallback>(nav(1))),
                    style = widgetTextStyle(a, GlanceTheme.colors.primary, 20f, align = TextAlign.Center),
                )
            }

            if (!data.configured) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(openApp(null))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Tap to add or turn on a calendar", style = widgetTextStyle(a, onSurface, 13f))
                }
                return@Column
            }

            val weekdayH = WEEKDAY_H * k
            val dayNumH = BarMetrics.dayNumberHeightDp(a.textScale).dp
            val lead = month.atDay(1).dayOfWeek.value - 1 // Monday-first
            val weeks = ceil((lead + month.lengthOfMonth()) / 7.0).toInt()
            val cellW = ((size.width.value - PAD.value * 2) / 7f).dp
            val cellH = ((size.height.value - PAD.value * 2 - HEADER_H.value - weekdayH.value) / weeks).dp

            Row(modifier = GlanceModifier.fillMaxWidth().height(weekdayH)) {
                DayOfWeek.entries.forEach { d ->
                    Text(
                        d.getDisplayName(DateTextStyle.NARROW, locale),
                        modifier = GlanceModifier.width(cellW),
                        style = widgetTextStyle(a, GlanceTheme.colors.onSurfaceVariant, 10f, align = TextAlign.Center),
                    )
                }
            }

            repeat(weeks) { w ->
                Row {
                    repeat(7) { c ->
                        val dayNum = w * 7 + c - lead + 1
                        if (dayNum < 1 || dayNum > month.lengthOfMonth()) {
                            Box(GlanceModifier.width(cellW).height(cellH)) {}
                        } else {
                            val date = month.atDay(dayNum)
                            DayCell(date, date == today, data.byDay[date].orEmpty(), data, dayNumH, cellW, cellH)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DayCell(
        date: LocalDate, isToday: Boolean, events: List<Occurrence>,
        data: MonthData, dayNumH: Dp, w: Dp, h: Dp,
    ) {
        val a = data.appearance
        // Bars with titles use the chosen bar height; without titles they are thin stripes.
        val barH = if (data.showTitles) BarMetrics.barHeightDp(a.barHeight, a.textScale).dp else STRIPE_H
        val gap = BarMetrics.GAP_DP.dp
        val slots = BarMetrics.slots(h.value, dayNumH.value, barH.value)
        val overflow = events.size > slots
        // When there is not enough room for everything, the last slot becomes a "+N" line.
        val shown = if (overflow && slots >= 2) slots - 1 else slots.coerceAtMost(events.size)

        Column(
            modifier = GlanceModifier.width(w).height(h).padding(1.dp).clickable(actionStartActivity(openApp(date))),
        ) {
            Box(GlanceModifier.fillMaxWidth().height(dayNumH), contentAlignment = Alignment.Center) {
                if (isToday) {
                    Box(
                        GlanceModifier.width(dayNumH).height(dayNumH)
                            .background(ColorProvider(TODAY_BG))
                            .cornerRadius(dayNumH / 2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${date.dayOfMonth}", style = widgetTextStyle(a, ColorProvider(TODAY_TEXT), 11f, bold = true))
                    }
                } else {
                    Text("${date.dayOfMonth}", style = widgetTextStyle(a, GlanceTheme.colors.onSurface, 11f))
                }
            }
            if (events.isEmpty()) return@Column

            if (slots <= 0) {
                // Cell too small for any bar: just show how many events there are.
                Text(
                    "•".repeat(events.size.coerceAtMost(3)),
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = widgetTextStyle(a, GlanceTheme.colors.primary, 9f, align = TextAlign.Center),
                )
                return@Column
            }

            events.take(shown).forEach { e ->
                val title = e.summary.ifBlank { "(no title)" }
                val tint = tintAt(data.coloring.indexFor(e), a.palette)
                Box(
                    GlanceModifier.fillMaxWidth().height(barH)
                        .background(ColorProvider(tint.bar))
                        .cornerRadius(BAR_RADIUS),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (data.showTitles) {
                        Text(
                            title, maxLines = 1,
                            modifier = GlanceModifier.padding(horizontal = 2.dp),
                            style = widgetTextStyle(a, ColorProvider(tint.text), BarMetrics.BASE_TEXT_SP),
                        )
                    }
                }
                Spacer(GlanceModifier.height(gap))
            }
            if (overflow && slots >= 2) {
                Text(
                    "+${events.size - shown}", maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = widgetTextStyle(a, GlanceTheme.colors.onSurfaceVariant, BarMetrics.BASE_TEXT_SP, align = TextAlign.Center),
                )
            }
        }
    }

    private fun nav(delta: Int): ActionParameters = actionParametersOf(NavDelta to delta)

    private fun openApp(date: LocalDate?): Intent =
        Widgets.openAppIntent(Uri.parse(if (date == null) "calendarwidget://open" else "calendarwidget://day/$date"))

    private companion object {
        val PAD = 6.dp
        val HEADER_H = 32.dp
        val WEEKDAY_H = 14.dp
        val STRIPE_H = 5.dp
        val BAR_RADIUS = 4.dp
        val TODAY_BG = Color(0xFFA8C7FA)
        val TODAY_TEXT = Color(0xFF062E6F)
    }
}

private val NavDelta = ActionParameters.Key<Int>("delta")

/** Prev / next / back-to-today. The chosen month is kept in plain app-private preferences. */
class MonthNavCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val store = SecureStore(context)
        val delta = parameters[NavDelta] ?: 0
        store.widgetMonthOffset = if (delta == 0) 0 else store.widgetMonthOffset + delta
        Widgets.requestRefresh()
        MonthWidget().update(context, glanceId)
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}
