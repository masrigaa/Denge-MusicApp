# Architecture.md — Déngé Architecture Guide

## Tujuan

Dokumen ini mendefinisikan **bagaimana** aplikasi Déngé dibangun — arsitektur teknis, struktur proyek, dependensi, alur data, serta keputusan arsitektur (ADR). Dokumen ini menjadi sumber kebenaran teknis (single source of truth) untuk seluruh implementasi kode. Kebutuhan produk dijelaskan di `PRD.md`; desain visual di `Design.md`.

---

## 1. High-Level Overview

**Déngé** (`com.asla.denge`) adalah aplikasi pemutar musik Android native yang dibangun 100% menggunakan **Kotlin** dan **Jetpack Compose**. Aplikasi ini berkomunikasi langsung dengan YouTube Music Innertube API untuk mencari metadata lagu, album, artis, serta menyelesaikan streaming audio berformat Opus (ad-free) berkualitas tinggi.

Déngé dirancang dengan filosofi **Privacy-First & Zero-Backend**:
- **Client-Side Only**: Tidak ada server perantara (backend), database cloud, atau biaya hosting. Seluruh pemrosesan dan permintaan stream berjalan langsung dari perangkat pengguna ke Innertube.
- **Local Profile**: Personalisasi nama pengguna dikelola langsung secara lokal menggunakan `EncryptedSharedPreferences` tanpa mewajibkan Google login yang rumit.
- **Background Playback**: Menggunakan **AndroidX Media3 (ExoPlayer)** di dalam Foreground Service bertipe `mediaPlayback`, terintegrasi dengan **MediaSession** Android serta status bar / Dynamic Island (Xiaomi HyperOS Hyper Island).
- **Offline Storage**: Seluruh lagu yang disukai (Liked Songs), riwayat pemutaran, lagu yang sering diputar (dengan reset bulanan), dan playlist custom disimpan lokal di SQLite melalui **Room Database**.

---

## 2. Tech Stack

| Layer              | Technology                          | Version     | Rationale                                        |
|--------------------|-------------------------------------|-------------|--------------------------------------------------|
| Language           | Kotlin                              | 2.0+        | Modern, aman dari null, performa native Android   |
| UI Framework       | Jetpack Compose                     | 1.7+        | Deklaratif, reactive, Material 3                 |
| Design System      | Brew & Bean Coffee Theme            | M3 1.3+     | Palet warm mocha/latte/cream, rounded surfaces   |
| Min SDK            | Android 12 (API 31)                 | —           | Mendukung MediaSession modern & Dynamic Island   |
| Target / Compile   | Android 16 (API 36)                 | —           | Kompatibilitas versi Android terbaru             |
| Audio Player       | AndroidX Media3 (ExoPlayer)         | 1.5+        | Standar resmi Google untuk background audio      |
| OS Integration     | Xiaomi HyperOS Hyper Island         | —           | Status bar Dynamic Island pill & lockscreen art  |
| Networking         | Ktor Client (OkHttp engine)         | 3.0+        | Asynchronous, ringan, native coroutines          |
| Serialization      | Kotlinx Serialization               | 1.7+        | JSON parsing berkecepatan tinggi & type-safe     |
| Local Database     | Room (SQLite)                       | 2.7+ (KSP)  | ORM resmi Android dengan Flow & coroutine        |
| Preferences        | EncryptedSharedPreferences          | 1.1+        | Penyimpanan profil nama pengguna & konfigurasi   |
| DI                 | Koin                                | 4.0+        | Dependency Injection ringan tanpa code-gen       |
| Navigation         | Jetpack Navigation Compose          | 2.8+        | Type-safe navigation                             |
| Image Loading      | Coil 3                              | 3.0+        | Kotlin Multiplatform & Compose image loader      |
| Build System       | Gradle (Kotlin DSL)                 | 8.7+        | Standar Android build tooling                    |

---

## 3. Directory / Source Map

