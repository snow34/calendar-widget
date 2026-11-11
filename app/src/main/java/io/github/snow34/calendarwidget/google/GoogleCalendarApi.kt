package io.github.snow34.calendarwidget.google

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** The one Google Calendar API call the app makes: adding an event. */
object GoogleCalendarApi {
    sealed interface InsertResult {
        data class Inserted(val iCalUid: String) : InsertResult
        data class Failed(val message: String) : InsertResult
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Blocking; call on Dispatchers.IO. [eventJson] comes from `EventDrafts.toGoogleJson`. */
    fun insert(accessToken: String, calendarId: String, eventJson: String): InsertResult {
        val url = "https://www.googleapis.com/calendar/v3/calendars/" +
            URLEncoder.encode(calendarId, "UTF-8").replace("+", "%20") + "/events"
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(eventJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                when {
                    resp.isSuccessful -> try {
                        val uid = JSONObject(body).optString("iCalUID")
                        if (uid.isBlank()) InsertResult.Failed("Google saved the event but sent an unexpected answer") else InsertResult.Inserted(uid)
                    } catch (_: JSONException) {
                        InsertResult.Failed("Google saved the event but sent an unexpected answer")
                    }
                    resp.code == 401 -> InsertResult.Failed("Google sign-in has expired. Sign in again in Settings → Google account.")
                    resp.code == 403 -> InsertResult.Failed(
                        "Google refused. You can only add events to calendars you own or may edit. Also check that the " +
                            "Google Calendar API is switched on in your Google Cloud project.",
                    )
                    resp.code == 404 -> InsertResult.Failed("Google could not find this calendar.")
                    else -> InsertResult.Failed("Google Calendar error (HTTP ${resp.code})")
                }
            }
        } catch (_: IOException) {
            InsertResult.Failed("Network error. Check the connection and try again.")
        }
    }
}
