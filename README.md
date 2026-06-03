# Azan Ulm — Android prayer-times app

An Android app that **plays the azan at each prayer time** using the monthly
timetable from the *Al-Salam (Friedens) Moschee, Ulm*. It reads a photo of the
printed sheet with on-device text recognition, lets you verify the times, then
schedules the call to prayer for **Fajr, Dhuhr, Asr, Maghrib and Isha** every
day. June 2026 is pre-loaded so it works the moment you install it; scan a new
sheet at the start of each month.

---

## What it does

- **Reads the sheet automatically** (offline) with Google ML Kit OCR, mapping
  each row's times to its date and the six printed columns
  (Fajr · Sunrise · Dhuhr · Asr · Maghrib · Isha).
- **Review screen** — every scanned day is shown in an editable grid so you can
  correct anything before saving. Rows the OCR wasn't fully sure about are
  highlighted.
- **Plays the azan** at the five daily prayers via exact alarms, even when the
  app is closed or the phone is asleep. A different recording is used for Fajr.
- **Survives reboots** — alarms are re-armed on boot, after updates, and on
  time/timezone changes.
- **Sunrise** (Sonnenaufgang) is stored for reference but never triggers an azan.

---

## Requirements

- **Android Studio** (Hedgehog 2023.1 or newer recommended).
- **JDK 17** (bundled with recent Android Studio).
- An Android phone or emulator running **Android 8.0 (API 26) or newer**.

---

## Open & build

1. Open **Android Studio** → **File ▸ Open…** and select this `AzanUlm` folder.
2. On first open, Android Studio will sync Gradle and **generate the Gradle
   wrapper** automatically. If it asks to trust the project, accept.
   *(If you build from the command line instead and have Gradle installed, run
   `gradle wrapper` once in this folder first, then `./gradlew assembleDebug`.)*
3. Connect your phone with **USB debugging on**, or start an emulator.
4. Press **Run ▶**. The app installs as **“Azan Ulm”.**

To produce an installable file: **Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)**.
The debug APK appears under `app/build/outputs/apk/debug/`.

---

## First run — grant these for reliable azan

The Home screen shows a prompt for anything missing:

1. **Notifications** — so the azan banner (with a Stop button) can appear.
2. **Exact alarms** — *Settings ▸ Apps ▸ Azan Ulm ▸ Alarms & reminders* (Android
   12+). Without this the azan can be delayed by minutes.
3. **Battery optimization exemption** — so the phone doesn't kill alarms while
   sleeping. The app opens the right screen for you.

These are normal Android requirements for any reliable alarm/azan app.

---

## Replace the placeholder audio with a real azan

The app ships with **short placeholder chimes** so it builds and makes sound out
of the box. Replace them with real recordings:

- Put your standard azan at `app/src/main/res/raw/azan.mp3`
- Put your Fajr azan at `app/src/main/res/raw/azan_fajr.mp3`

Use any azan recording you have the right to use. Keep the **exact file names**
(lowercase, no spaces). `.mp3`, `.m4a`/`.aac`, `.ogg`, or `.wav` all work — just
keep the base names `azan` and `azan_fajr`. If a file is missing, the app falls
back to the phone's default alarm sound.

---

## Scanning a new month

1. Tap **Scan a sheet from gallery** or **Take a photo of the sheet**.
2. The app reads the table and opens the **Review** screen.
3. Check the times (fix any the OCR misread), then tap **Save & schedule azan**.
4. New days are merged into storage and alarms update immediately.

Tips for a clean scan: photograph the table straight-on, fill the frame, avoid
glare and shadows.

---

## How timing works

- Times are the **local clock times** printed on the Ulm sheet. The app schedules
  them in your **phone's current timezone**, so keep the phone on Europe/Berlin
  for the printed times to be correct.
- Each enabled prayer gets its own exact alarm for its next occurrence; a small
  alarm just after midnight arms the new day; every fired alarm re-arms the rest.

---

## Project structure

```
AzanUlm/
├─ app/
│  ├─ build.gradle                     # module config, dependencies
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ assets/prayer_times_june_2026.json   # pre-loaded month
│     ├─ res/raw/azan.mp3, azan_fajr.mp3       # placeholder audio (replace)
│     └─ java/com/ulm/azan/
│        ├─ MainActivity.kt            # navigation, image capture, OCR wiring
│        ├─ data/                      # Prayer, DayTimes, PrayerStore, Settings
│        ├─ ocr/PrayerOcrParser.kt     # box-clustering table parser
│        ├─ alarm/                     # PrayerScheduler, receivers, AzanService
│        ├─ ui/                        # Home / Review / Settings (Compose)
│        └─ util/AppPermissions.kt
├─ build.gradle, settings.gradle, gradle.properties
└─ README.md
```

---

## Troubleshooting

- **No sound:** check the volume of the *Alarm* stream; confirm the azan files
  exist in `res/raw`; make sure “Azan enabled” is on in Settings.
- **Azan is late:** grant exact-alarm permission and exempt the app from battery
  optimization (some brands — Xiaomi, Huawei, Samsung — are aggressive; also add
  the app to “protected/auto-start” apps).
- **OCR missed rows:** retake the photo straight-on, or just fix the values on
  the Review screen — saving works the same.

---

*Times bundled from the Arabisch-Deutscher Verein Ulm e.V. timetable for June
2026 (Dhul-Hijjah 1447 – Muharram 1448). Verify against the official sheet.*
