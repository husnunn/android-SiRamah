# Prompt Agent Android — Penyesuaian Kecil untuk Policy Absensi dari Backend

Bantu saya melakukan **penyesuaian kecil di Android** agar aplikasi bisa membaca **policy absensi aktif** yang dikirim backend, tanpa memindahkan logika bisnis ke client.

## Konteks
Backend sekarang akan mengelola policy absensi harian dari admin, misalnya:
- batas awal check-in
- batas akhir tepat waktu
- batas akhir check-in
- batas awal check-out
- batas akhir check-out

Backend tetap menjadi **final decision maker** untuk:
- apakah siswa boleh check-in
- apakah siswa boleh check-out
- apakah statusnya hadir / terlambat / izin / sakit / dispensasi / libur
- apakah ada override harian
- apakah check-out dibebaskan

Android **tidak boleh menghitung rule jam sendiri**.
Android hanya:
- kirim request absensi,
- baca response/status `today`,
- render informasi ke UI.

---

# Tujuan
Saya ingin Android tetap memakai arsitektur sekarang, tetapi bisa menampilkan **informasi policy aktif** yang berasal dari backend.

Contoh info yang ingin ditampilkan di halaman attendance:
- Masuk: 06:00–07:00
- Setelah 07:00 dihitung terlambat
- Pulang: 14:00–18:00

Kalau backend tidak mengirim policy, Android tetap harus aman dan tidak crash.

---

# Struktur project Android saya
Ikuti struktur project saya yang sekarang:
- `api/ApiService.kt`
- `repository/AttendanceRepository.kt`
- `viewmodel/AttendanceViewModel.kt`
- `viewmodel/factory/AttendanceViewModelFactory.kt`
- `pages/attendance/AttendanceFragment.kt`
- model UI/data yang sudah ada seperti `AttendanceTodayUi`, `AttendanceSubmitUi`, `DailyAttendanceTodayData`, dll.

## Aturan arsitektur
- Fragment hanya untuk UI
- ViewModel untuk state
- Repository untuk mapping API
- helper untuk Wi-Fi/lokasi/device
- jangan pindahkan networking ke Fragment
- tetap pakai pola existing:
    - `LiveData + UiState`
    - `ViewModelFactory`
    - `Injection`
- jangan ubah ke Hilt / Flow / Compose kalau project sekarang belum memakai itu

---

# Perubahan kontrak API yang perlu didukung

Backend `GET /api/v1/student/daily-attendance/today` akan mulai mengirim blok policy aktif, misalnya:

```json
{
  "data": {
    "date": "2026-05-02",
    "status": "late",
    "label": "Terlambat",
    "check_in_at": "2026-05-02T07:18:00+07:00",
    "check_out_at": null,
    "late_minutes": 18,
    "can_check_in": false,
    "can_check_out": true,
    "message": "Anda terlambat 18 menit.",
    "site": {
      "id": 1,
      "name": "KANTOR EBD"
    },
    "effective_policy": {
      "check_in_open_at": "06:00",
      "check_in_on_time_until": "07:00",
      "check_in_close_at": "12:00",
      "check_out_open_at": "14:00",
      "check_out_close_at": "18:00"
    }
  }
}
Catatan penting
field policy bisa bernama effective_policy
kalau backend sementara memakai nama policy, siapkan mapping yang mudah disesuaikan
jika field ini null atau tidak ada, Android tetap aman


Kebutuhan perubahan Android
1. Model data

Tolong tambahkan / sesuaikan model response daily attendance agar bisa membaca:

site.id
site.name
effective_policy.check_in_open_at
effective_policy.check_in_on_time_until
effective_policy.check_in_close_at
effective_policy.check_out_open_at
effective_policy.check_out_close_at

Jika field tidak ada, model tetap aman.


2. UI model

Sesuaikan AttendanceTodayUi agar bisa membawa informasi policy ke UI, misalnya:

policyInfoText: String?
atau field terpisah:
checkInOpenAt
checkInOnTimeUntil
checkInCloseAt
checkOutOpenAt
checkOutCloseAt

Pilih cara yang paling konsisten dengan project saya.

Yang saya inginkan

Di halaman attendance minimal bisa muncul teks seperti:

Masuk: 06:00–07:00
Terlambat setelah 07:00
Pulang: 14:00–18:00

Kalau policy tidak ada, bagian ini boleh disembunyikan.



3. Repository

Sesuaikan AttendanceRepository agar:

membaca effective_policy dari response student daily attendance
memetakan ke AttendanceTodayUi
tetap menjaga pemisahan:
student = daily attendance
teacher = legacy attendance
tidak menghitung sendiri rule absensi
hanya menyusun string display policy
Contoh teks policy

Kalau semua field ada:

Masuk: 06:00–07:00
Terlambat sampai 12:00
Pulang: 14:00–18:00

Atau kalau mau dibuat satu string:

Masuk 06:00–07:00 • Terlambat sampai 12:00 • Pulang 14:00–18:00

Pilih format yang paling cocok untuk UI saya.



4. ViewModel

Sesuaikan AttendanceViewModel agar tetap:

memakai LiveData<UiState<...>>
memuat state today
mengembalikan AttendanceTodayUi yang sudah berisi policy info
tidak memindahkan logic API ke Fragment



5. Fragment / Layout

Sesuaikan AttendanceFragment dan layout agar bisa menampilkan info policy.

Kebutuhan UI

Tambahkan area kecil di halaman attendance, misalnya di bawah status harian atau di bawah status lokasi:

teks policy check-in/check-out
tampil hanya jika ada data

Contoh komponen:

tvAttendancePolicyInfo

Kalau perlu tambahkan satu card kecil:

cardAttendancePolicy

Tetapi jangan ubah layout secara ekstrem.


Perilaku yang diinginkan di UI
Jika backend mengirim effective_policy

Tampilkan info jam absensi.

Jika backend tidak mengirim effective_policy

Sembunyikan section policy.

Jika ada status override atau message dari backend

Tetap tampilkan seperti sekarang.
Policy info hanya sebagai informasi tambahan, bukan pengganti status.



Yang tidak boleh dilakukan Android

Android tidak boleh:

menghitung sendiri apakah siswa terlambat
menghitung sendiri apakah check-in sudah ditutup
menghitung sendiri apakah check-out sudah dibuka
mengganti can_check_in / can_check_out hasil backend

Android hanya boleh:

menampilkan policy
menampilkan status
menampilkan tombol sesuai hasil backend



Acceptance Criteria

Implementasi dianggap sesuai jika:

Android tetap bisa load status daily attendance seperti sekarang.
Jika backend mengirim effective_policy, Android bisa membaca tanpa crash.
Jika backend tidak mengirim policy, Android tetap aman.
UI attendance menampilkan info jam absensi secara rapi.
can_check_in dan can_check_out tetap berasal dari backend.
Arsitektur project tetap konsisten dengan pola existing.
Student tetap memakai flow daily attendance.
Teacher tetap legacy attendance jika backend teacher belum dimigrasi.
Output yang saya inginkan dari agent

Tolong hasilkan:

analisis perubahan kecil yang diperlukan,
daftar file yang harus diubah,
perubahan model data,
perubahan AttendanceTodayUi,
perubahan repository,
perubahan ViewModel bila perlu,
perubahan Fragment/layout,
contoh implementasi final dengan path file yang jelas.