package io.github.snow34.calendarwidget.sync

import android.content.Context
import io.github.snow34.calendarwidget.data.IcsFetcher
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SyncOutcome {
    data object Updated : SyncOutcome
    data object NotModified : SyncOutcome
    data object NoUrl : SyncOutcome
    data class Failed(val reason: String, val retryable: Boolean) : SyncOutcome
}

/** One fetch -> encrypt -> cache -> refresh-widget pass, shared by the UI and the background worker. */
object Syncer {
    suspend fun run(context: Context): SyncOutcome {
        val app = context.applicationContext
        val store = SecureStore(app)
        val outcome = withContext(Dispatchers.IO) {
            val url = store.loadUrl() ?: return@withContext SyncOutcome.NoUrl
            when (val r = IcsFetcher().fetch(url, store.etag)) {
                is IcsFetcher.Result.Updated -> {
                    store.saveIcs(r.text)
                    store.etag = r.etag
                    store.lastSyncMillis = System.currentTimeMillis()
                    SyncOutcome.Updated
                }
                IcsFetcher.Result.NotModified -> {
                    store.lastSyncMillis = System.currentTimeMillis()
                    SyncOutcome.NotModified
                }
                is IcsFetcher.Result.Failed -> SyncOutcome.Failed(r.reason, r.retryable)
            }
        }
        if (outcome == SyncOutcome.Updated || outcome == SyncOutcome.NotModified) Widgets.updateAll(app)
        return outcome
    }
}
