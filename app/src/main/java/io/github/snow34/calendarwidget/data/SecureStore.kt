package io.github.snow34.calendarwidget.data

import android.content.Context
import android.util.AtomicFile
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.CalendarSource
import io.github.snow34.calendarwidget.core.CalendarSources
import io.github.snow34.calendarwidget.core.ColorMode
import io.github.snow34.calendarwidget.core.EventColors
import io.github.snow34.calendarwidget.core.PendingEvent
import io.github.snow34.calendarwidget.core.WidgetOpacity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * The list of calendars (including their secret feed addresses) and each calendar's cached events are encrypted
 * at rest (see [Vault]). Non-sensitive preferences live in plain app-private SharedPreferences.
 */
class SecureStore(context: Context) {
    private val dir: File = context.applicationContext.filesDir
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init { migrateSingleCalendarInstall() }

    // ---- calendars ----

    fun hasCalendars(): Boolean = loadCalendars().isNotEmpty()

    fun loadCalendars(): List<CalendarSource> = synchronized(LOCK) {
        read(CALENDARS_FILE)?.let(::parseCalendars).orEmpty()
    }

    /** Adds a calendar and returns it, or null when the limit is reached or the address is already in the list. */
    fun addCalendar(name: String, feedUrl: String): CalendarSource? = synchronized(LOCK) {
        val list = loadCalendars()
        val url = CalendarSources.normalizeFeedUrl(feedUrl)
        if (list.size >= CalendarSources.MAX_CALENDARS || list.any { it.url == url }) return null
        val names = list.map { it.name }
        val chosen = CalendarSources.uniqueName(
            CalendarSources.cleanName(name, CalendarSources.suggestName(url, names)), names,
        )
        val added = CalendarSource(UUID.randomUUID().toString(), chosen, url, CalendarSources.nextColorIndex(list), true)
        saveCalendars(list + added)
        added
    }

    fun updateCalendar(id: String, change: (CalendarSource) -> CalendarSource) = synchronized(LOCK) {
        saveCalendars(loadCalendars().map { if (it.id == id) change(it) else it })
    }

    fun renameCalendar(id: String, newName: String) = synchronized(LOCK) {
        val others = loadCalendars().filter { it.id != id }.map { it.name }
        updateCalendar(id) { it.copy(name = CalendarSources.uniqueName(CalendarSources.cleanName(newName, it.name), others)) }
    }

    /** Removes the calendar, its address and its cached events from this phone. */
    fun removeCalendar(id: String) = synchronized(LOCK) {
        saveCalendars(loadCalendars().filterNot { it.id == id })
        File(dir, icsFile(id)).delete()
        prefs.edit().remove(etagKey(id)).remove(lastSyncKey(id)).apply()
        savePendingEvents(loadPendingEvents().filterNot { it.calendarId == id })
    }

    // ---- Google account (only for adding events in the app) ----

    /** The OAuth client ID from the user's own Google Cloud project. Not a secret (it identifies the app, not the user). */
    var googleClientId: String?
        get() = prefs.getString(KEY_GOOGLE_CLIENT_ID, null)?.takeIf { it.isNotBlank() }
        set(v) = prefs.edit().putString(KEY_GOOGLE_CLIENT_ID, v?.trim()).apply()

    fun hasGoogleAuth(): Boolean = File(dir, GOOGLE_AUTH_FILE).exists()

    /** The serialized sign-in state (includes the refresh token), stored encrypted. */
    fun loadGoogleAuth(): String? = read(GOOGLE_AUTH_FILE)
    fun saveGoogleAuth(json: String) = write(GOOGLE_AUTH_FILE, json)
    fun clearGoogleAuth() { File(dir, GOOGLE_AUTH_FILE).delete() }

    // ---- events added in the app that the feed does not show yet ----

