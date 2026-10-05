# Architecture.md — Déngé Architecture Guide

## Purpose

This document defines **how** the Déngé application is built — technical architecture, project directory structure, dependencies, data flow, and architectural decision records (ADR). It serves as the technical single source of truth for all code implementations. Product requirements are outlined in `PRD.md`; visual and UX guidelines in `Design.md`.

---

## 1. High-Level Overview

**Déngé** (`com.asla.denge`) is a native Android music player app built 100% in **Kotlin** and **Jetpack Compose**. It communicates directly with YouTube Music's Innertube API to query track metadata, albums, and artists, resolving high-quality, ad-free Opus audio streams.

Déngé is architected with a **Privacy-First & Zero-Backend** philosophy:
- **Client-Side Only**: No intermediary backend servers, cloud databases, or subscription fees. All network requests and stream resolution run directly from the client device to Innertube endpoints.
- **Local Profile**: Personalized user display names are managed entirely on-device via `EncryptedSharedPreferences`, without requiring complex Google account logins.
- **Background Playback**: Powered by **AndroidX Media3 (ExoPlayer)** hosted inside a `mediaPlayback` Foreground Service, fully integrated with Android's system **MediaSession**, lock screen media controls, and status bar islands (such as Xiaomi HyperOS Hyper Island).
- **Offline Storage**: All Liked Songs, playback history, top played tracks (with monthly reset counters), and custom playlists are stored locally in SQLite via **Room Database**.

---

## 2. Tech Stack

| Layer | Technology | Version | Rationale |
|:---|:---|:---:|:---|
| Language | Kotlin | 2.0+ | Modern, null-safe, native Android performance |
| UI Framework | Jetpack Compose | 1.7+ | Declarative, reactive, Material 3 design system |
| Design System | Brew & Bean Coffee Theme | M3 1.3+ | Warm mocha/latte/cream palette with rounded surfaces |
| Min SDK | Android 12 (API 31) | — | Supports modern MediaSession & Dynamic Island controls |
| Target / Compile | Android 16 (API 36) | — | Maximum compatibility with modern Android versions |
| Audio Player | AndroidX Media3 (ExoPlayer) | 1.5+ | Google's official standard for background audio |
| OS Integration | Xiaomi HyperOS Hyper Island | — | Status bar dynamic island pill & lockscreen art |
| Networking | Ktor Client (OkHttp engine) | 3.0+ | Asynchronous, lightweight, native coroutines |
| Serialization | Kotlinx Serialization | 1.7+ | High-speed, type-safe JSON parsing |
| Local Database | Room (SQLite) | 2.7+ (KSP) | Official Android ORM with Flow & coroutines |
| Preferences | EncryptedSharedPreferences | 1.1+ | Secure local profile and configuration storage |
| DI | Koin | 4.0+ | Lightweight dependency injection without code-gen overhead |
| Navigation | Jetpack Navigation Compose | 2.8+ | Type-safe navigation framework |
| Image Loading | Coil 3 | 3.0+ | Modern Kotlin Multiplatform & Compose image loader |
| Build System | Gradle (Kotlin DSL) | 8.7+ | Standard Android build tooling |

---

## 3. Directory / Source Map

```
AdsFreeMusic/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/asla/denge/
│   │       │   ├── AdsFreeApp.kt                # Application class, Koin initialization & CrashHandler
│   │       │   ├── MainActivity.kt              # Single Activity, Compose host, Edge-to-Edge
│   │       │   ├── CrashActivity.kt             # Crash recovery screen for uncaught fatal exceptions
│   │       │   │
│   │       │   ├── ui/                          # ── Presentation Layer ──
│   │       │   │   ├── components/              # Reusable UI components
│   │       │   │   │   ├── MiniPlayer.kt        # Persistent floating bottom player
│   │       │   │   │   ├── TrackItem.kt         # Track list item, menu (⋮), queue actions
│   │       │   │   │   ├── TrackArtwork.kt      # Rounded album artwork & fallback placeholder
│   │       │   │   │   ├── GenreSelectionDialog.kt # Modal dialog for managing Home genres
│   │       │   │   │   └── NameInputDialog.kt   # Dialog for updating listener profile name
│   │       │   │   ├── navigation/              # NavHost, NavRoutes, BottomNavigationBar
│   │       │   │   │   └── NavGraph.kt
│   │       │   │   ├── screens/                 # Primary app screens
│   │       │   │   │   ├── home/                # Home feed, genre shelves, quick play
│   │       │   │   │   ├── library/             # Library, local playlists, history, liked songs
│   │       │   │   │   ├── player/              # Full player, artwork preview, share, queue
│   │       │   │   │   ├── search/              # Instant debounced search
│   │       │   │   │   └── settings/            # Genre preferences, EQ, theme, app info
│   │       │   │   └── theme/                   # Brew & Bean color palette, Type, Shape
│   │       │   │
│   │       │   ├── domain/                      # ── Domain Layer (Pure Kotlin) ──
│   │       │   │   ├── model/                   # Track, Playlist, Artist, Album, UserAccount
│   │       │   │   ├── repository/              # MusicRepository, AuthRepository interfaces
│   │       │   │   └── usecase/                 # SearchMusicUseCase, GetStreamUrlUseCase
│   │       │   │
│   │       │   ├── data/                        # ── Data Layer ──
│   │       │   │   ├── local/                   # Room Database, DAOs, Entities
│   │       │   │   │   ├── db/MusicDatabase.kt
│   │       │   │   │   ├── dao/                 # TrackDao, PlaylistDao, HistoryDao
│   │       │   │   │   └── entity/              # CachedTrackEntity, PlaybackHistoryEntity, etc.
│   │       │   │   ├── remote/                  # Ktor Innertube API Client, parsers
│   │       │   │   │   └── innertube/           # Request/response builders & deciphering
│   │       │   │   └── repository/              # MusicRepositoryImpl, AuthRepositoryImpl
│   │       │   │
│   │       │   ├── player/                      # ── Playback Layer ──
│   │       │   │   ├── PlaybackService.kt       # MediaSessionService Foreground Service
│   │       │   │   ├── PlayerManager.kt         # Playback state manager, queue & ExoPlayer
│   │       │   │   └── AudioEffectsManager.kt   # Android audio equalizer controls
│   │       │   │
│   │       │   ├── di/                          # ── Dependency Injection ──
│   │       │   │   └── Modules.kt               # Koin modules (app, net, db, player, vm)
│   │       │   │
│   │       │   └── util/                        # Utility helpers, AccountPicker, Constants
│   │       │
│   │       ├── res/                             # Drawable icons, launcher icons, strings
│   │       └── AndroidManifest.xml              # Audio permissions, mediaPlayback foreground service
│   │
│   └── build.gradle.kts                         # App module configuration
│
├── docs/                                        # Official project documentation
│   ├── Architecture.md                          # Technical architecture (this file)
│   ├── Design.md                                # Brew & Bean UI/UX guidelines
│   ├── PRD.md                                   # Product requirements document
│   ├── Rules.md                                 # Code style standards & guardrails
│   └── Schema.md                                # Room SQLite database schema
│
├── build.gradle.kts                             # Root build configuration
├── settings.gradle.kts                          # Gradle repository & module settings
├── gradle.properties                            # JVM memory allocation & flags
├── .gitignore                                   # Git exclusion rules
└── README.md                                    # GitHub landing page & release guide
```

