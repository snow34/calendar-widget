package io.github.snow34.calendarwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.core.groupByDay
import io.github.snow34.calendarwidget.data.CalendarRepository
import io.github.snow34.calendarwidget.data.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.ceil

/** Whole-month grid. Event titles are drawn inside each day when they fit and the user allowed titles. */
class MonthWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = SecureStore(context)
        val configured = store.hasUrl()
        val showTitles = store.widgetShowTitles
        val month = YearMonth.now().plusMonths(store.widgetMonthOffset.toLong())
        val zone = ZoneId.systemDefault()
        val byDay = withContext(Dispatchers.Default) {
            val occ = CalendarRepository(store).occurrences(
                month.atDay(1).atStartOfDay(zone).toInstant(),
                month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(),
            )
            groupByDay(occ, zone)
        }
        provideContent { GlanceTheme { Content(configured, showTitles, month, byDay) } }
    }

    @Composable
    private fun Content(configured: Boolean, showTitles: Boolean, month: YearMonth, byDay: Map<LocalDate, List<Occurrence>>) {
        val size = LocalSize.current
        val locale = Locale.getDefault()
        val today = LocalDate.now()

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(PAD),
        ) {
            // Header: previous | month name (tap = back to this month) | next
            Row(modifier = GlanceModifier.fillMaxWidth().height(HEADER_H), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", modifier = GlanceModifier.width(36.dp).clickable(actionRunCallback<MonthNavCallback>(nav(-1))),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp, textAlign = TextAlign.Center),
                )
                Text(
                    "${month.month.getDisplayName(DateTextStyle.FULL, locale)} ${month.year}",
                    modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<MonthNavCallback>(nav(0))),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    "›", modifier = GlanceModifier.width(36.dp).clickable(actionRunCallback<MonthNavCallback>(nav(1))),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp, textAlign = TextAlign.Center),
                )
            }

            if (!configured) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(openApp(null))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Tap to add your calendar feed", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp))
                }
                return@Column
            }

            val lead = month.atDay(1).dayOfWeek.value - 1 // Monday-first
            val weeks = ceil((lead + month.lengthOfMonth()) / 7.0).toInt()
            val cellW = ((size.width.value - PAD.value * 2) / 7f).dp
            val cellH = ((size.height.value - PAD.value * 2 - HEADER_H.value - WEEKDAY_H.value) / weeks).dp
            val titleLines = ((cellH.value - DAY_NUM_H.value) / LINE_H.value).toInt().coerceAtLeast(0)

            Row(modifier = GlanceModifier.fillMaxWidth().height(WEEKDAY_H)) {
                DayOfWeek.entries.forEach { d ->
                    Text(
                        d.getDisplayName(DateTextStyle.NARROW, locale),
                        modifier = GlanceModifier.width(cellW),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center),
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
                            DayCell(date, date == today, byDay[date].orEmpty(), showTitles, titleLines, cellW, cellH)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DayCell(
        date: LocalDate, isToday: Boolean, events: List<Occurrence>,
        showTitles: Boolean, titleLines: Int, w: Dp, h: Dp,
    ) {
        Column(
            modifier = GlanceModifier.width(w).height(h).padding(1.dp).clickable(actionStartActivity(openApp(date))),
        ) {
            Text(
                "${date.dayOfMonth}",
                modifier = GlanceModifier.fillMaxWidth().height(DAY_NUM_H),
                style = TextStyle(
                    color = if (isToday) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
            )
            if (events.isEmpty()) return@Column
            if (showTitles && titleLines > 0) {
                val overflow = events.size > titleLines
                events.take(if (overflow) titleLines - 1 else titleLines).forEach { e ->
                    Text(
                        e.summary.ifBlank { "·" }, maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.secondaryContainer),
                        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 8.sp),
                    )
                }
                if (overflow) {
                    Text(
                        "+${events.size - (titleLines - 1)}", maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 8.sp),
                    )
                }
            } else {
                // Titles are off (privacy default) or the cell is too small: show how many events there are.
                Text(
                    "•".repeat(events.size.coerceAtMost(3)),
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 9.sp, textAlign = TextAlign.Center),
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
        val DAY_NUM_H = 14.dp
        val LINE_H = 11.dp
    }
}

private val NavDelta = ActionParameters.Key<Int>("delta")

/** Prev / next / back-to-today. The chosen month is kept in plain app-private preferences. */
class MonthNavCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val store = SecureStore(context)
        val delta = parameters[NavDelta] ?: 0
        store.widgetMonthOffset = if (delta == 0) 0 else store.widgetMonthOffset + delta
        MonthWidget().update(context, glanceId)
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}