    fun loadPendingEvents(): List<PendingEvent> = synchronized(LOCK) {
        val text = read(PENDING_FILE) ?: return emptyList()
        try {
            val arr = JSONArray(text)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                PendingEvent(
                    calendarId = o.getString("calendar"),
                    uid = o.getString("uid"),
                    title = o.optString("title"),
                    location = o.optString("location").ifBlank { null },
                    allDay = o.optBoolean("allDay"),
                    start = Instant.ofEpochMilli(o.getLong("start")),
                    end = Instant.ofEpochMilli(o.getLong("end")),
                    createdAtMillis = o.getLong("created"),
                )
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun savePendingEvents(list: List<PendingEvent>) = synchronized(LOCK) {
        if (list.isEmpty()) { File(dir, PENDING_FILE).delete(); return@synchronized }
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("calendar", it.calendarId).put("uid", it.uid).put("title", it.title)
                    .put("location", it.location ?: "").put("allDay", it.allDay)
                    .put("start", it.start.toEpochMilli()).put("end", it.end.toEpochMilli()).put("created", it.createdAtMillis),
            )
        }
        write(PENDING_FILE, arr.toString())
    }

    fun addPendingEvent(event: PendingEvent) = synchronized(LOCK) { savePendingEvents(loadPendingEvents() + event) }

    // ---- per-calendar cache and sync state ----

    fun loadIcs(calendarId: String): String? = if (isSafeId(calendarId)) read(icsFile(calendarId)) else null
    fun saveIcs(calendarId: String, text: String) { if (isSafeId(calendarId)) write(icsFile(calendarId), text) }

    fun etag(calendarId: String): String? = prefs.getString(etagKey(calendarId), null)
    fun setEtag(calendarId: String, etag: String?) = prefs.edit().putString(etagKey(calendarId), etag).apply()

    fun lastSyncMillis(calendarId: String): Long = prefs.getLong(lastSyncKey(calendarId), 0L)
    fun setLastSyncMillis(calendarId: String, millis: Long) = prefs.edit().putLong(lastSyncKey(calendarId), millis).apply()

    // ---- preferences ----

    var syncIntervalHours: Int
        get() = prefs.getInt(KEY_INTERVAL, 3)
        set(v) = prefs.edit().putInt(KEY_INTERVAL, v).apply()

    /** Everything about how the app and widgets look (theme, text size, font, bar height, event colors). Not secret. */
    var appearance: Appearance
        get() = Appearance.parse(prefs.getString(KEY_APPEARANCE, null))
        set(v) = prefs.edit().putString(KEY_APPEARANCE, v.encode()).apply()

    /** What decides an event's color: its calendar or its title. */
    var colorMode: ColorMode
        get() = ColorMode.parse(prefs.getString(KEY_COLOR_MODE, null)) ?: ColorMode.CALENDAR
        set(v) = prefs.edit().putString(KEY_COLOR_MODE, v.name).apply()

    /** When false the widgets hide event titles (month widget shows plain colored stripes), e.g. for privacy on a locked screen. */
    var widgetShowTitles: Boolean
        get() = prefs.getBoolean(KEY_WIDGET_TITLES, true)
        set(v) = prefs.edit().putBoolean(KEY_WIDGET_TITLES, v).apply()

    /** Months away from the current one that the month widget is showing (0 = this month). */
    var widgetMonthOffset: Int
        get() = prefs.getInt(KEY_MONTH_OFFSET, 0)
        set(v) = prefs.edit().putInt(KEY_MONTH_OFFSET, v.coerceIn(-120, 120)).apply()

    /** How solid the widget background is: 0 = fully see-through, 100 = solid (event bars and text are unaffected). */
    var widgetOpacityPercent: Int
        get() = WidgetOpacity.clampPercent(prefs.getInt(KEY_WIDGET_OPACITY, WidgetOpacity.DEFAULT_PERCENT))
        set(v) = prefs.edit().putInt(KEY_WIDGET_OPACITY, WidgetOpacity.clampPercent(v)).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(v) = prefs.edit().putBoolean(KEY_APP_LOCK, v).apply()

    // ---- storage ----

    private fun saveCalendars(list: List<CalendarSource>) = write(CALENDARS_FILE, toJson(list))

