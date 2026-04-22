# ReelBox — Android App

Your local video reel feed. No algorithm. No infinite scroll trap.

## Features
- Pick **any folder** on your device (including SD card / DCIM / Downloads)
- Remembers the last folder across sessions
- Shuffled vertical reel feed powered by ExoPlayer (endless looping)
- Custom adaptive icon (RB branding)
- Session timer (5 / 10 / 15 / 30 min or unlimited) — set before you start
- Auto-ends session when timer hits zero
- End screen shows videos watched + time spent
- Scans subfolders recursively

## How to Build

### Requirements
- [Android Studio Hedgehog or newer](https://developer.android.com/studio)
- Android SDK 35
- Physical Android device or emulator (API 26+)

### Steps
1. Open Android Studio → **File → Open** → select the `ReelBox` folder
2. Let Gradle sync (first sync downloads dependencies, takes ~2 min)
3. Plug in your Android device via USB (enable Developer Mode + USB Debugging)
4. Press ▶ Run — the app installs directly

### Install APK without Android Studio
1. In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Find the APK at: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer to phone and install (enable "Install unknown apps" for your file manager)

## Permissions
- `READ_MEDIA_VIDEO` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 12 and below)
- Storage Access Framework folder URI — no broad storage permission needed

## Project Structure
```
ReelBox/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/reelbox/app/
│   │   └── MainActivity.kt       ← entire app (single file)
│   └── res/
│       ├── drawable/             ← app icon vectors
│       ├── mipmap-anydpi-v26/    ← adaptive icon config
│       └── values/
│           └── themes.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```