```
AdsFreeMusic/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/asla/denge/
│   │       │   ├── AdsFreeApp.kt                # Application class, inisialisasi Koin & CrashHandler
│   │       │   ├── MainActivity.kt              # Single Activity, Compose host, Edge-to-Edge
│   │       │   ├── CrashActivity.kt             # Layar recovery saat fatal exception
│   │       │   │
│   │       │   ├── ui/                          # ── Presentation Layer ──
│   │       │   │   ├── components/              # Komponen reusable
│   │       │   │   │   ├── MiniPlayer.kt        # Player mengambang di bawah layar
│   │       │   │   │   ├── TrackItem.kt         # Item baris lagu, menu (⋮), aksi antrean
│   │       │   │   │   ├── TrackArtwork.kt      # Gambar album art rounded & placeholder
│   │       │   │   │   ├── GenreSelectionDialog.kt # Modal ganti preferensi genre
│   │       │   │   │   └── NameInputDialog.kt   # Dialog ubah nama panggilan
│   │       │   │   ├── navigation/              # NavHost, NavRoutes, BottomNavigationBar
│   │       │   │   │   └── NavGraph.kt
│   │       │   │   ├── screens/                 # Layar utama
│   │       │   │   │   ├── home/                # Beranda, genre feeds, quick play
│   │       │   │   │   ├── library/             # Pustaka, playlist lokal, riwayat, liked songs
│   │       │   │   │   ├── player/              # Full player, artwork preview, share, queue
│   │       │   │   │   ├── search/              # Pencarian instan debounced
│   │       │   │   │   └── settings/            # Pengaturan genre, EQ, tema, info app
│   │       │   │   └── theme/                   # Brew & Bean color palette, Type, Shape
│   │       │   │
│   │       │   ├── domain/                      # ── Domain Layer (Pure Kotlin) ──
│   │       │   │   ├── model/                   # Track, Playlist, Artist, Album, UserAccount
│   │       │   │   ├── repository/              # MusicRepository, AuthRepository interfaces
│   │       │   │   └── usecase/                 # SearchMusicUseCase, GetStreamUrlUseCase
│   │       │   │
│   │       │   ├── data/                        # ── Data Layer ──
│   │       │   │   ├── local/                   # Room Database, DAO, Entity
│   │       │   │   │   ├── db/MusicDatabase.kt
│   │       │   │   │   ├── dao/                 # TrackDao, PlaylistDao, HistoryDao
│   │       │   │   │   └── entity/              # CachedTrackEntity, PlaybackHistoryEntity, etc.
│   │       │   │   ├── remote/                  # Ktor Innertube API Client, parsers
│   │       │   │   │   └── innertube/           # Request/response builders & deciphering
│   │       │   │   └── repository/              # MusicRepositoryImpl, AuthRepositoryImpl
│   │       │   │
│   │       │   ├── player/                      # ── Playback Layer ──
│   │       │   │   ├── PlaybackService.kt       # MediaSessionService Foreground Service
│   │       │   │   ├── PlayerManager.kt         # State manager pemutaran, antrean & ExoPlayer
│   │       │   │   └── AudioEffectsManager.kt   # Kontrol Equalizer audio Android
│   │       │   │
│   │       │   ├── di/                          # ── Dependency Injection ──
│   │       │   │   └── Modules.kt               # Koin definitions (app, net, db, player, vm)
│   │       │   │
│   │       │   └── util/                        # Helper fungsi, AccountPicker, Constants
│   │       │
│   │       ├── res/                             # Drawable icons, launcher icons, strings
│   │       └── AndroidManifest.xml              # Izin audio, foreground service mediaPlayback
│   │
│   └── build.gradle.kts                         # Konfigurasi modul app
│
├── docs/                                        # Dokumentasi resmi
│   ├── Architecture.md                          # Arsitektur teknis (file ini)
│   ├── Design.md                                # Panduan UI/UX Brew & Bean
│   ├── PRD.md                                   # Spesifikasi produk
│   ├── Rules.md                                 # Standar kode & kontribusi
│   └── Schema.md                                # Skema database lokal Room
│
├── build.gradle.kts                             # Root build configuration
├── settings.gradle.kts                          # Modul repositori Gradle
├── gradle.properties                            # Alokasi memori JVM Gradle
├── .gitignore                                   # Rule pengabaian file Git
└── README.md                                    # GitHub Landing & panduan rilis
```

---

## 4. Alur Data (Data Flow)

### 4.1 Alur Pemutaran Audio (Playback Data Flow)

