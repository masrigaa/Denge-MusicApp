# Déngé ☕

<p align="center">
  <b>A lightweight, privacy-first, and completely ad-free music streaming client for Android.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.3.2-8D6E63?style=for-the-badge" alt="Version 1.3.2" />
  <img src="https://img.shields.io/badge/Android-12%2B%20(API%2031--36)-3E322E?style=for-the-badge&logo=android" alt="Android 12+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Ad--Free-100%25-2E7D32?style=for-the-badge" alt="Ad-Free" />
</p>

---

## 🌟 Highlights & Features

- **🚫 100% Ad-Free Playback**  
  Streams high-bitrate Opus audio directly via YouTube Music's client-side Innertube protocol. Zero video/audio advertisements.

- **🎧 True Background Playback & Media3**  
  Built with **AndroidX Media3 (ExoPlayer)** and a dedicated `mediaPlayback` Foreground Service. Seamless lock screen controls, Bluetooth metadata integration, and native **Xiaomi HyperOS Hyper Island** dynamic status bar support.

- **☕ "Brew & Bean" Coffee Aesthetic**  
  Immersive warm dark palette (Espresso, Warm Mocha, Hazelnut, and Creamy Latte) with full Edge-to-Edge display and smooth micro-animations.

- **🔒 Privacy-First & Zero-Backend**  
  No cloud servers, no trackers, and no mandatory Google Sign-In. Customize your personal display name locally via Settings with instant greeting updates.

- **📊 Smart Library & Monthly Reset**  
  - **Yang Sering Kamu Putar**: Top 2 most played songs with real-time play counter, automatically resetting on the 1st of every month.
  - **Liked Songs (♥)** & Custom Playlists saved directly in local SQLite via Room.
  - Full playback history and fast search cache.

- **🔗 Now Playing & Instant Share**  
  Tap the album artwork in the player to open a full HD artwork preview modal. Tap the **Share** button to instantly copy the authentic YouTube / YouTube Music track link directly to your clipboard.

- **🎚️ Audio Equalizer & Curated Genres**  
  Built-in equalizer with multiple presets (Bass Boost, Vocal, Rock, Flat) and 5 genre discovery feeds (J-Pop, Hololive/VTuber, Lofi, Western Pop, Anime OST).

---

## 📱 Download & Install

You can download the pre-compiled APK directly from the **[GitHub Releases](https://github.com/masrigaa/Denge-MusicApp/releases)** page:

1. Download **`Denge.apk`** from the latest release (v1.3.2).
2. Open the `.apk` on your Android device (Android 12 / API 31 or newer).
3. If prompted, allow installation from unknown sources.
4. Launch **Déngé** and enjoy unlimited ad-free music!

---

## 🛠️ Tech Stack & Architecture

Déngé follows **Clean Architecture & MVI/MVVM** principles:

| Component | Technology |
|---|---|
| **Language** | Kotlin 2.0+ (100% Kotlin) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Audio Engine** | AndroidX Media3 1.5+ (ExoPlayer, MediaSession) |
| **Networking** | Ktor Client 3.0+ (OkHttp Engine, Coroutines) |
| **Local Database** | Room Database 2.7+ (SQLite, KSP) |
| **Dependency Injection** | Koin 4.0+ |
| **Image Loading** | Coil 3.0+ |
| **Preferences** | EncryptedSharedPreferences |

For in-depth architectural specifications and diagrams, check:
- [`docs/Architecture.md`](docs/Architecture.md)
- [`docs/PRD.md`](docs/PRD.md)
- [`docs/Design.md`](docs/Design.md)
- [`docs/Schema.md`](docs/Schema.md)
- [`docs/Rules.md`](docs/Rules.md)

---

## 💻 Building from Source

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or newer
- **JDK** 17 or higher
- **Android SDK** API 31 - 36 installed

### Build Steps
```bash
# 1. Clone the repository
git clone https://github.com/masrigaa/Denge-MusicApp.git
cd Denge-MusicApp

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Output APK location
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Disclaimer

- **Educational Purpose**: Déngé is an open-source client developed solely for educational and personal research purposes.
- **Trademark**: YouTube and YouTube Music are registered trademarks of Google LLC. This project is not affiliated with, authorized, maintained, or endorsed by Google LLC or any of its affiliates.
- **Fair Use**: No copyrighted music is hosted or redistributed by this application or its developers; all audio streams are fetched client-side directly from public endpoints.

---

<p align="center">
  Made with ☕ by <b>Asla</b>
</p>
