# Déngé ☕

<p align="center">
  <b>A lightweight, privacy-first, and completely ad-free music streaming client for Android.</b>
</p>

<p align="center">
  <a href="https://github.com/masrigaa/Denge-MusicApp/releases/latest">
    <img src="https://img.shields.io/github/v/release/masrigaa/Denge-MusicApp?color=8D6E63&label=Latest%20Release&style=for-the-badge&logo=github" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Android-12%2B%20(API%2031--36)-3E322E?style=for-the-badge&logo=android" alt="Android 12+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Ad--Free-100%25-2E7D32?style=for-the-badge" alt="Ad-Free" />
</p>

<p align="center">
  <a href="https://github.com/masrigaa/Denge-MusicApp/releases/latest/download/Denge.apk">
    <img src="https://img.shields.io/badge/📥_Download_APK-Direct_Download-8D6E63?style=for-the-badge&logo=android&logoColor=white" height="42" alt="Download APK" />
  </a>
  &nbsp;
  <a href="https://github.com/masrigaa/Denge-MusicApp/releases">
    <img src="https://img.shields.io/badge/View_All_Releases-Changelog-3E322E?style=for-the-badge&logo=github" height="42" alt="All Releases" />
  </a>
</p>

---

## 📱 Download & Quick Start

Pemasangan sangat mudah tanpa perlu mendaftar akun atau login:

1. **Unduh Aplikasi:**  
   👉 **[Klik di sini untuk Download `Denge.apk` (Versi Terbaru)](https://github.com/masrigaa/Denge-MusicApp/releases/latest/download/Denge.apk)**
2. **Pasang (Install):**  
   Buka file `.apk` yang telah diunduh di perangkat Android kamu (mendukung Android 12 ke atas). Jika muncul konfirmasi keamanan, pilih **"Izinkan penginstalan dari sumber ini"**.
3. **Mulai Mendengarkan:**  
   Buka aplikasi **Déngé** dan nikmati jutaan katalog musik tanpa gangguan iklan.

> 💡 **Tips Pengguna Xiaomi / HyperOS / MIUI:**  
> Agar musik tidak terhenti saat layar HP mati, buka **Setelan HP > Aplikasi > Kelola Aplikasi > Déngé > Penghemat Baterai**, lalu pilih **"Tidak ada pembatasan (No restrictions)"**.

---

## 🌟 Highlights & Features

- **🚫 100% Ad-Free Audio**  
  Mengalirkan audio berkualitas tinggi (Opus / AAC) langsung melalui protokol klien YouTube Music. Bebas total dari jeda iklan suara maupun sponsor video.

- **🎧 True Background Playback & Media3**  
  Dibangun dengan arsitektur modern **AndroidX Media3 (ExoPlayer)** dan Foreground Service bertipe `mediaPlayback`. Musik tetap berjalan stabil saat layar mati atau aplikasi diminimalkan, dilengkapi kontrol lockscreen dan integrasi status bar dinamis (**Xiaomi HyperOS Hyper Island**).

- **☕ "Brew & Bean" Warm Aesthetic**  
  Antarmuka bertema kopi yang hangat, tenang, dan premium (Espresso, Warm Mocha, Hazelnut, Creamy Latte) berkonsep penuh *Edge-to-Edge* serta transisi halus.

- **🔒 Privacy-First & Zero-Backend**  
  Aplikasi berjalan murni di perangkat pengguna (*client-side only*). Tidak memerlukan login akun Google, tanpa server perantara, dan tidak mengumpulkan analitik privasi apapun. Kamu bebas mengubah nama panggilan profilmu langsung dari Pengaturan.

- **📊 Smart Library & Monthly Reset**  
  - **Yang Sering Kamu Putar**: Menampilkan 2 lagu teratas yang paling sering didengarkan lengkap dengan penghitung putaran real-time yang otomatis di-reset setiap tanggal 1 setiap bulannya.
  - **Liked Songs (♥)** & Playlist Kustom tersimpan aman di database lokal SQLite (Room).
  - Riwayat pemutaran kronologis dan cache pencarian cepat.

- **🔗 Now Playing & Instant Share**  
  Ketuk album artwork di layar Now Playing untuk membuka modal preview beresolusi tinggi. Tombol **Share** memungkinkanmu menyalin link lagu resmi YouTube / YouTube Music langsung ke clipboard dalam satu sentuhan.

- **🎚️ Audio Equalizer & Curated Genres**  
  Equalizer audio bawaan dengan berbagai preset (Bass Boost, Vocal, Rock, Flat) serta 5 kurasi genre musik pilihan di Beranda (J-Pop, Hololive / VTuber, Lofi, Western Pop, Anime OST).

---

## 🛠️ Tech Stack & Architecture

Déngé menerapkan prinsip **Clean Architecture & MVI/MVVM**:

| Komponen | Teknologi |
|---|---|
| **Bahasa** | Kotlin 2.0+ (100% Native Kotlin) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Audio Engine** | AndroidX Media3 (ExoPlayer, MediaSession) |
| **Networking** | Ktor Client (OkHttp Engine, Coroutines) |
| **Local Database** | Room Database (SQLite, KSP) |
| **Dependency Injection** | Koin |
| **Image Loading** | Coil 3 |
| **Penyimpanan Lokal** | EncryptedSharedPreferences |

Dokumentasi arsitektur dan teknis mendalam tersedia di direktori [`docs/`](docs/):
- [`docs/Architecture.md`](docs/Architecture.md) — Arsitektur sistem dan panduan implementasi
- [`docs/PRD.md`](docs/PRD.md) — Rincian spesifikasi produk dan fitur
- [`docs/Design.md`](docs/Design.md) — Panduan desain visual tema Brew & Bean
- [`docs/Schema.md`](docs/Schema.md) — Skema database lokal Room
- [`docs/Rules.md`](docs/Rules.md) — Standar kode dan panduan kontribusi

---

## 💻 Building from Source

### Persyaratan
- **Android Studio** Ladybug (2024.2+) atau lebih baru
- **JDK** 17 atau lebih tinggi
- **Android SDK** API 31 - 36

### Langkah Kompilasi
```bash
# 1. Clone repositori
git clone https://github.com/masrigaa/Denge-MusicApp.git
cd Denge-MusicApp

# 2. Build Debug APK
./gradlew assembleDebug

# 3. File APK akan terbentuk di:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Disclaimer

- **Educational Purpose**: Déngé adalah klien pemutar open-source yang dikembangkan semata-mata untuk tujuan riset dan edukasi.
- **Trademark**: YouTube dan YouTube Music adalah merek dagang terdaftar milik Google LLC. Proyek ini tidak berafiliasi dengan, disponsori, atau didukung oleh Google LLC.
- **Fair Use**: Aplikasi ini tidak menghosting atau mendistribusikan file audio berhak cipta; seluruh stream audio diakses langsung dari endpoint publik oleh perangkat pengguna.

---

<p align="center">
  Made with ☕ by <b>Asla</b>
</p>
