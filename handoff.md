# Handoff: Private Calendar (calendar-widget)

For the next AI agent. Read this first, check the work with the commands below, then continue from **Open items**.

> **Standing instruction from the user:** update this file after **every** change (status, changelog, open items,
> anything learned) and commit it together with the code change. See "How to keep this file current".

- Repo: `snow34/calendar-widget`. Work branch: **`claude/lucid-planck-vy2q5k`** (push only here; do **not** open a PR unless the user asks).
- Last updated: 2026-11-03, after commit `7bce348` ("App: follow the system light/dark setting").

---

## 1. What this is

A private, read-only Android calendar app + two home-screen widgets for **Google Calendar**, built so Google gets
nothing from the phone beyond the calendar fetch itself.

- Data source: Google Calendar's **"Secret address in iCal format"** URL, pasted once by the user. No Google sign-in,
  no OAuth, no Google SDKs, no Play Services (user runs a de-Googled Android OS).
- Features: Agenda tab, Month tab (colored titled bars per day), Settings tab; **Month widget** (grid with colored
  event bars) and **Upcoming widget** (short list); optional biometric app lock; widget background transparency slider.
- Honest limits (also in README): Google still sees the feed URL being fetched; anyone with the secret URL can read
  the calendar; the OS itself is out of the app's control.

## 2. About the user (affects how you communicate)

- Almost no technical knowledge; **Windows** PC, **Android Studio**, de-Googled phone. Explain steps one action at a time.
- Preferences: **ask clarifying questions, never assume.** The user has answered many design questions via AskUserQuestion; keep doing that for real forks.
- They test on an Android Studio emulator ("Medium Phone API 37") and a real phone, then report back (screenshots, pasted errors).
- They build the APK themselves in Android Studio (debug APK, no signing).

## 3. Repo layout

```
core/   pure Kotlin/JVM (no Android): ICS parsing + recurrence + helpers. Unit tested. Build/test anywhere.
app/    Android app (Compose UI, Glance widgets, WorkManager sync, Keystore storage). Min SDK 30, target/compile 35.
README.md   beginner-friendly Windows guide (Parts 0-5) + privacy model + developer notes
handoff.md  this file
```

Key files:

| Area | Files |
|---|---|
| Parsing / recurrence | `core/.../IcsParser.kt`, `RecurrenceExpander.kt`, `Model.kt` (RRULE, EXDATE, RECURRENCE-ID, TZID/DST, all-day) |
| Per-day placement | `core/.../DayGrouping.kt` (`groupByDay`: multi-day events on every day, sorted all-day first, then start, then title) |
| Colors | `core/.../EventColors.kt` (8-color palette, FNV-1a hash of normalised title, WCAG contrast helper). Palette is mirrored in `app/src/main/res/drawable/bar_0..7.xml` (**keep in sync, same order**) |
| Opacity math | `core/.../WidgetOpacity.kt` |
| Storage / security | `app/.../data/Vault.kt` (AES-256-GCM, non-exportable Keystore key), `SecureStore.kt` (encrypted URL + cached ICS in `AtomicFile`s; plain prefs for non-secret settings) |
| Network | `app/.../data/IcsFetcher.kt` (https only, 10 MiB cap, ETag, no redirects https->http, never logs the URL) |
| Sync | `app/.../sync/Syncer.kt` (shared by UI and worker), `SyncWorker.kt` (WorkManager periodic) |
| UI | `app/.../ui/MainActivity.kt` (FLAG_SECURE, app lock, deep link `calendarwidget://day/<date>`), `CalendarApp.kt` (Agenda/Month/Settings), `AppTheme.kt`, `EventTint.kt`, `Format.kt` |
| Widgets | `app/.../widget/MonthWidget.kt`, `CalendarWidget.kt` (Upcoming), `Widgets.kt` (refresh bus + `openAppIntent`), `WidgetBackground.kt` |
| Hardening | `AndroidManifest.xml` (only `INTERNET`, `allowBackup=false`, no cleartext), `res/xml/network_security_config.xml` (system CAs only), `data_extraction_rules.xml` |
| Build | `settings.gradle.kts` (`:app` only included if an Android SDK is found), `gradle/libs.versions.toml`, `app/build.gradle.kts` (task `verifyNoGoogleServices`) |

## 4. Decisions made (and why)

