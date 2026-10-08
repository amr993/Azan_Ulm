# How the Al-Salam Ulm prayer times are calculated

Reverse-engineered from the mosque's printed timetables and built into the app
(`calc/PrayerCalculator.kt`), so no monthly scanning is needed.

## The method (Diyanet, computed for Ulm)

| | Rule |
|---|---|
| Location | Ulm — 48.3984° N, 9.9916° E, time zone Europe/Berlin (DST automatic) |
| Fajr | Sun 18° below the horizon; in the long-daylight window (≈ 2 May – 10 Aug) the mosque's high-latitude dawn time is used (bundled table `assets/summer_fajr.json`) |
| Sunrise | Astronomical sunrise − 7 min |
| Dhuhr | Solar noon + 5 min |
| Asr | Standard (Shafi'i) shadow length 1× + 4 min |
| Maghrib | Sunset + 7 min |
| Isha | Maghrib + 90 min (fixed) |

Sunrise/sunset use a 0.833° horizon; the sun position is evaluated once per day
(03:00 local), times rounded to the nearest minute. The offsets are the
Diyanet "temkin" safety minutes — the Aladhan API's Diyanet method (13) reports
exactly these offsets.

## Validation

| Data | Days | Result |
|---|---|---|
| 2026 sheets (Jun–Oct) | 139 | every prayer time within 1 min |
| 2025 sheets (Feb, Jul–Dec) — independent | 211 | every prayer time within 1 min (1,266/1,266) |

About 80–90% of times match to the exact minute; the remainder differ by one
minute (rounding in the source).

Note: the sheets' footer says the *congregational* Fajr prayer is held one hour
before sunrise — that is the iqama time, not the azan time in the Fajr column.
