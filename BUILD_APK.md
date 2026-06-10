# Getting an installable APK onto your phone

This app must be compiled by the Android build system. Here are two easy ways to
get `app-debug.apk` — pick whichever you prefer.

> Optional but recommended first: replace the two placeholder chimes at
> `app/src/main/res/raw/azan.mp3` and `azan_fajr.mp3` with real azan recordings
> (keep the file names). You can also do this later and rebuild.

---

## Option A — Build in the cloud, no Android Studio (recommended)

You only need a free GitHub account. GitHub compiles the APK for you.

1. Go to **github.com** → sign in → **New repository**. Name it `azan-ulm`
   (Public or Private both work). Create it.
2. **Upload the project.** On the empty repo page click **“uploading an existing
   file”**, then drag in **everything inside the `AzanUlm` folder** (keep the
   folder structure intact). Make sure the hidden folder
   **`.github/workflows/build-apk.yml`** is included — it tells GitHub how to
   build. Commit the files.
   - If drag-and-drop hides the `.github` folder, use **GitHub Desktop** or
     `git` to push the whole folder instead.
3. GitHub starts building automatically. Open the **Actions** tab and wait for
   the green check ✓ (about 3–5 minutes the first time).
4. **Download the APK** either way:
   - **Releases** (right sidebar of the repo) → **“Latest debug APK”** →
     download **`app-debug.apk`**. This is the easiest link to open directly on
     your phone’s browser.
   - or open the finished **Actions** run → **Artifacts** →
     **`azan-ulm-debug-apk`** (a zip containing the APK).
5. On your phone, open `app-debug.apk` and allow **“Install unknown apps”** when
   prompted, then install.

To rebuild later (e.g., after adding real azan audio), just upload the changed
files again — a new APK is produced automatically.

---

## Option B — Build locally in Android Studio

1. Open the `AzanUlm` folder in **Android Studio**; let Gradle sync (it creates
   the Gradle wrapper automatically).
2. **Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s).**
3. When it finishes, click **locate** →
   `app/build/outputs/apk/debug/app-debug.apk`.
4. Transfer that file to your phone (USB, Drive, email) and install it, or just
   plug in your phone and press **Run ▶** to install directly.

---

## Notes

- A **debug APK** is signed with Android’s debug key — perfect for personal
  testing and side-loading. For the Google Play Store you’d make a **release**
  build signed with your own key.
- After installing, grant **notifications**, **exact alarms**, and **battery
  optimization exemption** when the app asks — these make the azan fire on time.
- Minimum phone version: **Android 8.0 (API 26)**.
