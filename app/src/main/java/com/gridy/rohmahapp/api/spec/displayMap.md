# Prompt Agent Android — Tambahkan Map Radius Attendance

Bantu saya menambahkan fitur **map radius absensi** pada halaman attendance Android saya.

## Tujuan
Saya ingin di halaman attendance Android terdapat tampilan peta yang memperlihatkan:

1. **titik pusat sekolah / attendance site**
2. **titik lokasi pengguna saat ini**
3. **lingkaran radius absensi sekolah**
4. **status apakah pengguna masih di dalam radius atau di luar radius**
5. **jarak pengguna ke titik pusat sekolah**

Fitur ini dipakai sebagai **visualisasi** agar user lebih mudah memahami validasi lokasi absensi.

---

# Konteks project
Project Android saya saat ini memakai pola arsitektur:
- Fragment untuk UI
- ViewModel untuk state
- Repository untuk akses data
- helper / util untuk lokasi / Wi-Fi / device info
- LiveData + UiState
- ViewBinding / XML layout
- jangan pindahkan networking ke Fragment
- jangan ubah ke Compose / Hilt jika project saat ini belum memakai itu

Ikuti struktur project saya yang sekarang.

---

# Kebutuhan utama fitur map radius

## Yang harus tampil di peta
1. **Marker sekolah**
    - titik pusat lokasi absensi sekolah
    - diambil dari data backend / attendance site

2. **Marker pengguna**
    - lokasi user saat ini
    - diambil dari GPS / lokasi perangkat

3. **Circle / lingkaran radius**
    - titik pusat = lokasi sekolah
    - radius = `radius_m` dari attendance site
    - warna merah transparan agar jelas terlihat

4. **Kamera map**
    - idealnya menampilkan area sekolah dan posisi pengguna
    - jika user belum ada lokasi, minimal fokus ke titik sekolah + radius

---

# Data yang dibutuhkan Android
Android harus menggunakan data:
- `site.latitude`
- `site.longitude`
- `site.radius_m`
- `site.name`
- `current user latitude`
- `current user longitude`

Jika backend punya struktur berbeda, sesuaikan mapping tetapi tetap hasil akhirnya harus sama:
- ada titik sekolah
- ada radius
- ada titik user

---

# Perilaku UI yang diinginkan

## Saat data sekolah dan lokasi user tersedia
- tampilkan marker sekolah
- tampilkan marker user
- tampilkan lingkaran radius
- tampilkan status:
    - "Dalam jangkauan sekolah"
    - atau "Di luar jangkauan sekolah"
- tampilkan jarak:
    - misalnya `Jarak Anda ke titik sekolah: 63 m`

## Jika lokasi user belum tersedia
- map tetap tampil dengan marker sekolah + radius
- marker user tidak perlu tampil
- tampilkan pesan:
    - "Lokasi Anda belum tersedia"

## Jika permission lokasi belum diberikan
- tampilkan state yang jelas
- tombol check-in tetap mengikuti rule yang sudah ada
- tampilkan pesan agar user mengaktifkan izin lokasi

---

# Perhitungan jarak
Gunakan perhitungan jarak standar Android, misalnya `Location.distanceBetween(...)`.

Yang dibutuhkan:
- hitung jarak user ke titik pusat sekolah dalam meter
- tentukan boolean:
    - `isInsideRadius = distance <= radius_m`

Namun keputusan final absensi tetap backend.  
Android hanya memakai ini untuk **visualisasi dan precheck UI**.

---

# Library / komponen map
Gunakan solusi map yang paling sesuai dengan project Android saat ini.

Jika belum ada map di project, gunakan **Google Maps SDK for Android**.

Map harus mendukung:
- marker sekolah
- marker user
- circle radius
- camera move / zoom

---

# Arsitektur implementasi yang diinginkan

## Fragment
Fragment hanya bertugas:
- setup view
- observe ViewModel
- render data map
- handle permission callback
- handle klik tombol

Fragment tidak boleh langsung membuat networking logic.

## ViewModel
ViewModel bertugas:
- menggabungkan state UI attendance
- menyiapkan data map untuk UI
- expose state seperti:
    - schoolLatLng
    - userLatLng
    - radiusMeter
    - distanceText
    - locationStatusText
    - isInsideRadius

## Repository
Repository tetap fokus ke:
- ambil data attendance site / today attendance dari backend
- mapping response ke model UI

## Helper / util
Jika perlu, buat helper lokasi untuk:
- ambil current location
- cek apakah GPS aktif
- cek permission lokasi

---

# Model UI yang diinginkan
Tambahkan / sesuaikan model UI agar halaman attendance bisa merender map radius.

Contoh field yang dibutuhkan:
- `siteName: String?`
- `siteLatitude: Double?`
- `siteLongitude: Double?`
- `radiusMeter: Int?`
- `userLatitude: Double?`
- `userLongitude: Double?`
- `distanceMeter: Float?`
- `distanceText: String`
- `locationStatusText: String`
- `isInsideRadius: Boolean`

Silakan sesuaikan penamaan dengan style project saya.

---

# Layout XML yang diinginkan
Di halaman attendance, tambahkan area map.

Kebutuhan minimal:
- satu container map
- teks status lokasi
- teks jarak user ke sekolah

Misalnya komponen:
- `FragmentContainerView` / `SupportMapFragment`
- `TextView tvLocationStatus`
- `TextView tvDistanceInfo`

Jika sudah ada `tvLocationStatus`, pertahankan dan sesuaikan isinya.

Jangan ubah layout secara ekstrem, cukup tambahkan section map yang rapi.

---

# Visual map yang diinginkan
- marker sekolah: titik pusat absensi
- marker user: posisi user
- circle radius: merah transparan
- map harus cukup zoom agar radius terlihat
- jika memungkinkan, tampilkan kedua marker dalam satu viewport

---

# Acceptance Criteria
Implementasi dianggap sesuai jika:

1. Halaman attendance Android menampilkan map.
2. Map menampilkan marker titik pusat sekolah.
3. Map menampilkan marker titik pengguna saat ini.
4. Map menampilkan lingkaran radius absensi sekolah.
5. Android menghitung dan menampilkan jarak user ke titik sekolah.
6. Android menampilkan status apakah user di dalam atau di luar radius.
7. Jika lokasi user belum tersedia, map tetap bisa menampilkan titik sekolah + radius.
8. Fragment tetap hanya menangani UI.
9. Logic data tetap melalui ViewModel + Repository.
10. Fitur ini hanya untuk visualisasi / precheck; keputusan akhir absensi tetap backend.

---

# Output yang saya inginkan dari agent
Tolong hasilkan:
1. analisis singkat perubahan yang diperlukan,
2. daftar file yang perlu dibuat / diubah,
3. perubahan model UI,
4. perubahan Fragment,
5. perubahan ViewModel,
6. perubahan layout XML,
7. integrasi Google Maps / map component,
8. contoh implementasi final yang sesuai struktur project saya.

Jika membuat kode, selalu tulis **path file** dengan jelas pada setiap snippet.