- iCal secret URL instead of OAuth: simplest, most private, read-only. (User chose; they also chose view-only.)
- **Plan changed:** the original plan said Room + SQLCipher. Implemented instead as **one AES-GCM encrypted file** for the cached ICS (no native lib). Trade-off: whole file is parsed on every read.
- Own ICS parser/expander in `core` (no ical4j) to keep dependencies small and the logic unit-testable without Android.
- Event colors are **chosen automatically from the event title** because Google's private iCal feed has no event colors.
- Multi-day events: the same bar repeats on each day (user chose this over one spanning bar).
- Widget titles are **shown by default** (user chose this explicitly, accepting the privacy trade-off); one Settings switch hides them (Month then shows plain stripes, Upcoming shows "Event").
- Rounded widget bars use **drawable resources** (`ImageProvider(R.drawable.bar_N)`), not `cornerRadius()` (API 31+ only).
- Widgets read all data **inside the composition**, keyed on `Widgets.revision` (see Gotchas 3).
- App theme follows system light/dark; dynamic (wallpaper) colors on Android 12+, Material palettes below. Chosen by the agent without asking; the user may prefer plain palettes.
- Package name `io.github.snow34.calendarwidget` was **guessed** from the repo owner and not confirmed by the user.
- Debug APK only; no release signing configured.

## 5. Current status

| Item | State |
|---|---|
| `core` unit tests (24) | **Pass** (DayGrouping 6, EventColors 5, Expander 10, WidgetOpacity 3) |
| `:app` builds and runs | **Yes** on the user's emulator (user compiled it; no errors reported after the fixes below) |
| Month/Upcoming widgets, colors, bars, today circle | Seen working by the user on the emulator |
| Widget transparency slider | **Works** for both widgets (user confirmed after the stale-session fix) |
| App follows system dark mode | **Pushed, NOT yet confirmed** by the user (last change, `7bce348`) |
| Real-phone install | User installed on a phone (noted dark-mode mismatch there); no other phone issues reported |
| `:app:lint`, `:app:verifyNoGoogleServices`, `:app:dependencies` | **Never run** (no Android SDK in the agent sandbox) |
| Instrumented/UI tests | None exist |
| Secret URL fetch against a real Google feed | Done by the user on emulator (events appeared); not covered by automated tests |

## 6. How to check the work

### In a sandbox without Android SDK (what previous agents had)

Only `:core` builds. Google's Maven (`dl.google.com`) is blocked (HTTP 403) by the agent proxy, so a normal
`settings.gradle.kts` fails resolving the Kotlin plugin. Workaround used all session (the file is **git-ignored**, so
recreate it if missing): copy `settings.gradle.kts` to `settings-sandbox.gradle.kts` and wrap **both** `google()` calls as

```kotlin
google { content {
    includeGroupAndSubgroups("com.android")
    includeGroupAndSubgroups("androidx")
    includeGroupAndSubgroups("com.google.android")
} }
```

Then:

```bash
./gradlew -c settings-sandbox.gradle.kts :core:test
grep -ho 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' core/build/test-results/test/*.xml
```

Notes: do **not** use `--offline` on the first run; Maven Central sometimes answers 429, just retry after ~30 s.
The committed `settings.gradle.kts` must keep plain `google()` (the filter breaks Android Studio: it blocks
`com.google.testing.platform`).

### On a machine with Android Studio / SDK (needed to really verify `:app`)

```bash
./gradlew :core:test
./gradlew :app:assembleDebug
./gradlew :app:lint :app:verifyNoGoogleServices   # NEVER RUN YET, do this first
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "gms|firebase|play"   # expect nothing
```

