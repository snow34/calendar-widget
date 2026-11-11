package io.github.snow34.calendarwidget.google

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.snow34.calendarwidget.data.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Signing in to Google, only so the app can add events. Uses the phone's browser (AppAuth, PKCE), so no Google Play
 * Services are needed. The resulting sign-in state, including the refresh token, is stored encrypted by [SecureStore].
 * Each user brings their own OAuth client ID from their own Google Cloud project (see README "Part 7").
 */
object GoogleAuth {
    /** Only event read/write on the user's calendars; no access to Gmail, contacts, Drive or other Google data. */
    const val SCOPE = "https://www.googleapis.com/auth/calendar.events"
    const val REDIRECT_URI = "io.github.snow34.calendarwidget:/oauth2redirect"

    private val config = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    sealed interface TokenResult {
        data class Token(val accessToken: String) : TokenResult
        data class Error(val message: String) : TokenResult
    }

    /** Intent that opens Google's sign-in page in the browser; pass the activity result to [completeSignIn]. */
    fun signInIntent(context: Context, clientId: String): Intent {
        val request = AuthorizationRequest.Builder(config, clientId.trim(), ResponseTypeValues.CODE, Uri.parse(REDIRECT_URI))
            .setScopes(SCOPE)
            .setPrompt("consent") // always hand out a refresh token, also when signing in again
            .setAdditionalParameters(mapOf("access_type" to "offline"))
            .build()
        val service = AuthorizationService(context)
        try {
            return service.getAuthorizationRequestIntent(request)
        } finally {
            service.dispose()
        }
    }

    /** Finishes sign-in from the browser's answer. Returns a message for the user. */
    suspend fun completeSignIn(context: Context, store: SecureStore, data: Intent?): String = withContext(Dispatchers.Main) {
        if (data == null) return@withContext "Sign-in was cancelled"
        val response = AuthorizationResponse.fromIntent(data)
        val error = AuthorizationException.fromIntent(data)
        if (response == null) return@withContext "Sign-in failed: ${describe(error)}"
        val state = AuthState(response, error)
        val service = AuthorizationService(context)
        try {
            val tokenError = suspendCancellableCoroutine { cont ->
                service.performTokenRequest(response.createTokenExchangeRequest()) { tokens, ex ->
                    state.update(tokens, ex)
                    cont.resume(ex)
                }
            }
            if (tokenError != null || state.refreshToken == null) {
                return@withContext "Sign-in failed: ${describe(tokenError)}"
            }
            store.saveGoogleAuth(state.jsonSerializeString())
            "Signed in to Google. New events are now saved straight to Google Calendar."
        } finally {
            service.dispose()
        }
    }

    /** A valid access token, refreshing it when needed. */
    suspend fun freshAccessToken(context: Context, store: SecureStore): TokenResult = withContext(Dispatchers.Main) {
        val json = store.loadGoogleAuth() ?: return@withContext TokenResult.Error("Not signed in to Google (Settings → Google account)")
        val state = try {
            AuthState.jsonDeserialize(json)
        } catch (_: JSONException) {
            return@withContext TokenResult.Error("Google sign-in data is damaged. Sign in again in Settings → Google account.")
        }
        val service = AuthorizationService(context)
        try {
            suspendCancellableCoroutine { cont ->
                state.performActionWithFreshTokens(service) { accessToken, _, ex ->
                    cont.resume(
                        if (ex == null && accessToken != null) TokenResult.Token(accessToken)
                        else TokenResult.Error("Google sign-in has expired or was withdrawn. Sign in again in Settings → Google account."),
                    )
                }
            }
        } finally {
            store.saveGoogleAuth(state.jsonSerializeString()) // keeps a refreshed token
            service.dispose()
        }
    }

    /** Forgets the sign-in on this phone and asks Google to cancel the permission. */
    suspend fun signOut(store: SecureStore) {
        val refreshToken = store.loadGoogleAuth()?.let {
            try { AuthState.jsonDeserialize(it).refreshToken } catch (_: JSONException) { null }
        }
        store.clearGoogleAuth()
        if (refreshToken != null) {
            withContext(Dispatchers.IO) {
                try {
                    OkHttpClient().newCall(
                        Request.Builder().url("https://oauth2.googleapis.com/revoke")
                            .post(FormBody.Builder().add("token", refreshToken).build()).build(),
                    ).execute().close()
                } catch (_: IOException) {
                    // Offline: the permission can also be removed at myaccount.google.com → Security → Third-party access.
                }
            }
        }
    }

    private fun describe(e: AuthorizationException?): String = when {
        e == null -> "no answer from Google"
        e.code == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.code -> "cancelled"
        else -> e.errorDescription ?: e.error ?: "error ${e.code}"
    }
}
