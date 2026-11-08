# SMART LIFE – STARK EDITION

Native Android companion app (Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, BLE, Health Connect) with an Iron Man / J.A.R.V.I.S. HUD theme, built for the Fastrack Jupiter R2 on Android 16 (API 36; minSdk 26).

There is no Shop, shopping, advertising, marketplace or purchase link anywhere in the code, resources or manifest, and the app has no analytics or network permission.

## Install (phone)

1. Get an APK: push this folder to a GitHub repository, open **Actions → Android build → latest run → Artifacts**, and download `smartlife-stark-debug-apk`.
2. Copy the APK to the phone and open it. Allow "Install unknown apps" for your file manager or browser when asked.
3. Open the app and, on the **Device** tab, grant Bluetooth permission, then scan.

The debug APK installs as `com.stark.smartlife.debug`. The release APK is unsigned unless you add signing secrets (below), and an unsigned APK cannot be installed.

## Build

- **GitHub Actions** (`.github/workflows/android.yml`): builds debug and release APKs on every push.
  Optional release signing secrets: `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
- **Android Studio**: open this folder, let it sync (it creates the Gradle wrapper), run the `app` configuration.
- **Command line** with the Android SDK (platform 36) and JDK 17: `gradle :app:assembleDebug`.

The Gradle wrapper files are not included because they could not be generated in the environment this project was written in; Android Studio and the workflow both provide Gradle 8.14.3 themselves.

## Status of this delivery

This project was written without an Android SDK or network access, so it has **not been compiled or run**. Treat the first CI run as the first build: expect a small number of compile or dependency-version fixes. Library versions (AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.2, Compose BOM 2025.10.00, Room 2.8.2, Health Connect 1.1.0, security-crypto 1.1.0-alpha06) were chosen from memory and could not be checked against the repositories.

## What the Jupiter R2 supports (researched)

From retailer and manufacturer listings: 1.38" TFT (LCD) 240×240 touchscreen, IP68, up to 5-day battery, heart rate, SpO₂, sleep, steps, calories, SingleSync Bluetooth calling, AI voice assistant, functional crown, 100+ sports modes, 100+ watch faces, Bluetooth 5.1. It pairs with the official Fastrack Smart World app (`com.titan.fastrack.reflex`).

- **Always-On Display: not supported.** No listing for the Jupiter R2 mentions it; Fastrack lists AOD only on AMOLED models such as the Xtreme Pro. The app therefore shows a disabled switch with this explanation and sends nothing (`aod/AodCapability.kt`).
- **Vendor Bluetooth protocol: not public.** Fastrack/Titan watches use proprietary GATT services. One third-party project reports a service `0x27F0` for a different model (FT_38112); that is unverified for the Jupiter R2 and was **not** adopted. No vendor UUID or command is hard-coded, and the app never writes to the watch.

## What works

| Area | Behaviour |
|---|---|
| Five tabs | Home, Health, Activity, Device, Profile |
| Themes | Stark (red/gold/blue HUD), Dark, Light, Follow system; animated arc reactor driven by real step progress |
| Languages | English, Tamil, Hindi (per-app locale; untranslated strings fall back to English) |
| Bluetooth | Scan, connect, disconnect, remember last watch, foreground service while syncing, GATT inspector listing every service and characteristic the watch exposes |
| Live data over BLE | Only through standard Bluetooth SIG services, if the watch exposes them: Heart Rate (0x180D) and Battery (0x180F). Read-only |
| Health Connect | Permission flow and 7-day import of steps, distance, active calories, heart rate, SpO₂ and sleep, stored in Room. Read-only. Data exists there only if another app, such as the official Fastrack app, wrote it |
| Charts and reports | 7-day charts for steps, calories, heart rate, SpO₂, sleep; today's heart-rate line |
| Workouts | Phone-timed start/stop with sport and duration history |
| Alarms and reminders | Phone-side daily alarms (reboot-safe) |
| Notifications | Notification-listener records call and app alerts (app name and title only) locally |
| Bluetooth calling | Explained, with a shortcut to Android Bluetooth settings (calling uses classic Bluetooth audio paired by Android) |
| Privacy | Profile and last watch encrypted with Android Keystore; no backup; delete-all-data button |

## What is intentionally not implemented

These need the proprietary protocol, so they are shown honestly as unavailable rather than faked: sending alarms, notifications or calls to the watch; watch-face install and gallery; stress score and sleep stages; AOD; firmware update; find-my-watch. To add one, use the GATT inspector on real hardware, record the traffic, and add a verified adapter beside `ble/Protocol.kt`.

## Layout

```
app/src/main/java/com/stark/smartlife/
  data/     Room entities/DAOs, DataStore settings, encrypted profile
  ble/      scanner, GATT client, standard-service decoding, foreground service
  health/   Health Connect repository
  notify/   notification listener
  alarms/   scheduler and receivers
  aod/      AOD capability (unsupported)
  ui/       theme, HUD components, ViewModel, five screens
```
