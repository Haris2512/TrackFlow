<div align="center">
  <img src="https://img.shields.io/badge/Tugas%20Final-Pemrograman%20Mobile%202026-FC4C02?style=for-the-badge&logo=android" alt="Tugas Final Mobile" />
  <h1>🏃 TrackFlow</h1>
  <p><b>Your Personal GPS Run Tracker & Route Navigation App</b></p>
  <p>TrackFlow adalah aplikasi Android inovatif berbasis navigasi peta yang dirancang khusus bagi para pelari dan penggiat olahraga untuk melacak rute, menghitung jarak, mencatat riwayat lari secara <i>offline/online</i>, serta menganalisis performa harian.</p>

  <img src="https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Android%20Studio-3DDC84?style=flat-square&logo=android-studio&logoColor=white" />
  <img src="https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/Retrofit-FF6C37?style=flat-square&logo=postman&logoColor=white" />
</div>

<br>

## ✨ Fitur Utama (Sesuai Spesifikasi Teknis)

Aplikasi TrackFlow dibangun dengan memenuhi seluruh **kriteria Tugas Final Lab Mobile 2026** tanpa terkecuali:

### 🌍 1. Navigasi & Live Tracking (OSMDroid & Location Services)
* **Perekaman Lari Real-Time:** Menggunakan `LocationManager` dan **Foreground Service** untuk melacak pergerakan GPS pengguna secara langsung di atas peta interaktif.
* **Smart Route Planner:** Pengguna dapat mengetuk titik mana saja di peta untuk secara otomatis membuat rute lari, menghitung estimasi jarak tempuh, dan waktu lari menggunakan integrasi **API OSRM**.

### 🗄️ 2. Penyimpanan Lokal (SQLite CRUD)
* **Manajemen Riwayat Lari:** Semua aktivitas lari (jarak, waktu, kecepatan/pace, tanggal) langsung tersimpan ke dalam *Database SQLite*.
* **Upload Bukti Foto:** Pengguna dapat menambahkan foto momen lari yang akan disimpan langsung di memori HP (menggunakan sistem URI persisten).
* Fitur mencakup: Buat (Create), Baca (Read), Perbarui (Update), dan Hapus (Delete via tekan lama pada *card*).

### 📡 3. Integrasi API RESTful (Retrofit)
* **Kalkulasi Rute Cerdas:** Memanggil *Open Source Routing Machine* (OSRM) secara asinkronus menggunakan *Retrofit* untuk menggambar garis rute perjalanan.
* **Daftar Atlet (Komunitas):** Mengambil data JSON atlet dunia untuk ditampilkan dalam bentuk daftar bergaya menggunakan `RecyclerView`.

### 🛡️ 4. Ketahanan & Penanganan Error (Error Handling & Offline State)
* **Sistem Peringatan Manusiawi:** Jika GPS mati atau Koneksi Internet terputus, aplikasi tidak akan macet (crash). TrackFlow dilengkapi jendela *AlertDialog* interaktif yang ramah pengguna.
* **Dynamic Refresh Button:** Tersedia mekanisme layar *Empty State* dan tombol **Coba Lagi** secara dinamis apabila sistem gagal mengambil data dari server.

### 🎨 5. UI/UX Modern (Adaptive Light & Dark Mode)
* Dibangun dengan `Material Design 3` yang mendukung peralihan otomatis/manual antara **Light Mode** dan **Dark Mode** secara halus.
* Kalender riwayat *Streak* harian yang otomatis beradaptasi dengan sistem tema dan tanggal hari ini.
* Tampilan memukau ala *Strava* dengan kombinasi warna Oranye Neon (`#FC4C02`) dan palet warna netral elegan.

---

## 🛠️ Arsitektur & Teknologi

| Teknologi / Komponen | Detail Penggunaan di TrackFlow |
| --- | --- |
| **Bahasa** | Java 11 |
| **Minimum SDK** | API 24 (Nougat) |
| **Target SDK** | API 36 |
| **Maps Engine** | [OSMDroid](https://github.com/osmdroid/osmdroid) v6.1.18 |
| **Networking** | Retrofit2 + Gson Converter |
| **Local Database** | SQLiteOpenHelper |
| **Komponen UI** | BottomNavigationView, ViewPager2, CardView, FloatingActionButton |

---

## 🚀 Cara Menjalankan Aplikasi (Untuk Asisten Penilai)

1. **Clone Repository ini** ke mesin lokal Anda:
   ```bash
   git clone https://github.com/username/TrackFlow.git
   ```
2. Buka proyek menggunakan **Android Studio**.
3. Pastikan **Emulator** atau **Perangkat Asli** Anda memiliki akses Internet yang aktif untuk sinkronisasi awal peta.
4. **Izin Aplikasi:** Saat aplikasi pertama kali dibuka, berikan semua izin yang diminta (Lokasi Presisi & Akses Notifikasi).
5. **Simulasi Perekaman:** 
   - Masuk ke tab **Record**.
   - Jika menggunakan emulator, ubah titik lokasi melalui pengaturan *Location* emulator (`...` -> `Location`) untuk melihat rute lari bergerak.
6. **Uji Offline Mode:** 
   - Coba matikan WiFi/Data Anda.
   - Buka tab **Map** lalu ketuk peta, atau buka tab **Komunitas**, maka fitur **Error Handling (Tombol Refresh/AlertDialog)** akan otomatis bekerja menjaga aplikasi dari *crash*.

---

## 📝 Catatan Khusus Implementasi

* **Foreground Service:** Ketika mode rekaman aktif, TrackFlow akan memunculkan notifikasi berjalan di *status bar* sehingga OS Android tidak mematikan pelacakan GPS di latar belakang.
* **Keamanan Data:** Data `SharedPreferences` digunakan untuk menyimpan preferensi tema (Gelap/Terang) serta nama profil pengguna agar tidak hilang saat aplikasi ditutup.

---
<div align="center">
  <p><b>Dikembangkan untuk memenuhi Tugas Final Laboratorium Pemrograman Mobile 2026</b><br>
  <i>Seluruh hak cipta dan kode sumber dilindungi sesuai ketentuan akademik.</i></p>
</div>