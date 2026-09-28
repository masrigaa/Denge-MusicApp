# PRD.md — Product Requirement Document (Déngé)

## Tujuan

Dokumen ini mendefinisikan **apa** itu Déngé, **siapa** target penggunanya, dan **fitur apa saja** yang disediakan. Dokumen ini menjadi sumber kebenaran tingkat produk. Keputusan arsitektur teknis dijelaskan di `Architecture.md`; visual dan panduan desain di `Design.md`.

---

## 1. Ringkasan Produk

### 1.1 Visi
**Déngé** adalah aplikasi pemutar musik Android native yang ringan, elegan, dan bebas iklan (100% ad-free). Déngé menghadirkan pengalaman mendengarkan musik YouTube Music berkualitas tinggi dengan visual bertema hangat *Brew & Bean Coffee*, pemutaran latar belakang (background playback), integrasi status bar modern, serta kontrol privasi penuh tanpa membutuhkan server maupun biaya langganan.

### 1.2 Target Pengguna
| Atribut         | Nilai                                        |
|-----------------|----------------------------------------------|
| Target Utama    | Pengguna personal / penikmat musik harian    |
| Platform        | Android 12 hingga Android 16 (API 31 - 36)   |
| Konteks         | Komuter, fokus kerja/belajar, santai         |
| Motivasi Kunci  | Menikmati katalog musik luas tanpa jeda iklan, konsumsi RAM/baterai efisien |

### 1.3 Tujuan & Metrik Keberhasilan
- **M1 — Pemutaran Bebas Iklan**: 100% audio diputar tanpa gangguan iklan audio maupun jeda video.
- **M2 — Efisiensi Memori & Baterai**: Konsumsi memori jauh lebih ringan daripada aplikasi browser atau webview wrapper.
- **M3 — Pemutaran Latar Belakang & Dynamic Island**: Lagu tetap berjalan lancar saat layar mati atau aplikasi diminimalkan, terintegrasi dengan status bar dan HyperOS Hyper Island.
- **M4 — Pengalaman Pengguna Premium**: Visual hangat dengan tema Brew & Bean, transisi halus, dan kemudahan navigasi.

---

## 2. Fitur Utama (v1.3.2)

### 2.1 Beranda (Home Screen)
- **Sapaan Personal**: Badge nama pengguna yang dapat dikustomisasi (misal: "Halo, Asla ☕").
- **Lagu yang Sering Kamu Putar**: Menampilkan 2 lagu teratas yang paling sering didengarkan lengkap dengan penghitung jumlah pemutaran (*"Diputar X kali"*). Perhitungan ini otomatis di-reset setiap tanggal 1 setiap bulannya.
- **Baru Saja Diputar**: Daftar horizontal riwayat lagu yang terakhir dimainkan untuk akses instan.
- **Rekomendasi Genre Pilihan**: 5 seksi genre (J-Pop, Hololive / VTuber, Lofi, Western Pop, Anime OST) yang dapat disesuaikan lewat Pengaturan.

### 2.2 Now Playing (Pemutar Penuh)
- **Album Art HD & Preview**: Menampilkan gambar cover resolusi tinggi. Mengetuk cover akan membuka dialog preview HD.
- **Tombol Share Instan**: Tombol khusus di dialog preview yang langsung menyalin (copy) link YouTube / YouTube Music resmi ke clipboard untuk dibagikan dengan sekali sentuh.
- **Kontrol Pemutaran Lengkap**: Play, Pause, Next, Previous, Shuffle, Repeat (Off, All, One), dan Slider progres interaktif.
- **Antrean & Radio "Up Next"**: Menampilkan lagu berikutnya serta rekomendasi radio otomatis dari lagu yang sedang berjalan.

### 2.3 Pustaka (Library Screen)
- **Lagu yang Disukai (Liked Songs)**: Penyimpanan lokal satu ketukan untuk menandai lagu favorit dengan ikon hati (♥).
- **Playlist Kustom**: Kemampuan membuat dan mengelola playlist lokal tanpa batas.
- **Riwayat Pemutaran**: Catatan kronologis pemutaran lengkap.

### 2.4 Pencarian (Search Screen)
- **Debounced Live Search**: Pencarian cepat ke katalog YouTube Music dengan jeda ketik otomatis.
- **Menu Opsi Lagu (⋮)**: Opsi instan untuk "Putar Sekarang", "Putar Berikutnya", "Tambah ke Antrean", dan "Lihat Cover HD".

### 2.5 Pengaturan (Settings Screen)
- **Kartu Pengaturan Interaktif**:
  - **Preferensi Genre**: Memilih genre musik favorit yang ditampilkan di Beranda.
  - **Equalizer**: Penyesuaian audio effect dengan preset instan (Bass Boost, Vocal, Rock, Flat, dll.).
- **Kartu Informasi & Personalisasi**:
  - **Ubah Nama Panggilan**: Dialog kustomisasi nama lokal tanpa perlu login Google.
  - **Tema & Warna**: Tema Brew & Bean Coffee (Dark Mocha & Latte).
  - **Kualitas Audio**: High Quality Opus Audio Stream.
  - **Versi Aplikasi**: Informasi rilis v1.3.2.

---

## 3. Alur Pengguna (User Flows)

### 3.1 Alur Mulai Pertama Kali (First Launch)
1. Pengguna membuka Déngé untuk pertama kalinya.
2. Aplikasi langsung siap memutar musik (tanpa splash screen panjang atau paksaan login akun).
3. Nama awal diatur secara aman ("Sobat Musik"), pengguna dapat mengubahnya sewaktu-waktu di menu Pengaturan.

### 3.2 Alur Pemutaran & Pengaturan Antrean
1. Pengguna mengetuk lagu dari Beranda, Pustaka, atau Pencarian.
2. Jika memilih titik tiga (⋮):
   - **Putar Berikutnya**: Menyisipkan lagu tepat setelah lagu yang sedang diputar.
   - **Tambah ke Antrean**: Menambahkan lagu ke ujung antrean aktif.
3. Notifikasi Foreground Service aktif dengan kontrol penuh di lockscreen dan Dynamic Island.

### 3.3 Alur Berbagi Musik (Share)
1. Di layar Now Playing, pengguna mengetuk cover album untuk membuka dialog preview.
2. Pengguna mengetuk tombol **Share**.
3. Link YouTube Music resmi otomatis tersalin ke clipboard dan toast konfirmasi muncul. Pengguna tinggal menempelkan (*paste*) link tersebut di WhatsApp, Discord, atau media sosial lainnya.

---

## 4. Keamanan & Kepatuhan
- **Zero Server Footprint**: Tidak menyimpan data di cloud server Déngé.
- **Privacy First**: Tidak mengambil kontak, lokasi, atau file pribadi pengguna.
- **Educational / Fair-Use**: Dibuat sebagai pemutar media alternatif berbasis open API client.

---

*Terakhir diperbarui: 27 September 2026 — Déngé v1.3.2*