    private fun toJson(list: List<CalendarSource>): String = JSONArray().also { arr ->
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url).put("color", it.colorIndex).put("enabled", it.enabled))
        }
    }.toString()

    private fun parseCalendars(text: String): List<CalendarSource> = try {
        val arr = JSONArray(text)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id")
            val url = o.optString("url")
            if (!isSafeId(id) || url.isBlank()) null
            else CalendarSource(
                id = id,
                name = o.optString("name", "Calendar").ifBlank { "Calendar" },
                url = url,
                colorIndex = Math.floorMod(o.optInt("color", 0), EventColors.PALETTE_SIZE),
                enabled = o.optBoolean("enabled", true),
            )
        }
    } catch (_: JSONException) {
        emptyList()
    }

    /**
     * Earlier versions kept one feed URL ("feed_url.enc") and one cache ("events.ics.enc"). Turn that into the first
     * entry of the calendar list, and keep coloring by title so the look doesn't change for an existing install.
     */
    private fun migrateSingleCalendarInstall() = synchronized(LOCK) {
        if (File(dir, CALENDARS_FILE).exists()) return@synchronized
        val legacyUrl = read(LEGACY_URL_FILE) ?: return@synchronized
        val url = CalendarSources.normalizeFeedUrl(legacyUrl)
        val first = CalendarSource(LEGACY_ID, CalendarSources.suggestName(url, emptyList()), url, 0, true)
        File(dir, LEGACY_ICS_FILE).let { if (it.exists()) it.renameTo(File(dir, icsFile(LEGACY_ID))) }
        prefs.edit()
            .putString(etagKey(LEGACY_ID), prefs.getString(KEY_LEGACY_ETAG, null))
            .putLong(lastSyncKey(LEGACY_ID), prefs.getLong(KEY_LEGACY_LAST_SYNC, 0L))
            .remove(KEY_LEGACY_ETAG).remove(KEY_LEGACY_LAST_SYNC)
            .putString(KEY_COLOR_MODE, ColorMode.TITLE.name)
            .apply()
        saveCalendars(listOf(first))
        File(dir, LEGACY_URL_FILE).delete()
    }

    private fun read(name: String): String? {
        val f = AtomicFile(File(dir, name))
        val bytes = try { f.readFully() } catch (_: java.io.IOException) { return null }
        return Vault.decrypt(bytes)?.toString(Charsets.UTF_8)
    }

    private fun write(name: String, text: String) {
        val f = AtomicFile(File(dir, name))
        val out = f.startWrite()
        try {
            out.write(Vault.encrypt(text.toByteArray(Charsets.UTF_8)))
            f.finishWrite(out)
        } catch (e: Exception) {
            f.failWrite(out)
            throw e
        }
    }

    private fun icsFile(calendarId: String) = "events-$calendarId.ics.enc"
    private fun etagKey(calendarId: String) = "etag_$calendarId"
    private fun lastSyncKey(calendarId: String) = "last_sync_$calendarId"

    /** Calendar ids become file names, so only plain characters are accepted. */
    private fun isSafeId(id: String) = SAFE_ID.matches(id)

    private companion object {
        val LOCK = Any()
        val SAFE_ID = Regex("[A-Za-z0-9-]{1,64}")
        const val CALENDARS_FILE = "calendars.json.enc"
        const val GOOGLE_AUTH_FILE = "google_auth.json.enc"
        const val PENDING_FILE = "pending_events.json.enc"
        const val KEY_GOOGLE_CLIENT_ID = "google_client_id"
        const val LEGACY_ID = "main"
        const val LEGACY_URL_FILE = "feed_url.enc"
        const val LEGACY_ICS_FILE = "events.ics.enc"
        const val KEY_LEGACY_ETAG = "etag"
        const val KEY_LEGACY_LAST_SYNC = "last_sync"
        const val KEY_INTERVAL = "interval_hours"
        const val KEY_COLOR_MODE = "color_mode"
        const val KEY_APPEARANCE = "appearance_v1"
        const val KEY_WIDGET_TITLES = "widget_titles"
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_MONTH_OFFSET = "widget_month_offset"
        const val KEY_WIDGET_OPACITY = "widget_opacity_percent"
    }
}
