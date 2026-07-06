# 📽️ ReelBox — Your local video reel feed.

A minimalist video player built for people who want to stop watching.

A few months ago, I noticed I was losing hours to the "infinite scroll" trap. TikTok, Reels, Shorts—they all work because they never end. You don't decide to stop; you just eventually pass out from exhaustion.

I wanted the modern "reel" experience, but for my own content. No algorithms. No random influencers. No "suggested for you" garbage. Just the videos I actually chose to download.

So I built ReelBox. It's TikTok, but for people who actually want to get their shit together and stop scrolling.

---

## ✨ Why this exists

Every video app is designed to keep you inside. ReelBox is designed to kick you out.

- **📂 Folder-Centric**: Pick a folder. Any folder. The app recursively scans it in the background while you start watching.
- **⏲️ The 5-Minute Rule**: Every session is exactly 5 minutes. When time is up, the app stops. You can extend for another 5 minutes if you really need to, but the choice is intentional. 
- **⚖️ Smart Shuffle**: It remembers what you've watched. Videos you haven't seen in a while get bumped to the front so your feed stays fresh.
- **🧠 Persistent Memory**: It remembers your folder so you don't have to navigate your file system every single time you open the app.
- **🎨 Glass UI**: A modern, neon-on-black aesthetic with glassmorphism and smooth animations. It looks premium, but doesn't track your soul.

---

## 🛠️ The Tech Bit

I kept this simple so it actually works.

- **Engine**: Media3 ExoPlayer for the heavy lifting.
- **UI**: 100% Jetpack Compose.
- **Speed**: Folder scanning runs on background threads so the first video loads instantly.
- **Startup**: Uses the AndroidX Splashscreen library for a clean, logo-free transition.

---

## 🚀 How to Build

### Requirements
- Android Studio Ladybug (or newer).
- A physical phone or an emulator that doesn't lag.

### Steps
1. **Open**: Launch Android Studio -> **File -> Open** -> select `ReelBox`.
2. **Sync**: Wait for Gradle to finish its ritual (~1-2 min).
3. **Run**: Hit the green play button.

### Just give me the APK
If you don't want to mess with code, build the APK from **Build -> Build APK(s)**. You'll find it at `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to your phone and install.

---

## 🛡️ Permissions

- **`READ_MEDIA_VIDEO`**: For Android 13+.
- **SAF (Storage Access Framework)**: Because Google insists on it. We only see the folders you specifically pick.

---

## 📁 Project Structure

I've kept this lean.

```text
ReelBox/
├── app/src/main/
│   ├── java/com/reelbox/app/
│   │   └── MainActivity.kt       ← Where the magic happens (UI + Logic).
│   └── res/
│       ├── drawable/             ← Icons and glass assets.
│       └── values/
│           └── themes.xml        ← The glass and neon definitions.
```

---

## 📝 Final Thought
ReelBox isn't about productivity—it's about intentionality. Enjoy your content, but don't let it own your time. 

Fork it, build it, and stop scrolling.
