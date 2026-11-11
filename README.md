# calendar-widget

A private calendar app and home-screen widgets for Android. It shows your Google Calendar
events (and other calendars), and it is built so that nothing else on your phone is shared with Google.
Adding events from inside the app is optional (Part 7); without it the app only reads your calendars.

> **Status:** early version. The calculation part is tested and the app has been built and run, but the newest
> features are not tried yet. A build after an update may show a few errors. If it does, copy the message and send it
> to whoever is helping you build this. They are usually quick to fix.

---

## Try it on your computer and phone (Windows, no experience needed)

You will need: a Windows PC, about 15 GB of free space, an internet connection, and (for the phone part)
a USB cable and an Android phone running **Android 12 or newer**.

### Part 0: Get your calendar's secret address (2 minutes)

The app reads your calendar through a private web address, so you don't have to sign in to Google in the app.

1. On a computer, open [Google Calendar](https://calendar.google.com) and sign in.
2. Click the gear icon, then **Settings**.
3. On the left, under **Settings for my calendars**, click your calendar.
4. Scroll down to **Integrate calendar**.
5. Copy the address under **Secret address in iCal format**. It starts with `https://calendar.google.com/`.

**Treat this address like a password.** Anyone who has it can read your calendar. If it leaks, press
**Reset** next to it in the same place and the old address stops working.

**More than one calendar?** Repeat Part 0 for each calendar (every Google calendar has its own secret address). Google
usually only shows that address for calendars you own or manage; a calendar someone else shares with you normally has
none unless its owner made it public. Any other iCal or `webcal://` address works as well, for example
school holidays or a sports fixtures feed. In the app's **Calendars** tab (bottom bar) you can then:
- add as many as 20 calendars, give each a name, and rename or remove them later (removing only deletes them from the phone);
- see all your calendars in one list, and switch each one off and on with its switch, without removing it. Calendars that are switched off are hidden in the Agenda, the Month view **and** both widgets;
- tap a calendar's **color dot** to change its color;
- in the **Appearance** section, choose **Color events by: Calendar** (every event takes its calendar's color, like Google Calendar) or **Event name**
  (the color is picked from the event's name). If you used an earlier version of the app, your first calendar is kept
  and the colors stay *by event name* until you change this.

### Part 1: Install the tools (about 20 minutes, one time only)

1. Download **Android Studio** from <https://developer.android.com/studio> and install it. Accept the
   default choices. This is the free program used to build and run Android apps. It also installs everything else needed.
2. Download this project: on this repository's GitHub page click **Code → Download ZIP** (make sure the
   branch `claude/lucid-planck-vy2q5k` is selected), then right-click the ZIP and choose **Extract All**.
   (If you already use Git, `git clone` works too.)

### Part 2: Open the project

1. Start Android Studio. On the welcome screen click **Open**.
2. Choose the extracted `calendar-widget` folder and click **OK**. Click **Trust Project** if asked.
3. Wait. A progress bar at the bottom says it is downloading things. The first time this can take 5-15 minutes.
   Wait until it stops.
4. If you do not see a folder called **app** on the left, click **File → Sync Project with Gradle Files** and wait again.
5. Check the bottom panel. If the sync worked, the top bar has a green **▶ Run** button with the name **app** next to it.
   If it still says **Add Configuration** and ▶ is grey, the sync failed. See "If something goes wrong" below.

### Part 3: Test it on your computer with a virtual phone

A "virtual phone" (called an *emulator*) is a fake phone that runs in a window on your PC.

1. Click **Tools → Device Manager**, then **Create Device** (or the **+** button).
2. Pick any phone, for example **Pixel 6**, and click **Next**.
3. Choose a system image for **API 34** (click **Download** next to it if needed, then **Next**).
   Pick one that does **not** say "Google Play". That keeps Google out of the test phone too.
4. Click **Finish**.
5. At the top of the window, make sure the virtual phone is selected, then click the green **▶ Run** button.
6. Wait for the phone window to start (a few minutes the first time). The app opens by itself.
   **Important:** the virtual phone already has its own built-in "Calendar" app and widget. That is *not* this app.
   Ours is called **Private Calendar**. Swipe up on the home screen to find it in the app list.
7. In the app, open the **Calendars** tab (bottom bar; on first start the app opens there by itself). Under **Add a calendar**, paste your secret address (Part 0) and press **Add & sync**.
   The bottom bar has four tabs: **Agenda**, **Month**, **Calendars** (your list of calendars with on/off switches) and **Settings**.
   Settings is split into sections: **Appearance**, **Widgets**, **Sync** and **Security**. Tap a section's name to open or close it. When a section is closed, a short line under its name shows its current choices.
   (Copy the address on your PC, then in the virtual phone click in the box and press **Ctrl+V**.)
8. Open the **Agenda** and **Month** tabs to see your events. Every event gets a color, and the same
   event always gets the same color in the app and in the widgets (the colors are picked automatically from the
   event's name, because Google's private calendar address doesn't include the colors you set in Google Calendar).
   In the **Month** tab each day shows colored bars with the event names, today's date has a light circle,
   and tapping a day lists its events below the grid. If a day has more events than fit, it shows "+2" (or similar).
9. **Adding and changing events.** Tap **+ Add event** (in the Agenda tab, or in the Month tab for the day you selected).
   - If you have set up the optional **Google account** sign-in (Part 7), a form opens inside the app. Fill in the name, pick the
     calendar, the date and time (or **All day**), and optionally a place and notes, then tap **Save**. The event is saved
     straight to Google Calendar and shows up in the app and widgets immediately.
   - Without that sign-in, **+ Add event** opens Google Calendar's own "new event" page in your web browser instead.
   - To change or delete an event, tap **Edit in Google Calendar** on it (this always opens Google's site in the browser).
   Google updates the private calendar address slowly, so a change made on Google's site can take **a few hours** to show up
   in this app. If an event can't be opened directly (for example one imported from another calendar), you land on that day's
   page in Google Calendar instead.

**Widgets.** On the virtual phone's home screen, press and hold on an empty spot, tap **Widgets**,
find **Private Calendar**, and drag one onto the screen. There are two:
- **Private Calendar – Month**: the whole month. Under each date every event is a colored bar with its name,
  and today's date has a light circle. The same event always gets the same color. Use the arrows
  to change month, tap the month name to come back to this month, and tap a day to open it in the app.
  Make the widget as large as you can so more bars and longer names fit. If a day has more events than
  fit, it shows "+2" (or similar) for the rest, and an event that lasts several days appears on each day.
- **Private Calendar – Upcoming**: a short list of what is coming next.

Widgets show event names by default. Anyone who can see your home screen can read them, so if you
want to hide them, open the **Widgets** section in the app's **Settings** tab and switch off **Show event titles on the widget**. The Month
widget then shows plain colored stripes with no text, and the Upcoming widget shows "Event".

**See-through widgets.** In the **Widgets** section of the app's **Settings** tab, drag the **Widget background** slider. 100% is solid,
0% is completely see-through, and it moves in steps of 5%. Let go of the slider and both widgets update
(give them a second). Only the background fades; the colored event bars and the text stay solid. On a bright or
busy wallpaper a value between 30% and 60% is usually easiest to read.

**Change how it looks.** In the app's **Settings** tab, open the **Appearance** section. Everything you change there also updates the widgets:
- **Theme:** follow the phone, always light, or always dark (widgets always follow the phone). **Use wallpaper colors**
  gives the app the colors of your wallpaper; switch it off for plain colors.
- **Text size:** Small, Normal, Large or Extra large (app and widgets).
- **Font:** Sans-serif, Serif or Monospace. These are the fonts built into Android.
- **Event bar height:** Compact, Normal, Tall or Extra tall. The bars never get smaller than their text needs.
- **Color events by:** **Calendar** (every event takes its calendar's color) or **Event name** (the color is picked from the event's name).
- **Event colors:** pick a ready-made set (Default, Pastel, Vivid, Earth), or tap any of the 8 color squares to change it with
  sliders or a color code like `#D95D77`. Calendars and event names both pick from these 8 colors, and the text on a bar is
  automatically black or white, whichever is easier to read. The sample bars at the top of the section show the result.
- **Reset appearance** puts everything back the way it was at the start.

### Part 4: Put it on your real phone

1. On your phone open **Settings → About phone** and tap **Build number** seven times in a row.
   It will say you are now a developer.
2. Go back to **Settings → System → Developer options** and switch on **USB debugging**.
3. Connect the phone to the PC with the USB cable. On the phone, tap **Allow** on the
   "Allow USB debugging?" question.
4. In Android Studio, at the top, open the device list and choose your phone's name.
5. Click the green **▶ Run** button. The app is installed and starts on your phone.
6. Open the app's **Calendars** tab, paste your secret address under **Add a calendar** and press **Add & sync**.
7. To add a widget: press and hold on an empty spot of your home screen, tap **Widgets**,
   find **Private Calendar – Month** (or **– Upcoming**), and drag it onto the screen.

### Part 5: Make an app file (APK) to install on any phone

An **APK** is the installable app file. You make it once on your PC and can then copy it to your phone
without a cable.

1. In Android Studio's top menu click **Build**. The next item is called
   **Generate App Bundles or APKs** in new versions, or **Build Bundle(s) / APK(s)** in older ones.
2. Choose **Generate APKs** (older versions: **Build APK(s)**).
3. Wait until a pop-up in the bottom-right corner says the APK was generated. The first build can take a few minutes.
4. Click **locate** in that pop-up. A Windows folder opens with a file called `app-debug.apk`
   (full path inside the project: `app\build\outputs\apk\debug\app-debug.apk`).
5. Send that file to your phone (USB cable, a cloud drive, email to yourself...).
6. On the phone, open the file (for example from the **Downloads** or **Files** app) and tap **Install**.
   If the phone says installing from this source is blocked, tap **Settings**, switch on **Allow from this
   source** for the app you opened the file with, go back and tap **Install** again.
7. Open **Private Calendar** from the app list, then the **Calendars** tab. Paste your secret address under **Add a calendar** and press **Add & sync**.

This is a *debug* app file. That is fine for your own phone. It is signed with a throwaway key made by
Android Studio, so you can only update it with another build from the same PC. To update, build a new APK and
install it over the old one.

### Part 6: Change the look in the code (optional, for tinkerers)

You can change the look with the settings above, but some things are fixed in the code. To change them: in Android Studio press
**Shift twice** (search everywhere), type the file name, open it, change the value, then press the green **▶ Run**
(and rebuild the APK if you use one). Always change one thing at a time so you can undo it.

| I want to change... | File (search for the name) | What to edit |
|---|---|---|
| The 8 starting colors | `EventColors.kt` | `DEFAULT_PALETTE`: eight codes like `0xD95D77` (red, green, blue in hex) |
| The ready-made color sets | `Appearance.kt` | `AppearancePresets.all`: add or edit a set (each needs exactly 8 colors) |
| How big "Small / Large..." text is | `Appearance.kt` | `TextScale`: the numbers 0.85, 1.0, 1.15, 1.3 |
| How tall the bars are | `Appearance.kt` | `BarHeight` (dp numbers). Spacing and cell rules are in `BarMetrics` below it |
| What a fresh install looks like | `Appearance.kt` | the default values in `data class Appearance(...)` |
| The circle around today's date | `CalendarApp.kt` (app) and `MonthWidget.kt` (widget) | `TODAY_BG` (circle) and `TODAY_TEXT` (number) |
| Month widget spacing and text sizes | `MonthWidget.kt` | the `PAD`, `HEADER_H`, `WEEKDAY_H`, `STRIPE_H`, `BAR_RADIUS` lines near the bottom; text sizes are the numbers like `15f` and `20f` inside `widgetTextStyle(...)` calls |
| The widget background colors | `WidgetBackground.kt` | the two `Color(0xFF...)` values (light and dark) |
| App light/dark colors, fonts | `AppTheme.kt` | `AppTheme` and `typographyFor` |
| App and widget names | `strings.xml` | `app_name`, `widget_upcoming_label`, `widget_month_label` |

**A custom font for the app** (widgets can only use Android's built-in fonts): create the folder `app/src/main/res/font`, copy a
`.ttf` file into it (name it with lowercase letters, digits and `_` only, like `my_font.ttf`), then in `AppTheme.kt` change one line in
`typographyFor`, for example `FontChoice.SERIF -> FontFamily(Font(R.font.my_font))` (Android Studio will offer to add the missing imports).
After that, the **Serif** choice in Settings → Appearance uses your font.

**Good to know:** the app remembers the colors and look you chose. If you change `DEFAULT_PALETTE` or the defaults and don't see
any difference, open Settings → Appearance and tap **Reset appearance** once.

### Part 7: Add events from inside the app (optional, about 30 minutes, one time only)

Without this part, **+ Add event** opens Google Calendar in the browser. With it, events are saved straight from the app.
Google only allows that for apps that are registered with Google, so you register *your own copy* of the app in a free
Google Cloud project. Nobody else gets access; the project only identifies your app to Google.

> Google changes the look and wording of its Cloud Console often. If a button below is named differently, look for the
> closest match. I could not check these screens myself.

**A. Find your app's fingerprint (SHA-1)** – Google uses it to recognise your copy of the app.
1. In Android Studio, open the **Terminal** tab at the bottom.
2. Type `gradlew.bat signingReport` and press Enter. Wait until it finishes.
3. In the output, find the block that says `Variant: debug`. Copy the long code after **SHA1:** (it looks like `AB:CD:12:...`).
   This belongs to the computer you build the app on. If you later build the APK on another computer, repeat step C there.

**B. Create the Google Cloud project**
1. Open <https://console.cloud.google.com> and sign in with the Google account that owns your calendar.
2. At the top, open the project list → **New project**. Name it, for example, `Private Calendar`, and click **Create**. Make sure it is selected.
3. Open the menu → **APIs & Services → Library**, search for **Google Calendar API**, open it and click **Enable**.
4. Open **APIs & Services → OAuth consent screen** (may be called **Google Auth Platform**). Choose **External**, fill in an app name
   (e.g. `Private Calendar`) and your e-mail address where asked, and save.
5. Under **Audience** (or **Publishing status**), click **Publish app** / **In production** and confirm. If you leave it in **Testing**,
   Google signs the app out every 7 days and you must add your own address as a **test user**.

**C. Create the client ID**
1. Open **APIs & Services → Credentials** (or **Clients**) → **Create credentials → OAuth client ID**.
2. Application type: **Android**.
3. Package name: `io.github.snow34.calendarwidget`
4. SHA-1 certificate fingerprint: paste the code from step A.
5. Click **Create**. Then open the new client and, under **Advanced settings**, switch on **Enable custom URI scheme** and **Save**.
   (Without this, Google shows "Custom URI scheme is not enabled for your Android client".)
6. Copy the **Client ID**. It ends with `.apps.googleusercontent.com`.

**D. Sign in from the app**
1. In the app, open **Settings → Google account**.
2. Paste the client ID and tap **Sign in with Google**. Your browser opens Google's sign-in page.
3. Google will say the app **isn't verified**. That is expected for your own private app: tap **Advanced → Go to Private Calendar (unsafe)**.
4. Allow access to your calendar events. The browser returns to the app, and Settings says **Signed in**.
5. Now **+ Add event** opens the form in the app. You can add events to the Google calendars you added by their **secret address**
   (other iCal feeds stay view-only).

**What the app can do with this sign-in:** see, add, change and delete events in your Google calendars (it only ever adds them).
No access to e-mail, contacts, Drive or anything else. The permission is stored encrypted on the phone. **Sign out** in
Settings → Google account removes it from the phone and asks Google to cancel it; you can also remove it at
<https://myaccount.google.com/permissions>.

### If something goes wrong

| What you see | What to do |
|---|---|
| Red errors after opening the project | Click **File → Sync Project with Gradle Files**. If errors stay, copy the first red message and send it for help. |
| No **app** folder on the left | Same: **File → Sync Project with Gradle Files**. |
| "Incompatible Gradle JVM version" (Java 25) | Click **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**. Open the **Gradle JDK** list and choose an entry whose grey text says **21** (for example **temurin-21**). Names can be misleading: on some versions of Android Studio "jbr-21" is really Java 25, so always check the grey version text. Never pick one that says 25. Click **OK**, then **File → Sync Project with Gradle Files**. If no entry shows 21, choose **Download JDK…**, set Version **21**, Vendor **Eclipse Temurin**, and click **Download**. |
| "Could not find com.google.…" during sync | Make sure you have the latest version of this project (download the ZIP again), then click **Try Again** in the yellow bar. |
| The virtual phone shows a calendar but no iCal option | That is the phone's built-in calendar. Open **Private Calendar** from the app list instead. |
| Your phone isn't in the device list | Use a data cable (not charge-only), unlock the phone, tap **Allow** on the phone, and make sure USB debugging is on. |
| "App not installed" on the phone | Your phone must be Android 12 or newer. Remove any older copy of the app and try again. |
| "Feed rejected (HTTP 404)" in the app | The secret address changed or was reset. Copy the new one from Google Calendar. |
| One calendar shows no events but the others do | The message appears at the top of the **Calendars** tab after you press **Sync now** there (for example "Feed rejected (HTTP 404)" means that calendar's address was reset: remove it and add the new one). The other calendars keep working. |
| Events from another calendar are an hour or more off | The feed uses an unusual time zone name the app doesn't know, so it falls back to the phone's time zone. Tell whoever maintains this app which feed it is. |
| "Custom URI scheme is not enabled" when signing in | In Google Cloud, open your Android client ID, switch on **Enable custom URI scheme** under **Advanced settings**, save, wait a few minutes, try again (Part 7, C5). |
| Google says the app is "blocked" or shows "Error 400"/"redirect_uri" when signing in | Check Part 7: package name exactly `io.github.snow34.calendarwidget`, the SHA-1 from the computer that built this app, and the custom URI scheme switched on. |
| Saving an event says "Google refused" | You can only add to calendars you own (or may edit), and the Google Calendar API must be enabled in your project (Part 7, B3). |
| Saving an event says the sign-in "expired" | Sign in again in Settings → Google account. If it happens every week, your Google Cloud app is still in **Testing**: publish it (Part 7, B5). |
| "Edit in Google Calendar" opens a day page, not the event | Normal for events Google can't link directly (imported ones, for example). Find the event on that day and edit it there. |
| A new event doesn't show up in the app | Google refreshes the private calendar address slowly (up to a few hours). Press **Sync now** in the **Calendars** tab later. |
| Widget shows plain stripes or "Event" instead of names | **Show event titles on the widget** is switched off (Settings → Widgets). Switch it on. |
| Month widget cells are too small to read | Press and hold the widget, then drag its edges to make it bigger. |

---

## How it works

- You paste each calendar's **secret iCal address** once. Reading your calendars needs no Google sign-in and no Google code.
- Only if you set up the optional Google sign-in (Part 7) does the app hold a permission to **add** events to your
  Google calendars (calendar events only, stored encrypted). It still reads through the secret addresses.
- A background job downloads the calendar file over HTTPS and stores it encrypted (AES-256-GCM, with a key
  that can't be taken out of the phone's Keystore). The address is stored the same way.
- The app (agenda and month views) and the two widgets (month grid, upcoming list) read the encrypted copy. Repeating events, exceptions and
  time zones are worked out by the `core` module.

## Privacy model

| | |
|---|---|
| Permissions requested by the app | `INTERNET` only (the scheduling library adds a few of its own) |
| Google/Firebase/Play Services code | None; `gradlew.bat :app:verifyNoGoogleServices` fails the build if any appears |
| Network | HTTPS only, system certificate authorities only, no plain http, no redirects to http |
| Adding / editing events | Editing and deleting happen on Google's own site in the browser. Adding happens there too, unless you set up the optional Google sign-in (Part 7): then the app can add events itself and holds an encrypted permission for your calendar events only |
| Backups | Disabled; transfer rules exclude everything |
| Screen | Screenshots and the recent-apps preview are blocked |
| Widgets | Show event titles by default; one switch in Settings → Widgets hides them (stripes only) |
| App lock | Optional fingerprint / screen lock |
| Logging | The address and event contents are never logged |

What this does **not** hide: Google's servers still see that your calendar address is being fetched,
because the calendar lives there. Anyone holding the secret address can read the calendar. The phone's
operating system is outside the app's control (this project assumes a de-Googled OS).

---

## For developers

- `core/` is pure Kotlin/JVM: ICS parser and recurrence expander (RRULE, EXDATE, RECURRENCE-ID, DST-safe). Unit tested.
- `app/` is the Android app (Jetpack Compose, Glance widget, WorkManager, Keystore). Min SDK 30.
- `:app` is included only when an Android SDK is found (`ANDROID_HOME`, `ANDROID_SDK_ROOT` or `local.properties`).
- Shared logic worth testing (parsing, recurrence, placing events on days) lives in `core/`; `app/` stays thin.

```bash
./gradlew :core:test                                  # no Android SDK needed
./gradlew :app:assembleDebug                          # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug                           # install on a connected device
./gradlew :app:lint :app:verifyNoGoogleServices
```

On Windows use `gradlew.bat` instead of `./gradlew`.
