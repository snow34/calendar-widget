# Handoff: Private Calendar (calendar-widget)

For the next AI agent. Read this first, check the work with the commands below, then continue from **Open items**.

> **Standing instruction from the user:** update this file after **every** change (status, changelog, open items,
> anything learned) and commit it together with the code change. See "How to keep this file current".

- Repo: `snow34/calendar-widget`. Work branch: **`claude/lucid-planck-vy2q5k`** (push only here; do **not** open a PR unless the user asks).
- Last updated: 2026-11-03, with the **Appearance settings** change (newest Changelog row; its hash is the commit that follows `f761a15`).

---

## 1. What this is

A private, read-only Android calendar app + two home-screen widgets for **Google Calendar**, built so Google gets
nothing from the phone beyond the calendar fetch itself.

- Data source: one or more **iCal feed addresses**, pasted once each by the user: Google Calendar's **"Secret address in
  iCal format"** (one per Google calendar) or any other `https://` / `webcal://` .ics feed (Outlook, Apple, holidays...).
  Up to 20 calendars. No Google sign-in, no OAuth, no Google SDKs, no Play Services (user runs a de-Googled Android OS).
- Features: Agenda tab, Month tab (colored titled bars per day), Settings tab; **Month widget** (grid with colored
  event bars) and **Upcoming widget** (short list); optional biometric app lock; widget background transparency slider;
  **+ Add event / Edit in Google Calendar** buttons that open Google Calendar's website in the browser (app stays read-only);
  **multiple calendars** (name, color dot, on/off switch, remove; color events by calendar or by event name).
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
app/    Android app (Compose UI, Glance widgets, WorkManager sync, Keystore storage). **Min SDK 31 (Android 12)**, target/compile 35.
README.md   beginner-friendly Windows guide (Parts 0-5) + privacy model + developer notes
handoff.md  this file
```

Key files:

| Area | Files |
|---|---|
| Parsing / recurrence | `core/.../IcsParser.kt`, `RecurrenceExpander.kt`, `Model.kt` (RRULE, EXDATE, RECURRENCE-ID, TZID/DST, all-day) |
| Calendars list | `core/.../CalendarSource.kt` (`CalendarSource`, `CalendarSources`: `normalizeFeedUrl` webcal->https, `nextColorIndex`, `suggestName`, `uniqueName`, `cleanName`), `EventColoring.kt` (`ColorMode` CALENDAR/TITLE, `EventColoring`), `WindowsTimeZones.kt` (Outlook TZID -> IANA), `Occurrence.calendarId` |
| Browser links to Google Calendar | `core/.../GoogleCalendarLinks.kt` (`addEventUrl`, `dayUrl`, `calendarIdFromFeedUrl`, `eventUrl`), `app/.../ui/ExternalLinks.kt` (`openInBrowser`) |
| Per-day placement | `core/.../DayGrouping.kt` (`groupByDay`: multi-day events on every day, sorted all-day first, then start, then title) |
| Colors | `core/.../EventColors.kt` (`DEFAULT_PALETTE` of 8 `0xRRGGBB` ints, `indexFor(title)` FNV-1a hash, `readableTextOn(rgb)` = black/white by WCAG contrast, `ColorHex`). The live palette is `Appearance.palette` (user-editable); there are **no palette drawables any more** |
| Appearance | `core/.../Appearance.kt`: `Appearance` (theme mode, wallpaper colors, `TextScale`, `FontChoice`, `BarHeight`, palette; `encode()` / tolerant `parse()` stored as one pref `appearance_v1`), `AppearancePresets` (Default/Pastel/Vivid/Earth), `BarMetrics` (bar/cell/day-number sizes shared by app and widget so text never clips). `app/.../ui/AppearanceSection.kt` (Settings UI: chips, 8 swatches, color dialog with RGB sliders + hex, preview, reset), `AppTheme.kt` (theme, wallpaper colors, font family, text scale via `LocalDensity`), `widget/WidgetText.kt` (`widgetTextStyle`, Glance font mapping) |
| Opacity math | `core/.../WidgetOpacity.kt` |
| Storage / security | `app/.../data/Vault.kt` (AES-256-GCM, non-exportable Keystore key), `SecureStore.kt` (encrypted `calendars.json.enc` = list incl. secret URLs, one encrypted `events-<id>.ics.enc` per calendar in `AtomicFile`s; etag/last-sync per calendar and other settings in plain prefs; **migrates** the old single-URL install on first open) |
| Network | `app/.../data/IcsFetcher.kt` (https only, 10 MiB cap, ETag, no redirects https->http, never logs the URL) |
| Sync | `app/.../sync/Syncer.kt` (shared by UI and worker), `SyncWorker.kt` (WorkManager periodic) |
| UI | `app/.../ui/MainActivity.kt` (FLAG_SECURE, app lock, deep link `calendarwidget://day/<date>`), `CalendarApp.kt` (Agenda/Month), `SettingsScreen.kt` (calendar list, add/rename/remove, color mode, sync interval, widget options, app lock), `AppTheme.kt`, `AppearanceSection.kt`, `EventTint.kt` (`tintAt(index, palette)`, `CalendarContext`, `opaqueColor`), `Format.kt` |
| Widgets | `app/.../widget/MonthWidget.kt`, `CalendarWidget.kt` (Upcoming), `Widgets.kt` (refresh bus + `openAppIntent`), `WidgetBackground.kt`, `WidgetText.kt` |
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
- **Appearance settings (user decisions, 2026-11-03):** user asked how to change fonts/colors and chose an **Appearance section in Settings** plus a written "where to change it in code" guide (README Part 6). Controls: theme (system/light/dark), event colors, text size, widget bar height, font. Colors = edit the **8 shared colors** (RGB sliders / hex) + ready-made sets (Default = original look, Pastel, Vivid, Earth); calendars and event-name coloring both use these 8. The user's phone is **Android 12+**, so **minSdk was raised 30 -> 31** (Android 11 no longer supported). Agent choices (not asked): text on a bar is auto black/white (`readableTextOn`); text size multiplies every sp in app and widgets (4 steps 0.85/1/1.15/1.3); widget fonts are limited to system families (Glance cannot load font files); theme override applies to the **app only** (widgets always follow the phone); "use wallpaper colors" switch (default on) resolves the earlier open question about dynamic vs plain colors; bar default height is now 14dp in both app and widget (widget was 13dp); today-circle colors stay constants.
- **Multiple calendars (user decisions, 2026-11-03):** sources = Google calendars **and** other iCal feeds; coloring = **calendar color with a Settings switch to event-name colors** (default CALENDAR for fresh installs; a *migrated* single-calendar install is set to TITLE so its look does not change); widgets = **all calendars that are switched on** (one shared setting, no per-widget choice). Agent choices (not asked): sync only switched-on calendars; a failing calendar never blocks the others (`SyncResult.failures`); max 20 calendars; duplicate URLs rejected; per-calendar color = palette entry, tap the dot to cycle; calendar name shown on event cards only when >1 calendar; Outlook-style Windows `TZID`s mapped to IANA, unknown ones fall back to the phone zone.
- "+ Add event" always opens Google's form for the **default** calendar (no reliable way found to preselect another).
- **Adding/editing events (user decision, 2026-11-03):** the user asked how hard in-app editing via Google Calendar API would be. Agent explained: needs OAuth + Google Cloud project, browser-based sign-in (no Play Services), AppAuth, migrating *reading* to the Calendar API too (Google's private iCal feed refreshes only every few hours), recurring-event edit semantics, offline queue, and cannot be tested in the sandbox. User chose the **cheap browser-button route for now**: `+ Add event` opens `https://calendar.google.com/calendar/render?action=TEMPLATE&dates=...` (prefilled 09:00-10:00 on the chosen day; agent's default, not asked) and `Edit in Google Calendar` opens the event page. App remains read-only with no Google sign-in.
- If the user later wants full in-app editing, they ticked **all** of: add single events, edit/delete, repeating events, several Google calendars. That is the large OAuth/API project described above; start by asking again, it is a design fork.