```
┌───────────┐      ┌──────────────┐      ┌─────────────────┐      ┌────────────────┐
│ Pengguna  │─────▶│ Screen / UI  │─────▶│   ViewModel     │─────▶│ PlayerManager  │
│ (Tap Lagu)│      │  (Compose)   │      │                 │      │                │
└───────────┘      └──────────────┘      └─────────────────┘      └───────┬────────┘
                                                                          │
                                                                          ▼
┌──────────────────┐      ┌───────────────┐      ┌────────────────────────┴────────┐
│ Notification &   │◀─────│PlaybackService│◀─────│ MusicRepository.getStreamUrl()  │
│ Hyper Island     │      │ (Foreground)  │      │ (Resolve direct Opus audio url) │
└──────────────────┘      └───────────────┘      └─────────────────────────────────┘
```

1. Pengguna memilih lagu di UI (atau opsi "Putar Berikutnya" / "Tambah ke Antrean").
2. `PlayerManager` menerima request dan meminta resolusi stream URL ke `MusicRepository`.
3. `InnertubeClient` mengekstrak direct playback stream audio tanpa segmen iklan.
4. `PlaybackService` memulai Foreground Service dengan MediaSession aktif.
5. Android System dan Xiaomi Hyper Island menerima notifikasi audio dan menampilkan kontrol di status bar serta lockscreen.

### 4.2 Alur Pencarian (Search Flow)

```
Pengguna mengetik query ──▶ Debounce (300ms) ──▶ InnertubeClient.search()
                                                       │
                                                       ▼
Compose UI ◀── StateFlow<SearchUiState> ◀── MusicRepository.search()
```

### 4.3 Alur Profil & Nama Lokal

1. Nama pengguna disimpan langsung di `EncryptedSharedPreferences`.
2. Pengguna dapat mengubah nama kapan saja melalui menu **Pengaturan ⚙️ -> Ubah Nama Panggilan**.
3. Nama tersebut otomatis tampil di greeting Beranda ("Halo, [Nama Pengguna] ☕").
4. Tidak diperlukan kredensial akun, username, maupun password.

---

## 5. Keputusan Arsitektur (ADR)

### ADR-001: Innertube API Langsung (Client-Side) Dibandingkan WebView
- **Keputusan**: Berkomunikasi langsung via HTTP ke endpoint Innertube YouTube Music.
- **Alasan**: Memberikan kontrol total atas pemilihan stream Opus berkualitas tinggi tanpa iklan, memungkinkan background playback sejati, dan konsumsi memori/baterai jauh lebih hemat daripada merender WebView.

### ADR-002: AndroidX Media3 Foreground Service
- **Keputusan**: Menggunakan AndroidX Media3 (`PlaybackService`) dengan tipe `mediaPlayback`.
- **Alasan**: Kompatibel penuh dengan spesifikasi Android 12+, tidak dimatikan oleh sistem saat diminimalkan, dan mendukung integrasi native status bar / HyperOS Hyper Island.

### ADR-003: Koin Dependency Injection
- **Keputusan**: Menggunakan Koin daripada Dagger/Hilt.
- **Alasan**: Waktu kompilasi jauh lebih cepat (tanpa kapt/ksp overhead untuk DI), DSL Kotlin yang mudah dibaca, dan modularitas yang sangat fleksibel.

### ADR-004: Room SQLite dengan Reset Bulanan Otomatis
- **Keputusan**: Melacak riwayat dan frekuensi putar lagu di Room database, dengan filter reset bulanan pada tanggal 1 setiap bulannya untuk bagian "Yang Sering Kamu Putar".
- **Alasan**: Menjaga daftar lagu teratas selalu relevan dengan preferensi mendengarkan pengguna di bulan berjalan.

---

## 6. Prinsip Keamanan & Privasi

1. **Zero Data Telemetry**: Tidak ada analitik, pelacakan privasi, atau pengiriman data ke pihak ketiga.
2. **Encrypted Local Storage**: Data lokal disimpan di penyimpanan privat internal aplikasi (`data/data/com.asla.denge`).
3. **No Credential Phishing**: Aplikasi tidak meminta password akun Google maupun kredensial sensitif.
4. **Ad-Free Pipeline**: Filter URL audio murni mengambil format audio stream, sehingga konten iklan tidak pernah dimuat ke dalam ExoPlayer.

---

*Terakhir diperbarui: 27 September 2026 — Déngé v1.3.2*
