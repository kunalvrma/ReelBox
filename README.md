# 📽️ ReelBox — Android App

> **Your local video reel feed.** No algorithm. No infinite scroll trap. Just you and your content. (And maybe some questionable videos you forgot you downloaded in 2017).

ReelBox is a minimalist, intentional video player designed for those who want to enjoy their local video collection in a modern, "reel-style" vertical feed without the addictive traps of social media. It's basically TikTok, but for people who actually want to *stop* watching at some point.

---

## ✨ Features

- **📂 Folder-Centric**: Pick **any folder** on your device. Yes, even that "Work Research" folder. We won't judge.
- **🧠 Persistent Memory**: Remembers your last selected folder so you don't have to navigate through the abyss of your file system every time.
- **🔀 Shuffled Feed**: Vertical reel feed powered by **ExoPlayer**. It's random, because variety is the spice of life, or whatever.
- **⏲️ Session Timer**: Take control of your time with preset limits (5, 10, 15, 30 min). 
- **🛑 Intentional Ending**: The app automatically ends the session when the timer hits zero. No "just one more video" excuses. The app literally stops. It's for your own good.
- **📊 Session Stats**: Summary screen shows exactly how many videos you watched. Prepare for the "I spent *how* long doing this?" realization.
- **🔍 Deep Scan**: Recursively scans subfolders. It will find that one video buried five levels deep.
- **🎨 Modern UI**: Minimalist "RB" branding. Dark theme, because we aren't savages who use light mode at 2 AM. Vertically locked, because landscape reels are a crime.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (100% declarative, 100% "why is my layout doing that?")
- **Engine**: Media3 ExoPlayer — because building a video player from scratch is a path to madness.
- **State**: Flow & Compose State for reactive UI updates.
- **Storage**: Storage Access Framework (SAF). It's secure, it's persistent, and it's slightly annoying to implement. You're welcome.
- **Language**: Kotlin. Obviously.

---

## 🚀 How to Build

### Requirements
- [Android Studio Hedgehog](https://developer.android.com/studio) (or whatever the latest animal is now).
- Android SDK 35.
- A physical Android device or an emulator that doesn't take 10 minutes to boot.

### Steps
1. **Open**: Launch Android Studio → **File → Open** → select `ReelBox`.
2. **Sync**: Let Gradle sync. Use this time to reflect on your life choices or grab a coffee. (~2 min).
3. **Connect**: Plug in your device. If it's not recognized, try a different cable. It's always the cable.
4. **Run**: Press the green **▶ Run** button and hope for the best.

### "I don't want to open Android Studio" (APK)
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Find the loot at: `app/build/outputs/apk/debug/app-debug.apk`.
3. Transfer to phone and pray the "Install unknown apps" toggle works.

---

## 🛡️ Permissions

- **`READ_MEDIA_VIDEO`**: For Android 13+.
- **`READ_EXTERNAL_STORAGE`**: For the ancient devices still out there.
- **SAF**: Because Google says so. We only see what you let us see.

---

## 📁 Project Structure

```text
ReelBox/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/reelbox/app/
│   │   └── MainActivity.kt       ← Yes, the entire app is in one file. Efficiency? Or laziness? You decide.
│   └── res/
│       ├── drawable/             ← Icons and stuff.
│       └── values/
│           ├── themes.xml        ← Dark mode or bust.
│           └── colors.xml        ← Neon green and "I can't see anything" black.
```

---

## 📝 License
ReelBox is open-source. Fork it, break it, fix it, make it yours. Just don't blame me if you get stuck in a "work research" loop.
