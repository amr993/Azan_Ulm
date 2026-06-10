# Security review — Azan Ulm

Concern addressed: *"make sure this app cannot be used as a door for my phone to
be hacked."* This document records the review and the hardening that was applied.

## Bottom line

The app is, by design, a very small attack surface. The single most important
fact: **it has no INTERNET permission and makes no network calls of any kind.**
An app with no network access and no dynamic-code loading cannot act as a remote
backdoor — there is no channel to receive commands or send your data out.

## What was checked (and the result)

| Check | Result |
|---|---|
| Internet / network permission | **None.** App cannot reach the network. |
| Dangerous permissions (location, contacts, SMS, mic, storage, camera) | **None requested.** Photos use the system Photo Picker; camera uses the system camera app — no permission held by this app. |
| WebView / JavaScript bridges | **None.** (Common hacking vector — absent.) |
| Dynamic code loading (DexClassLoader, native libs, `Runtime.exec`, reflection on input) | **None.** No code is ever loaded or run from outside the APK. |
| Exported components | Minimised — see below. |
| PendingIntents | All 5 are `FLAG_IMMUTABLE` (cannot be tampered with by other apps). |
| Cleartext / HTTP traffic | Blocked at the platform level (see hardening). |
| Private data storage | App-private internal storage + `MODE_PRIVATE` prefs. Nothing world-readable/writable. |
| Data backed up off device | Disabled (see hardening). |
| Third-party SDKs (ads/analytics/trackers) | **None.** Only Google AndroidX/Compose and on-device ML Kit. |
| Secrets / API keys in code | **None** (the app needs none). |

### Exported components (the only things other apps/the system can reach)
- **MainActivity** — `exported=true` because it is the launcher icon. It ignores
  any data passed in an Intent, so there is nothing to exploit.
- **BootReceiver** — `exported=true` so the system can tell it the phone rebooted.
  It only listens to **protected system broadcasts** (BOOT_COMPLETED, TIME_SET,
  etc.) that ordinary apps are not allowed to send, and all it does is re-arm the
  prayer alarms. No untrusted input is used.
- **AzanService, AlarmReceiver, FileProvider, the widget** — all `exported=false`
  (private to the app).

## Hardening applied in this version

1. **No backup of app data off the device** — `android:allowBackup="false"`.
   Your stored times/settings never leave the phone via cloud backup.
2. **Cleartext traffic blocked + only system CAs trusted** — a
   `network_security_config.xml` forbids any non-HTTPS traffic and refuses
   user-installed certificates. Pure defense-in-depth (the app has no network
   code), but it guarantees no silent man-in-the-middle channel can ever open.
3. **Widget receiver locked down** — changed from exported to `exported="false"`.
4. **A hardened, non-debuggable release build** — `app-release.apk` is built with
   `debuggable=false`, which closes off ADB inspection/injection of the app's
   private data that is possible with a debug APK. It is signed with the standard
   debug key so it still side-loads without extra setup.

## How to stay safe when installing

- Install **`app-release.apk`** (the hardened build) rather than the debug one for
  day-to-day use. `app-debug.apk` remains as a fallback.
- Only install APKs **you built yourself** from this source (your own GitHub
  Actions run). Don't accept an "Azan Ulm" APK from anyone else.
- After installing, you can turn **"Install unknown apps"** back **off** for your
  browser/Files app — it's only needed during installation.
- Optional extra hardening: in `app/build.gradle` set `minifyEnabled true` and
  `shrinkResources true` to obfuscate the code (makes reverse-engineering harder).

## Residual notes

- A debug-key-signed APK is fine for personal side-loading; for the Google Play
  Store you would sign a release with your own private keystore.
- The bundled `azan.mp3` / `azan_fajr.mp3` are placeholder tones; replacing them
  with your own recordings has no security impact.

## Location (optional "mute azan away from home" feature)

This feature is **off by default**. When you enable it and set a home location:
- The app reads the device's location **only at the five prayer times** to compare
  the distance to home — there is no continuous tracking or geofence polling.
- Location data is used **on-device only** and is **never transmitted** (the app
  still has no INTERNET permission, and cleartext traffic is blocked).
- `ACCESS_FINE/COARSE/BACKGROUND_LOCATION` are requested only when you turn the
  feature on. If you never enable it, the app never asks for or uses location.
- It "fails open": if location is unavailable or permission is missing, the azan
  plays as normal rather than being silently muted.
