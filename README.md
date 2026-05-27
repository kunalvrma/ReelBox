# 📽️ ReelBox — Android App

> **Your local video reel feed.** No algorithm. No infinite scroll trap. Just you and your content.

ReelBox is a minimalist, intentional video player designed for those who want to enjoy their local video collection in a modern, "reel-style" vertical feed without the addictive traps of social media. It's TikTok, but for people who actually want to *stop* watching.

---

## ✨ Features

- **📂 Folder-Centric**: Pick **any folder** on your device. ReelBox recursively scans for videos, including subfolders.
- **🧠 Persistent Memory**: Remembers your last selected folder so you can jump straight back in.
- **⚖️ Weighted Shuffle**: Features a smart, non-repeating queue. Videos you've watched less frequently are prioritized, ensuring your feed stays fresh.
- **⏲️ Focused Sessions**: Every session is a fixed **5 minutes**. No more losing hours to the scroll.
- **➕ Extend or End**: When time is up, the choice is yours. Extend for another 5 minutes or end the session intentionally.
- **📊 Session Stats**: See exactly how many unique videos you watched and your total session time.
- **🎨 Modern UI**: Minimalist branding with a high-contrast neon-on-black theme.
- **👆 Intuitive Controls**: Tap to play/pause with a visual indicator. Swipe right to access the sidebar for folder management and session ending.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose — 100% declarative UI with smooth animations.
- **Engine**: Media3 ExoPlayer — Robust, high-performance video playback.
- **State**: Kotlin Coroutines & Compose State for a reactive user experience.
- **Storage**: Storage Access Framework (SAF) for secure, user-controlled file access.
- **Launch**: AndroidX Splashscreen library for a seamless, logo-free startup.

---

## 🚀 How to Build

### Requirements
- [Android Studio Ladybug](https://developer.android.com/studio) (or newer).
- Android SDK 35.
- A physical Android device or emulator (API 27+).

### Steps
1. **Open**: Launch Android Studio → **File → Open** → select `ReelBox`.
2. **Sync**: Let Gradle sync finish. (~1-2 min).
3. **Run**: Press the green **▶ Run** button.

### Standalone APK
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Locate the APK at: `app/build/outputs/apk/debug/app-debug.apk`.
3. Transfer to your phone and install (ensure "Install unknown apps" is enabled for your file manager).

---

## 🛡️ Permissions

- **`READ_MEDIA_VIDEO`**: Required for Android 13+.
- **`READ_EXTERNAL_STORAGE`**: Fallback for older Android versions (up to API 32).
- **SAF**: Used for picking and persisting folder access permissions.

---

## 📁 Project Structure

```text
ReelBox/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/reelbox/app/
│   │   └── MainActivity.kt       ← Core application logic and UI.
│   └── res/
│       ├── drawable/             ← Vector icons and assets.
│       └── values/
│           └── themes.xml        ← Splash and App theme definitions.
```

---

## 📝 License
ReelBox is open-source. Fork it, build it, and enjoy your content intentionally.
