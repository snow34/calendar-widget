# calendar-widget

A private, read-only Android calendar app and home-screen widget for Google Calendar,
built so that nothing on the phone is exposed to Google beyond the calendar fetch itself.

## How it works

- You paste your calendar's **secret iCal address** once (Google Calendar → Settings → your calendar →
  *Integrate calendar* → *Secret address in iCal format*). No Google sign-in, OAuth or Google SDK is involved.
- A background job downloads the `.ics` feed over HTTPS and stores it encrypted (AES-256-GCM, non-exportable
  Android Keystore key). The URL is stored the same way.
- The app (agenda + month views) and the widget read the encrypted cache. Recurring events, exceptions and
  time zones are expanded by the `core` module.

## Privacy model

| | |
|---|---|
| Permissions requested by the app | `INTERNET` only (WorkManager's library manifest adds scheduling permissions) |
| Google/Firebase/Play Services code | None; `./gradlew :app:verifyNoGoogleServices` fails the build if any appears |
| Network | HTTPS only, system CAs only, no cleartext, no redirects to http |
| Backups | Disabled; data extraction rules exclude everything |
| Screen | `FLAG_SECURE` (no screenshots / recents thumbnail) |
| Widget | Shows times only by default; titles are opt-in |
| App lock | Optional biometric / device credential |
| Logging | The feed URL and event contents are never logged |

What this does **not** hide: Google's servers still see that your feed URL is being fetched, because the
calendar lives there. Anyone holding the secret URL can read the calendar; reset it in Google Calendar
settings if it leaks. The OS is outside the app's control (this project assumes a de-Googled OS).

## Layout

- `core/` – pure Kotlin/JVM: ICS parser + recurrence expander (RRULE, EXDATE, RECURRENCE-ID, DST-safe). Unit tested.
- `app/` – Android app (Jetpack Compose UI, Glance widget, WorkManager sync, Keystore storage). Min SDK 30.

## Build

```bash
./gradlew :core:test            # JVM tests, no Android SDK needed
```

With an Android SDK (`ANDROID_HOME` set or `local.properties` present) the `:app` module is included too:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lint :app:verifyNoGoogleServices
```
