package io.github.snow34.calendarwidget.sync

import android.content.Context
import io.github.snow34.calendarwidget.data.IcsFetcher
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncFailure(val calendarName: String, val reason: String, val retryable: Boolean)

/** Outcome of one pass over all switched-on calendars. A calendar that fails never blocks the others. */
data class SyncResult(val calendars: Int, val updated: Int, val unchanged: Int, val failures: List<SyncFailure>) {
    /** One line for the Settings screen. */
    fun summary(): String {
        if (calendars == 0) return "No calendar is switched on"
        val ok = when {
            updated > 0 -> "Synced $updated calendar${if (updated == 1) "" else "s"}"
            unchanged > 0 -> "Already up to date"
            else -> null
        }
        if (failures.isEmpty()) return ok.orEmpty()
        val problems = failures.joinToString("; ") { "${it.calendarName}: ${it.reason}" }
        return if (ok == null) "Could not sync: $problems" else "$ok. Could not sync: $problems"
    }
}

/** Fetch -> encrypt -> cache for every switched-on calendar, then refresh the widgets. Shared by the UI and the worker. */
object Syncer {
    suspend fun run(context: Context): SyncResult {
        val app = context.applicationContext
        val store = SecureStore(app)
        val fetcher = IcsFetcher()
        var updated = 0
        var unchanged = 0
        val failures = mutableListOf<SyncFailure>()
        val active = store.loadCalendars().filter { it.enabled }

        withContext(Dispatchers.IO) {
            for (cal in active) {
                when (val r = fetcher.fetch(cal.url, store.etag(cal.id))) {
                    is IcsFetcher.Result.Updated -> {
                        store.saveIcs(cal.id, r.text)
                        store.setEtag(cal.id, r.etag)
                        store.setLastSyncMillis(cal.id, System.currentTimeMillis())
                        updated++
                    }
                    IcsFetcher.Result.NotModified -> {
                        store.setLastSyncMillis(cal.id, System.currentTimeMillis())
                        unchanged++
                    }
                    is IcsFetcher.Result.Failed -> failures += SyncFailure(cal.name, r.reason, r.retryable)
                }
            }
        }
        if (updated + unchanged > 0) Widgets.updateAll(app)
        return SyncResult(active.size, updated, unchanged, failures)
    }
}
