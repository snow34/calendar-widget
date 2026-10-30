# calendar-widget

A private, read-only calendar app and home-screen widget for Android. It shows your Google Calendar
events, and it is built so that nothing else on your phone is shared with Google.

> **Status:** early version. The calculation part is tested, but the Android app has not been built on a
> real machine yet. Your first build may show a few errors. If it does, copy the message and send it
> to whoever is helping you build this. They are usually quick to fix.

---

## Try it on your computer and phone (Windows, no experience needed)

You will need: a Windows PC, about 15 GB of free space, an internet connection, and (for the phone part)
a USB cable and an Android phone running **Android 11 or newer**.

### Part 0: Get your calendar's secret address (2 minutes)

The app reads your calendar through a private web address, so you don't have to sign in to Google in the app.

1. On a computer, open [Google Calendar](https://calendar.google.com) and sign in.
2. Click the gear icon, then **Settings**.
3. On the left, under **Settings for my calendars**, click your calendar.
4. Scroll down to **Integrate calendar**.
5. Copy the address under **Secret address in iCal format**. It starts with `https://calendar.google.com/`.

**Treat this address like a password.** Anyone who has it can read your calendar. If it leaks, press
**Reset** next to it in the same place and the old address stops working.

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
7. In the app, open **Settings**, paste your secret address (Part 0), and press **Save & sync**.
   (Copy the address on your PC, then in the virtual phone click in the box and press **Ctrl+V**.)
8. Open the **Agenda** and **Month** tabs to see your events.

**Widgets.** On the virtual phone's home screen, press and hold on an empty spot, tap **Widgets**,
find **Private Calendar**, and drag one onto the screen. There are two:
- **Private Calendar – Month**: the whole month. Each day shows your event names. Use the arrows
  to change month, tap the month name to come back to this month, and tap a day to open it in the app.
  Make the widget as large as you can so the names fit.
- **Private Calendar – Upcoming**: a short list of what is coming next.

For privacy, widgets show dots instead of event names until you switch on **Show event titles on the widget**
in the app's **Settings** tab. Do that to see names in the Month widget.

### Part 4: Put it on your real phone

1. On your phone open **Settings → About phone** and tap **Build number** seven times in a row.
   It will say you are now a developer.
2. Go back to **Settings → System → Developer options** and switch on **USB debugging**.
3. Connect the phone to the PC with the USB cable. On the phone, tap **Allow** on the
   "Allow USB debugging?" question.
4. In Android Studio, at the top, open the device list and choose your phone's name.
5. Click the green **▶ Run** button. The app is installed and starts on your phone.
6. Open the app's **Settings** tab, paste your secret address and press **Save & sync**.
7. To add a widget: press and hold on an empty spot of your home screen, tap **Widgets**,
   find **Private Calendar – Month** (or **– Upcoming**), and drag it onto the screen.

**No cable? Copy the app file instead.**
1. In Android Studio choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. When it finishes, click **locate** in the pop-up. You will see the file `app-debug.apk`.
3. Send that file to your phone (cable, cloud drive, etc.) and tap it on the phone. If the phone says
   installing from this source is blocked, allow it for the app you opened the file with, then try again.

### If something goes wrong

| What you see | What to do |
|---|---|
| Red errors after opening the project | Click **File → Sync Project with Gradle Files**. If errors stay, copy the first red message and send it for help. |
| No **app** folder on the left | Same: **File → Sync Project with Gradle Files**. |
| "Incompatible Gradle JVM version" (Java 25) | Click **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**. Open the **Gradle JDK** list and choose an entry whose grey text says **21** (for example **temurin-21**). Names can be misleading: on some versions of Android Studio "jbr-21" is really Java 25, so always check the grey version text. Never pick one that says 25. Click **OK**, then **File → Sync Project with Gradle Files**. If no entry shows 21, choose **Download JDK…**, set Version **21**, Vendor **Eclipse Temurin**, and click **Download**. |
| "Could not find com.google.…" during sync | Make sure you have the latest version of this project (download the ZIP again), then click **Try Again** in the yellow bar. |
| The virtual phone shows a calendar but no iCal option | That is the phone's built-in calendar. Open **Private Calendar** from the app list instead. |
| Your phone isn't in the device list | Use a data cable (not charge-only), unlock the phone, tap **Allow** on the phone, and make sure USB debugging is on. |
| "App not installed" on the phone | Your phone must be Android 11 or newer. Remove any older copy of the app and try again. |
| "Feed rejected (HTTP 404)" in the app | The secret address changed or was reset. Copy the new one from Google Calendar. |
| Widget shows dots or "Event" instead of names | That is on purpose, for privacy. Turn on **Show event titles on the widget** in Settings. |
| Month widget cells are too small to read | Press and hold the widget, then drag its edges to make it bigger. |

---

## How it works

- You paste your calendar's **secret iCal address** once. No Google sign-in, OAuth or Google code is involved.
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
| Backups | Disabled; transfer rules exclude everything |
| Screen | Screenshots and the recent-apps preview are blocked |
| Widgets | Show dots / times only by default; event titles are opt-in |
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
