package io.github.snow34.calendarwidget.google

import android.content.Context
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.EventDraft
import io.github.snow34.calendarwidget.core.EventDrafts
import io.github.snow34.calendarwidget.core.GoogleCalendarLinks
import io.github.snow34.calendarwidget.core.PendingEvent
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Adds an event to one of the user's Google calendars and shows it in the app right away. */
object GoogleEvents {
    /** The Google calendars an event can be added to: switched on, and added by their Google secret address. */
    fun writableCalendars(sources: List<CalendarSource>): List<CalendarSource> =
        sources.filter { it.enabled && GoogleCalendarLinks.calendarIdFromFeedUrl(it.url) != null }

    /** Returns null on success, or a message explaining what went wrong. */
    suspend fun add(context: Context, store: SecureStore, source: CalendarSource, draft: EventDraft): String? {
        EventDrafts.validate(draft)?.let { return it }
        val calendarId = GoogleCalendarLinks.calendarIdFromFeedUrl(source.url) ?: return "${source.name} is not a Google calendar"
        val token = when (val t = GoogleAuth.freshAccessToken(context, store)) {
            is GoogleAuth.TokenResult.Token -> t.accessToken
            is GoogleAuth.TokenResult.Error -> return t.message
        }
        val result = withContext(Dispatchers.IO) { GoogleCalendarApi.insert(token, calendarId, EventDrafts.toGoogleJson(draft)) }
        return when (result) {
            is GoogleCalendarApi.InsertResult.Failed -> result.message
            is GoogleCalendarApi.InsertResult.Inserted -> {
                store.addPendingEvent(
                    PendingEvent(
                        calendarId = source.id, uid = result.iCalUid, title = draft.title.trim(),
                        location = draft.location.trim().ifBlank { null }, allDay = draft.allDay,
                        start = draft.startInstant(), end = draft.endInstant(), createdAtMillis = System.currentTimeMillis(),
                    ),
                )
                Widgets.updateAll(context)
                null
            }
        }
    }
}
