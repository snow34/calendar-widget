package io.github.snow34.calendarwidget.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.snow34.calendarwidget.core.Appearance
import io.github.snow34.calendarwidget.core.AppearancePresets
import io.github.snow34.calendarwidget.core.SettingsSummary
import io.github.snow34.calendarwidget.data.SecureStore
import io.github.snow34.calendarwidget.google.GoogleAuth
import io.github.snow34.calendarwidget.sync.SyncWorker
import io.github.snow34.calendarwidget.widget.Widgets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SECTION_APPEARANCE = "appearance"
private const val SECTION_WIDGETS = "widgets"
private const val SECTION_SYNC = "sync"
private const val SECTION_SECURITY = "security"
private const val SECTION_GOOGLE = "google"

@Composable
internal fun SettingsScreen(
    store: SecureStore, context: Context, appearance: Appearance, onAppearanceChange: (Appearance) -> Unit, onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var interval by remember { mutableIntStateOf(store.syncIntervalHours) }
    var colorMode by remember { mutableStateOf(store.colorMode) }
    var titles by remember { mutableStateOf(store.widgetShowTitles) }
    var opacity by remember { mutableFloatStateOf(store.widgetOpacityPercent.toFloat()) }
    var lock by remember { mutableStateOf(store.appLockEnabled) }
    var clientId by remember { mutableStateOf(store.googleClientId.orEmpty()) }
    var signedIn by remember { mutableStateOf(store.hasGoogleAuth()) }
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            status = GoogleAuth.completeSignIn(context, store, result.data)
            signedIn = store.hasGoogleAuth()
        }
    }
    // Open sections: all start closed; several can be open at once.
    var openSections by rememberSaveable { mutableStateOf(emptySet<String>()) }

    fun toggleSection(id: String) { openSections = if (id in openSections) openSections - id else openSections + id }
    fun refreshWidgets() { scope.launch { Widgets.updateAll(context) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Feedback (e.g. the screen-lock warning) stays visible at the top even when every section is closed.
        if (status != null) {
            item(key = "status") {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(status.orEmpty(), Modifier.weight(1f).padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { status = null }) { Text("OK") }
                    }
                }
            }
        }

        item(key = SECTION_APPEARANCE) {
            SettingsSection(
                title = "Appearance",
                summary = SettingsSummary.appearance(appearance.themeMode, appearance.textScale, AppearancePresets.matching(appearance.palette)?.name),
                expanded = SECTION_APPEARANCE in openSections,
                onToggle = { toggleSection(SECTION_APPEARANCE) },
            ) {
                AppearanceSection(
                    appearance = appearance,
                    colorMode = colorMode,
                    onColorModeChange = { mode ->
                        colorMode = mode; store.colorMode = mode
                        onChanged(); refreshWidgets()
                    },
                ) { updated ->
                    store.appearance = updated
                    onAppearanceChange(updated)
                    onChanged()
                    refreshWidgets()
                }
            }
        }

        item(key = SECTION_WIDGETS) {
            SettingsSection(
                title = "Widgets",
                summary = SettingsSummary.widgets(titles, opacity.roundToInt()),
                expanded = SECTION_WIDGETS in openSections,
                onToggle = { toggleSection(SECTION_WIDGETS) },
            ) {
                ToggleRow("Show event titles on the widget", titles) {
                    titles = it; store.widgetShowTitles = it
                    refreshWidgets()
                }
                Text("Widget background: ${opacity.roundToInt()}%")
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0f..100f,
                    steps = 19, // 5% steps
                    // Save and redraw once when the finger lifts, not on every tick of the drag.
                    onValueChangeFinished = {
                        store.widgetOpacityPercent = opacity.roundToInt()
                        refreshWidgets()
                    },
                )
                Text("0% is see-through, 100% is solid. Event bars and text always stay solid.", style = MaterialTheme.typography.bodySmall)
            }
        }

        item(key = SECTION_SYNC) {
            SettingsSection(
                title = "Sync",
                summary = SettingsSummary.sync(interval),
                expanded = SECTION_SYNC in openSections,
                onToggle = { toggleSection(SECTION_SYNC) },
            ) {
                Text("Sync every", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 6, 12).forEach { h ->
                        FilterChip(
                            selected = interval == h,
                            onClick = { interval = h; store.syncIntervalHours = h; if (store.hasCalendars()) SyncWorker.schedule(context, h) },
                            label = { Text("${h}h") },
                        )
                    }
                }
            }
        }

        item(key = SECTION_SECURITY) {
            SettingsSection(
                title = "Security",
                summary = SettingsSummary.security(lock),
                expanded = SECTION_SECURITY in openSections,
                onToggle = { toggleSection(SECTION_SECURITY) },
            ) {
                ToggleRow("Require fingerprint / screen lock to open the app", lock) { want ->
                    val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    if (want && !ok) status = "Set up a screen lock on the device first" else { lock = want; store.appLockEnabled = want }
                }
            }
        }

        item(key = SECTION_GOOGLE) {
            SettingsSection(
                title = "Google account",
                summary = SettingsSummary.googleAccount(store.googleClientId != null, signedIn),
                expanded = SECTION_GOOGLE in openSections,
                onToggle = { toggleSection(SECTION_GOOGLE) },
            ) {
                Text(
                    "Optional. Sign in once so \"+ Add event\" saves new events straight to your Google calendars, without " +
                        "opening Google Calendar. The app only gets permission for calendar events (not e-mail, contacts or files). " +
                        "You first need your own free Google Cloud \"client ID\": see Part 7 of the guide (README).",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (signedIn) {
                    Text("Signed in. New events are saved straight to Google Calendar.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        scope.launch {
                            GoogleAuth.signOut(store)
                            signedIn = false
                            status = "Signed out of Google. \"+ Add event\" opens Google Calendar again."
                        }
                    }) { Text("Sign out") }
                } else {
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it.trim() },
                        label = { Text("OAuth client ID (…apps.googleusercontent.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = clientId.endsWith(".apps.googleusercontent.com"),
                        onClick = {
                            store.googleClientId = clientId
                            signInLauncher.launch(GoogleAuth.signInIntent(context, clientId))
                        },
                    ) { Text("Sign in with Google") }
                }
            }
        }

        item(key = "note") {
            Text(
                "Editing and deleting events opens Google Calendar in your web browser. Adding does too, unless you sign in " +
                    "under Google account above. Google refreshes the private calendar address slowly, so a change can take a few hours to appear here.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
