package io.github.snow34.calendarwidget.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads the iCal feed. The URL is a bearer secret, so nothing here logs it or puts it in an error message.
 */
class IcsFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followSslRedirects(false) // never follow a redirect from https to http
        .build(),
) {
    sealed interface Result {
        data class Updated(val text: String, val etag: String?) : Result
        data object NotModified : Result
        data class Failed(val reason: String, val retryable: Boolean) : Result
    }

    fun fetch(url: String, etag: String?): Result {
        val parsed = parseHttpsUrl(url) ?: return Result.Failed("Feed URL must be a valid https:// address", false)
        val req = Request.Builder().url(parsed)
            .header("Accept", "text/calendar, */*;q=0.1")
            .apply { if (etag != null) header("If-None-Match", etag) }
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                when {
                    resp.code == 304 -> Result.NotModified
                    resp.code in 500..599 || resp.code == 429 -> Result.Failed("Server busy (HTTP ${resp.code})", true)
                    !resp.isSuccessful -> Result.Failed("Feed rejected (HTTP ${resp.code}); the URL may have been reset", false)
                    else -> {
                        val source = resp.body?.source() ?: return@use Result.Failed("Empty response", true)
                        if (source.request(MAX_BYTES + 1)) {
                            Result.Failed("Feed is larger than ${MAX_BYTES / (1024 * 1024)} MiB", false)
                        } else {
                            val text = source.readUtf8()
                            if (!text.contains("BEGIN:VCALENDAR")) Result.Failed("Response is not an iCal feed", false)
                            else Result.Updated(text, resp.header("ETag"))
                        }
                    }
                }
            }
        } catch (_: IOException) {
            Result.Failed("Network error", true)
        }
    }

    companion object {
        const val MAX_BYTES = 10L * 1024 * 1024

        fun parseHttpsUrl(url: String): HttpUrl? =
            url.trim().toHttpUrlOrNull()?.takeIf { it.isHttps }
    }
}
