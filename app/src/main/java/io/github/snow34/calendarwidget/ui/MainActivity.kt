package io.github.snow34.calendarwidget.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.snow34.calendarwidget.data.SecureStore
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MainActivity : FragmentActivity() {
    private lateinit var store: SecureStore
    private var unlocked by mutableStateOf(false)
    private var focus by mutableStateOf<DayFocus?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hides content from screenshots, screen recording and the recents thumbnail.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        store = SecureStore(this)
        focus = dayFrom(intent)?.let(::DayFocus)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (unlocked || !store.appLockEnabled) {
                        CalendarApp(store, focus)
                    } else {
                        LockedScreen(onUnlock = ::authenticate)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        focus = dayFrom(intent)?.let(::DayFocus)
    }

    /** The month widget opens `calendarwidget://day/2025-03-14`. Anything else is ignored. */
    private fun dayFrom(intent: Intent?): LocalDate? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "calendarwidget" || uri.host != "day") return null
        return try { LocalDate.parse(uri.lastPathSegment) } catch (_: DateTimeParseException) { null }
    }

    override fun onStart() {
        super.onStart()
        if (store.appLockEnabled && !unlocked) authenticate()
    }

    override fun onStop() {
        super.onStop()
        unlocked = false
    }

    private fun authenticate() {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Lock was enabled but the device has no screen lock any more; fail closed rather than open.
            return
        }
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock calendar")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}

@androidx.compose.runtime.Composable
private fun LockedScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Calendar locked", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onUnlock, modifier = Modifier.padding(top = 16.dp)) { Text("Unlock") }
    }
}