---

## 4. Data Flow

### 4.1 Playback Data Flow

```
┌───────────┐      ┌──────────────┐      ┌─────────────────┐      ┌────────────────┐
│   User    │─────▶│ Screen / UI  │─────▶│   ViewModel     │─────▶│ PlayerManager  │
│(Tap Track)│      │  (Compose)   │      │                 │      │                │
└───────────┘      └──────────────┘      └─────────────────┘      └───────┬────────┘
                                                                          │
                                                                          ▼
┌──────────────────┐      ┌───────────────┐      ┌────────────────────────┴────────┐
│ Notification &   │◀─────│PlaybackService│◀─────│ MusicRepository.getStreamUrl()  │
│ Hyper Island     │      │ (Foreground)  │      │ (Resolve direct Opus audio url) │
└──────────────────┘      └───────────────┘      └─────────────────────────────────┘
```

1. The user taps a track in the UI (or triggers "Play Next" / "Add to Queue").
2. `PlayerManager` receives the request and resolves the stream URL via `MusicRepository`.
3. `InnertubeClient` extracts the clean direct Opus playback stream without ad segments.
4. `PlaybackService` launches or updates the Foreground Service with an active MediaSession.
5. The Android System and dynamic status bars receive media metadata and expose controls on the lock screen and notification shade.

### 4.2 Search Flow

```
User enters query ──▶ Debounce (300ms) ──▶ InnertubeClient.search()
                                                   │
                                                   ▼
Compose UI ◀── StateFlow<SearchUiState> ◀── MusicRepository.search()
```

### 4.3 Profile & Local Name Flow

1. The listener's profile name is stored locally in `EncryptedSharedPreferences`.
2. Users can change their name anytime via **Settings ⚙️ -> Edit Profile Name**.
3. The display name immediately reflects across the Home greeting ("Good morning, [Name] ☕").
4. No account credentials, passwords, or emails are ever requested.

---

## 5. Architectural Decision Records (ADR)

### ADR-001: Direct Innertube Client vs WebView
- **Decision**: Connect directly over HTTP to YouTube Music Innertube API endpoints.
- **Rationale**: Provides granular control over selecting high-bitrate, ad-free Opus audio streams, enables genuine background playback, and consumes far less RAM/battery than rendering a WebView.

### ADR-002: AndroidX Media3 Foreground Service
- **Decision**: Utilize AndroidX Media3 (`PlaybackService`) with `mediaPlayback` foreground service type.
- **Rationale**: Fully compliant with Android 12+ background limits, prevents OS process termination when minimized, and supports native MediaSession status bar integrations.

### ADR-003: Koin Dependency Injection
- **Decision**: Use Koin rather than Dagger/Hilt.
- **Rationale**: Significantly faster compilation times (no kapt/ksp code-gen overhead for DI), concise Kotlin DSL, and lightweight runtime footprint.

### ADR-004: Room SQLite with Automatic Monthly Reset
- **Decision**: Record playback history and play count frequency in Room database, with an automated monthly reset on the 1st of every month for "Your Top Plays".
- **Rationale**: Keeps top-played music recommendations fresh and relevant to the user's current listening habits.

---

## 6. Security & Privacy Principles

1. **Zero Data Telemetry**: No tracking SDKs, analytics, or third-party diagnostic reporting.
2. **Encrypted Local Storage**: Data is kept securely in the app's private sandbox (`data/data/com.asla.denge`).
3. **No Credential Phishing**: The app never prompts for Google passwords or sensitive private keys.
4. **Ad-Free Pipeline**: Direct audio URL extractors target pure audio stream formats, guaranteeing that video ad segments are never passed to ExoPlayer.