## 5. Current status

| Item | State |
|---|---|
| `core` unit tests (65) | **Pass** (Appearance 8, BarMetrics 5, CalendarSources 5, DayGrouping 6, EventColoring 5, EventColors 9, Expander 10, GoogleCalendarLinks 9, WidgetOpacity 3, WindowsTimeZones 5) |
| Appearance settings (theme, text size, font, bar height, editable colors, presets) | **Pushed, NOT compiled or run anywhere** (app code written blind; `core` logic unit tested). Unverified Glance APIs: `TextStyle(fontFamily=...)` / `androidx.glance.text.FontFamily`, `GlanceModifier.cornerRadius` (needs API 31+, now guaranteed by minSdk 31). If the build fails there, fix or drop widget fonts first. |
| `:app` builds and runs | **Yes** on the user's emulator (user compiled it; no errors reported after the fixes below) |
| Month/Upcoming widgets, colors, bars, today circle | Seen working by the user on the emulator |
| Widget transparency slider | **Works** for both widgets (user confirmed after the stale-session fix) |
| App follows system dark mode | **Pushed, NOT yet confirmed** by the user (`7bce348`) |
| Multiple calendars (Settings list, merge, colors, widgets, migration) | **Pushed, NOT compiled or run anywhere** (app code written blind; only the `core` logic is unit tested). **The upgrade/migration path for an existing single-URL install is untested** and the first thing to verify. |
| Add event / Edit in Google Calendar buttons | **Pushed, unverified on a device** (app code uncompiled in sandbox; link builder is unit tested). The event deep link format (`eid` = base64url of `<eventId> <calendarId>`) is unofficial; the fallback is the day view. |
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
2. Settings: under **Add a calendar** paste a secret iCal URL, **Add & sync**; events appear in Agenda and Month. Repeat for a second calendar (and a non-Google `webcal://` feed). Try: switch a calendar off/on, tap its color dot, rename, remove, **Color events by** Calendar/Event name, a bad address (error under the form, other calendars still sync).
3. Month tab: colored bars with titles, today circle, "+N" overflow, tap day lists events as colored cards, whole screen scrolls.
4. Add both widgets. Month: arrows change month, tap month name returns to today, tap a day opens the app on that day.
5. Settings: titles switch (widgets), transparency slider (both widgets; release finger to apply), sync interval chips, app lock.
6. Switch system light/dark: app **and** widgets follow (item pending confirmation).
7. Remove all calendars (or switch them all off): widgets show "Tap to add or turn on a calendar"; removed calendars' cached events are deleted.
8. **Upgrade test:** install the previous APK (single URL), then install this one over it: the calendar must appear in the list (named after the Google calendar id), events still show, colors still by event name.

