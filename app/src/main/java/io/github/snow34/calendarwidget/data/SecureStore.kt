package io.github.snow34.calendarwidget.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

/**
 * The secret feed URL and the cached events are encrypted at rest (see [Vault]).
 * Non-sensitive preferences live in plain app-private SharedPreferences.
 */
class SecureStore(context: Context) {
    private val dir: File = context.applicationContext.filesDir
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun hasUrl(): Boolean = File(dir, URL_FILE).exists()
    fun loadUrl(): String? = read(URL_FILE)
    fun saveUrl(url: String) = write(URL_FILE, url)

    fun loadIcs(): String? = read(ICS_FILE)
    fun saveIcs(text: String) = write(ICS_FILE, text)

    /** Removes the URL and every cached event. */
    fun wipe() {
        File(dir, URL_FILE).delete()
        File(dir, ICS_FILE).delete()
        prefs.edit().remove(KEY_ETAG).remove(KEY_LAST_SYNC).apply()
    }

    var etag: String?
        get() = prefs.getString(KEY_ETAG, null)
        set(v) = prefs.edit().putString(KEY_ETAG, v).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_SYNC, v).apply()

    var syncIntervalHours: Int
        get() = prefs.getInt(KEY_INTERVAL, 3)
        set(v) = prefs.edit().putInt(KEY_INTERVAL, v).apply()

    /** When false the widget shows times only, so event titles never appear on the home screen. */
    var widgetShowTitles: Boolean
        get() = prefs.getBoolean(KEY_WIDGET_TITLES, false)
        set(v) = prefs.edit().putBoolean(KEY_WIDGET_TITLES, v).apply()

    /** Months away from the current one that the month widget is showing (0 = this month). */
    var widgetMonthOffset: Int
        get() = prefs.getInt(KEY_MONTH_OFFSET, 0)
        set(v) = prefs.edit().putInt(KEY_MONTH_OFFSET, v.coerceIn(-120, 120)).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(v) = prefs.edit().putBoolean(KEY_APP_LOCK, v).apply()

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

    private companion object {
        const val URL_FILE = "feed_url.enc"
        const val ICS_FILE = "events.ics.enc"
        const val KEY_ETAG = "etag"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_INTERVAL = "interval_hours"
        const val KEY_WIDGET_TITLES = "widget_titles"
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_MONTH_OFFSET = "widget_month_offset"
    }
}