Use a **JDK 17-21** for Gradle (Gradle 8.14.3 does not support Java 25; Android Studio's "jbr-21" entry may really be Java 25. Eclipse Temurin 21 works). Inspect the merged manifest: app requests only `INTERNET` (WorkManager adds its own scheduling permissions).

### Manual test checklist (emulator or phone)

1. Install, open **Private Calendar** (not the emulator's built-in Calendar).
2. Settings: paste secret iCal URL, **Save & sync**; events appear in Agenda and Month.
3. Month tab: colored bars with titles, today circle, "+N" overflow, tap day lists events as colored cards, whole screen scrolls.
4. Add both widgets. Month: arrows change month, tap month name returns to today, tap a day opens the app on that day.
5. Settings: titles switch (widgets), transparency slider (both widgets; release finger to apply), sync interval chips, app lock.
6. Switch system light/dark: app **and** widgets follow (item pending confirmation).
7. Remove URL: widgets show "Tap to add your calendar feed"; cached events deleted.

## 7. Gotchas learned (read before changing things)

1. **Sandbox cannot compile `:app`.** Every Android change so far was written blind; the user compiled it and reported errors, which were then fixed. Expect that loop for future app changes, and tell the user plainly that app code is unverified.
2. Library API pitfalls already hit: `Constraints` is `androidx.work.Constraints`; `GlanceAppWidget.update()` is a member (no import); use the `Intent` overload of `actionStartActivity(...)`; Glance `ColorProvider(day, night)` is `androidx.glance.color.ColorProvider`; palette ints are `0xRRGGBB` (alpha 0), wrap with `0xFF000000 or rgb` before `Color(...)`.
3. **Glance session staleness:** `update()` on a live widget session only recomposes and does *not* re-run `provideGlance`. Values read once there go stale (this broke the Month widget opacity). Fix in place: widgets read inside `provideContent` via `remember(revision)`, and every settings/sync/nav change calls `Widgets.requestRefresh()` / `Widgets.updateAll()`. Keep this pattern for any new widget input.
4. Drawable colors `bar_0..7.xml` must match `EventColors.palette` (checked by hand/script only; there is no automated test across the module boundary).
5. `MonthWidget` cell/bar counts are derived from `LocalSize` (`SizeMode.Exact`); on small widgets bars degrade to dots.
6. `Widgets.revision` is in-process state; fine because widget sessions live in the app process.
7. Settings `widgetShowTitles` default is **true** (flipped from false at the user's request); existing README text reflects this.

## 8. Open items / suggested next steps

1. **Ask the user to confirm** the dark-mode fix on both emulator and real phone (and whether they prefer wallpaper-based or plain Material colors).
2. Run `:app:lint` and `:app:verifyNoGoogleServices` on a machine with the SDK; fix findings. The privacy claim "no Google code" is currently only enforced by that unrun task.
3. Add a README note that the app follows system dark mode (not yet documented).
4. Confirm or change the package name `io.github.snow34.calendarwidget` before any wider distribution (changing it later means a new install).
5. Release signing / signed APK (user chose debug for now). A release build also needs R8 verified (`isMinifyEnabled = true`; no keep rules were needed so far but unverified).
6. Tests worth adding: `IcsFetcher` with MockWebServer (304, 429, oversize, non-ICS body, http URL rejected); a Robolectric/instrumented test for `Vault`/`SecureStore`; an automated palette-vs-drawable sync check.
7. Verify on a real device over a day or two: WorkManager periodic sync, widget refresh after midnight (today circle), time zone changes, battery restrictions on the user's OS.
8. In-app app-lock: untested behavior when no screen lock is set (code fails closed: stays locked).
9. Possible user requests seen so far that may come back: bar height/text size tuning, more/other colors, in-app month view tweaks.

## 9. Conventions for commits and process

- Develop and push on `claude/lucid-planck-vy2q5k` only. `git push -u origin claude/lucid-planck-vy2q5k` (retry on network errors with 2/4/8/16 s backoff).
- Commit message trailer used so far (keep it consistent):
  ```
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01Qp8bHUYJh1xHnLmHTPEm5E
  ```
- Do not put model identifiers anywhere else in the repo.
- Keep README beginner-friendly (Windows, no jargon without explanation); update it when user-visible behavior changes.
- The agent may be run in "plan mode" by the user: then do not edit anything except the plan file; use AskUserQuestion for real forks and ExitPlanMode for approval.

## 10. Changelog (newest first)

| Commit | Date | Change |
|---|---|---|
| `7bce348` | 2026-10-30 | App follows system light/dark (`AppTheme`, DayNight window theme). Unconfirmed by user. |
| `3763445` | 2026-10-30 | Widgets re-read settings/events on every refresh (fixes Month widget ignoring opacity: stale Glance session). |
| `a839990` | 2026-10-30 | Widget background transparency slider (0-100%, 5% steps, shared by both widgets; only background fades). |
| `ca6c862` | 2026-10-30 | Event colors in app: colored cards (Agenda, selected day), month grid bars, README "Part 5: Make an app file (APK)". |
| `d1b2ce8` | 2026-10-30 | Month widget events as colored titled bars; `EventColors`, day ordering, titles on by default. |
| `6c13d27` | 2026-10-30 | Fixed the first real compile errors (`Constraints` import, `actionStartActivity`, `update` import). |
| `7172d57`, `407e354` | 2026-10-30 | README: Gradle JDK troubleshooting (Java 25 vs 21). |
| `8830958` | 2026-10-30 | Fixed Android Studio sync failure (removed `google()` group filter); added Month widget and `groupByDay`. |
| `1ec541b` | 2026-10-30 | Beginner-friendly Windows README. |
| `71a4eb7` | 2026-10-30 | Android app module: encrypted storage, hardened fetch, Compose UI, Glance widget, app lock, `verifyNoGoogleServices`. |
| `3f02fe2` | 2026-10-30 | Gradle skeleton + pure-Kotlin ICS parser with recurrence expansion (10 tests). |
| (this commit) | 2026-11-03 | Added `handoff.md`. |

## 11. How to keep this file current

After **every** change you make:

1. Add a row at the top of the Changelog (date, short description; use the commit hash once known, or "(this commit)" and fix it on the next change).
2. Update section 5 (Status) and section 8 (Open items): tick off what was verified, add what is new or still unverified.
3. Add anything surprising to section 7 (Gotchas) and any new decision to section 4.
4. Update "Last updated" at the top.
5. Commit `handoff.md` in the **same commit** as the code change, and push.