## 7. Gotchas learned (read before changing things)

1. **Sandbox cannot compile `:app`.** Every Android change so far was written blind; the user compiled it and reported errors, which were then fixed. Expect that loop for future app changes, and tell the user plainly that app code is unverified.
2. Library API pitfalls already hit: `Constraints` is `androidx.work.Constraints`; `GlanceAppWidget.update()` is a member (no import); use the `Intent` overload of `actionStartActivity(...)`; Glance `ColorProvider(day, night)` is `androidx.glance.color.ColorProvider`; palette ints are `0xRRGGBB` (alpha 0), wrap with `0xFF000000 or rgb` before `Color(...)`.
3. **Glance session staleness:** `update()` on a live widget session only recomposes and does *not* re-run `provideGlance`. Values read once there go stale (this broke the Month widget opacity). Fix in place: widgets read inside `provideContent` via `remember(revision)`, and every settings/sync/nav change calls `Widgets.requestRefresh()` / `Widgets.updateAll()`. Keep this pattern for any new widget input.
4. (Removed) The palette drawables are gone: bars/today circle are drawn with `ColorProvider` + `cornerRadius()` (needs minSdk 31). Changing `EventColors.DEFAULT_PALETTE` only affects installs without a stored appearance or after **Reset appearance** (the palette is saved inside `appearance_v1`).
5. `MonthWidget` cell/bar counts are derived from `LocalSize` (`SizeMode.Exact`); on small widgets bars degrade to dots.
6. `Widgets.revision` is in-process state; fine because widget sessions live in the app process.
7. **Google private feed lag:** changes made in Google Calendar can take hours to appear in the feed, so adding/editing via the browser is not reflected immediately. This is stated in Settings and README. It is also why true in-app editing would require reading via the Calendar API.
8. `GoogleCalendarLinks.eventUrl` only builds a direct link when the iCal UID ends with `@google.com` and the calendar id could be read from the secret URL (`calendar/ical/<id>/private-<token>/basic.ics`); otherwise it opens the day view. `eid` uses the URL-safe base64 alphabet without padding (a `+` would be mangled in a query string). Recurring instances use `<eventId>_<start in UTC>` (or `_<date>` for all-day); a *moved* instance of a series uses its new start, which Google may not recognise (falls to the series/day).
9. `Occurrence.recurring` (new field, default false) is set by `RecurrenceExpander`.
10. **Multi-calendar internals:** calendar ids are UUIDs (legacy install uses `main`) and become file names, so `SecureStore.isSafeId` only accepts `[A-Za-z0-9-]{1,64}`. List keys in Compose use `calendarId-uid-start` because two calendars can contain the same UID. `Syncer.run` returns `SyncResult` (the old `SyncOutcome` is gone). Widgets read `store.loadCalendars()` + `EventColoring.from(sources, store.colorMode)` inside `load()`; hidden calendars are excluded everywhere via `CalendarRepository.occurrences`. Enabling a calendar that has no cache triggers a sync. `org.json` is used for the encrypted list (Android API; not available in plain JVM unit tests, so serialization/migration is untested).
11. **Non-Google feeds:** only RRULE features already supported by `RecurrenceExpander` work (no BYSETPOS/BYWEEKNO/BYYEARDAY, and BYDAY ordinals only for MONTHLY/YEARLY); unknown `TZID`s fall back to the phone's zone; `VTIMEZONE` blocks are ignored; `webcal://` is rewritten to `https://`; http (non-TLS) is rejected.
12. Settings `widgetShowTitles` default is **true** (flipped from false at the user's request); existing README text reflects this.

## 8. Open items / suggested next steps

1. **Ask the user to confirm** the dark-mode fix on both emulator and real phone (and whether they prefer wallpaper-based or plain Material colors).
2. Run `:app:lint` and `:app:verifyNoGoogleServices` on a machine with the SDK; fix findings. The privacy claim "no Google code" is currently only enforced by that unrun task.
3. **First thing next:** have the user build and run the **multi-calendar and Appearance changes** (neither was ever compiled) and test the upgrade path (checklist item 8). Then check every Appearance control (theme, wallpaper switch, text size XL does not clip month cells, font, bar height, preset/custom colors, reset) on emulator and phone, and that widgets refresh after each change. Known small gap: forcing Light/Dark in the app does not change the status-bar icon color. Then: add a README note that the app follows system dark mode (not yet documented). Confirm the browser buttons work on the user's phone: does the default browser open, does the event link land on the event or the day page, what does an *unsigned-in* browser do?
4. Confirm or change the package name `io.github.snow34.calendarwidget` before any wider distribution (changing it later means a new install).
5. Release signing / signed APK (user chose debug for now). A release build also needs R8 verified (`isMinifyEnabled = true`; no keep rules were needed so far but unverified).
6. Tests worth adding: `SecureStore` migration + JSON round trip (Robolectric or instrumented; needs Keystore); a fake-feed test of `Syncer` with two calendars where one fails; `IcsFetcher` with MockWebServer (304, 429, oversize, non-ICS body, http URL rejected); a Robolectric/instrumented test for `Vault`/`SecureStore`; an automated palette-vs-drawable sync check.
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
| (this commit) | 2026-11-03 | **Appearance settings:** Settings > Appearance (theme/wallpaper colors, text size, font, bar height, 8 editable colors + presets, preview, reset), applied to app and both widgets; `core` gained `Appearance`, `AppearancePresets`, `BarMetrics`, `ColorHex`, `readableTextOn` (+17 tests, 65 total); palette drawables deleted; **minSdk 31**; README Part 6 = where/how to change the look in code. |
| `f761a15` | 2026-11-03 | **Multiple calendars:** calendar list in Settings (add/rename/remove/switch/color), per-calendar encrypted cache and sync, merged Agenda/Month/widgets, calendar-vs-title color mode, `webcal://`, Windows TZID map, migration of the old single URL; `core` gained `CalendarSource(s)`, `EventColoring`, `WindowsTimeZones`, `Occurrence.calendarId` (+15 tests, 48 total). |
| `d1bf7a8` | 2026-11-03 | `+ Add event` (Agenda, Month) and `Edit in Google Calendar` (event cards) open Google Calendar in the browser; `GoogleCalendarLinks` in core (9 tests); `Occurrence.recurring`; Settings + README explain the feed delay. |
| `063efe9` | 2026-11-03 | Added `handoff.md`. |
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

## 11. How to keep this file current

After **every** change you make:

1. Add a row at the top of the Changelog (date, short description; use the commit hash once known, or "(this commit)" and fix it on the next change).
2. Update section 5 (Status) and section 8 (Open items): tick off what was verified, add what is new or still unverified.
3. Add anything surprising to section 7 (Gotchas) and any new decision to section 4.
4. Update "Last updated" at the top.
5. Commit `handoff.md` in the **same commit** as the code change, and push.
