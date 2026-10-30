package io.github.snow34.calendarwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.snow34.calendarwidget.core.Occurrence
import io.github.snow34.calendarwidget.data.CalendarRepository
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.ui.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class CalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = SecureStore(context)
        val configured = store.hasUrl()
        val showTitles = store.widgetShowTitles
        val items = withContext(Dispatchers.Default) {
            val now = Instant.now()
            CalendarRepository(store).occurrences(now.minus(1, ChronoUnit.HOURS), now.plus(14, ChronoUnit.DAYS))
                .filter { it.end.isAfter(now) }
                .take(MAX_ITEMS)
        }
        provideContent { GlanceTheme { Content(configured, showTitles, items) } }
    }

    @Composable
    private fun Content(configured: Boolean, showTitles: Boolean, items: List<Occurrence>) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp)
                .clickable(actionStartActivity(Widgets.openAppIntent())),
        ) {
            when {
                !configured -> Line("Tap to add your calendar feed", bold = false)
                items.isEmpty() -> Line("Nothing coming up", bold = false)
                else -> items.forEach { o ->
                    val title = if (showTitles) o.summary.ifBlank { "(no title)" } else "Event"
                    Line("${Format.widgetWhen(o)}  $title", bold = false)
                    Spacer(GlanceModifier.height(4.dp))
                }
            }
        }
    }

    @Composable
    private fun Line(text: String, bold: Boolean) {
        Text(
            text = text,
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }

    private companion object { const val MAX_ITEMS = 6 }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}
