# Prompt Agent Android — Dukungan Override Absensi Harian

Bantu saya menyesuaikan aplikasi Android saya agar mendukung **override absensi harian** dari backend, tanpa memindahkan logika bisnis utama ke Android.

## Konteks
Aplikasi Android saya hanya sebagai client. **Backend tetap menjadi penentu final** untuk:
- apakah siswa boleh check-in,
- apakah siswa boleh check-out,
- apakah hari itu ada override,
- apakah siswa dipulangkan lebih awal,
- apakah check-out dibebaskan,
- apakah hari itu libur / izin / sakit / dispensasi,
- apakah status hari itu hadir / terlambat / alpa / dll.

Android **tidak boleh** menghitung sendiri rule override. Android hanya:
- mengirim data absensi fisik,
- membaca response/status dari backend,
- menyesuaikan UI berdasarkan hasil final backend.

---

# Tujuan Implementasi Android

Saya ingin Android:
1. membaca status harian final dari endpoint `daily-attendance/today`,
2. menampilkan informasi override harian jika ada,
3. mengaktifkan / menonaktifkan tombol check-in dan check-out berdasarkan data backend,
4. menampilkan pesan seperti:
    - "Hari ini siswa dipulangkan lebih awal."
    - "Check-out dibuka pukul 10:00."
    - "Hari ini check-out manual tidak diwajibkan."
5. tetap memakai validasi lokal minimum seperti:
    - Wi-Fi aktif,
    - lokasi aktif,
    - permission lokasi,
      tetapi keputusan final tetap backend.
6. menjaga arsitektur project Android saya tetap konsisten:
    - Fragment hanya untuk UI
    - ViewModel untuk state
    - Repository untuk mapping API
    - helper untuk Wi-Fi/lokasi/device
7. mengikuti pola project saya yang sekarang:
    - `LiveData + UiState`
    - `ViewModelFactory`
    - `Repository`
    - `Injection`
    - jangan pindahkan networking ke Fragment

---

# Struktur Project Android Saya
Sesuaikan implementasi dengan struktur project saya yang sudah ada, terutama:
- `api/ApiService.kt`
- `repository/AttendanceRepository.kt`
- `viewmodel/AttendanceViewModel.kt`
- `viewmodel/factory/AttendanceViewModelFactory.kt`
- `pages/attendance/AttendanceFragment.kt`
- model UI/data yang sudah ada seperti `AttendanceTodayUi`, `AttendanceSubmitUi`, dll.

Jangan ubah arsitektur menjadi Hilt/Flow/Compose jika project saat ini belum memakai itu.
Ikuti pola existing project.

---

# Perubahan Kontrak API yang Harus Didukung Android

Android harus siap membaca response `GET /api/v1/student/daily-attendance/today` yang bisa mengandung field seperti:

```json
{
  "data": {
    "date": "2026-05-02",
    "status": "present",
    "label": "Hadir",
    "check_in_at": "2026-05-02T06:45:00+07:00",
    "check_out_at": null,
    "late_minutes": 0,
    "can_check_in": false,
    "can_check_out": true,
    "override": {
      "active": true,
      "event_type": "teacher_meeting",
      "dismiss_students_early": true,
      "waive_check_out": false
    },
    "message": "Hari ini siswa dipulangkan lebih awal. Check-out dibuka pukul 10:00."
  }
}

Contoh lain jika check-out dibebaskan:
{
  "data": {
    "date": "2026-05-02",
    "status": "present",
    "label": "Hadir",
    "check_in_at": "2026-05-02T06:45:00+07:00",
    "check_out_at": null,
    "late_minutes": 0,
    "can_check_in": false,
    "can_check_out": false,
    "override": {
      "active": true,
      "event_type": "teacher_meeting",
      "dismiss_students_early": true,
      "waive_check_out": true
    },
    "message": "Hari ini siswa dipulangkan lebih awal dan tidak wajib check-out manual."
  }
}

Kebutuhan Model Android
Tolong tambahkan / sesuaikan model agar bisa membaca field:
status
label
check_in_at
check_out_at
late_minutes
can_check_in
can_check_out
message
override.active
override.event_type
override.dismiss_students_early
override.waive_check_out
Jika field override tidak ada atau null, Android harus tetap aman.

Kebutuhan Mapping UI Android

Android perlu memetakan status harian ke tampilan UI.

Status yang harus didukung
present → Hadir
late → Terlambat
excused → Izin
sick → Sakit
dispensation → Dispensasi
absent → Alpa
holiday → Libur
Event override yang harus dikenali
teacher_meeting
early_dismissal
special_event
attendance_closed
holiday_override
fallback event lain
Tampilan yang diinginkan di halaman attendance
badge status harian
pesan info harian
riwayat check-in
riwayat check-out
tombol masuk
tombol pulang

Perilaku UI yang Diinginkan
1. Jika override.active = true

Android harus menampilkan kartu/info override.

Contoh:

"Hari ini siswa dipulangkan lebih awal."
"Ada override absensi untuk hari ini."
2. Jika dismiss_students_early = true

Tampilkan pesan khusus bahwa siswa dipulangkan lebih awal.

3. Jika waive_check_out = true
tombol check-out harus nonaktif
tampilkan info: "Check-out manual tidak diwajibkan hari ini."
4. Jika can_check_in = false

Tombol masuk nonaktif.

5. Jika can_check_out = false

Tombol pulang nonaktif.

6. Jika backend memberi message

Message itu harus ditampilkan di UI, bukan diabaikan.

Aturan Android yang Harus Dipertahankan
Android hanya precheck lokal

Android tetap boleh memeriksa:

permission lokasi
GPS/lokasi aktif
Wi-Fi tersambung

Tetapi Android tidak boleh menentukan sendiri:

kapan check-in dibuka
kapan check-out dibuka
apakah hari ini siswa pulang cepat
apakah check-out dibebaskan

Semua itu harus berasal dari backend.


Kebutuhan pada Repository

Tolong sesuaikan AttendanceRepository agar:

memetakan response today ke AttendanceTodayUi,
menambahkan field untuk override harian, misalnya:
overrideActive
overrideEventType
dismissStudentsEarly
waiveCheckOut
infoMessage
membentuk teks riwayat:
Hadir Masuk: 06:45 WIB
Hadir Pulang: 10:15 WIB
jika late, tambahkan teks:
(Terlambat 18 menit)
tetap memisahkan flow:
student = daily attendance
teacher = legacy attendance
tetap memakai mapping reason code untuk submit response

Kebutuhan pada ViewModel

Tolong sesuaikan AttendanceViewModel agar:

tetap memakai LiveData<UiState<...>>
tetap sesuai pola project saya
memanggil repository untuk memuat state harian
memuat ulang status harian setelah submit check-in/check-out
tidak memindahkan logic API ke Fragment


Kebutuhan pada Fragment

Tolong sesuaikan AttendanceFragment agar:

tetap hanya meng-handle binding UI
tetap pakai ViewModelProvider + AttendanceViewModelFactory
menampilkan:
badge status
pesan info
status lokasi
history check-in/check-out
menonaktifkan tombol sesuai canCheckIn / canCheckOut
menampilkan informasi override jika ada

Jika perlu, tambahkan komponen UI baru di layout seperti:

cardDailyStatus
tvDailyStatusBadge
tvDailyStatusMessage

Tetapi jangan ubah struktur halaman secara ekstrem.


Kebutuhan Formatting Waktu

Pastikan Android format waktu tetap konsisten.

Gunakan format seperti:

06:45 WIB
10:15 WIB

Jika backend sudah mengirim waktu dengan offset benar, Android cukup parse normal.
Jika ada workaround sementara untuk waktu yang salah format, beri komentar yang jelas di kode bahwa itu workaround sementara.


Acceptance Criteria Android

Implementasi dianggap sesuai jika:

Android bisa membaca status harian final dari backend.
Android bisa membaca field override jika tersedia.
Android menampilkan badge status harian yang benar.
Android menampilkan pesan override harian yang benar.
Tombol check-in/check-out mengikuti can_check_in dan can_check_out.
Jika waive_check_out = true, tombol check-out nonaktif dan ada pesan yang menjelaskan.
Jika dismiss_students_early = true, Android menampilkan info bahwa siswa dipulangkan lebih awal.
Android tetap memakai arsitektur project yang sekarang.
Fragment tidak membuat ApiService langsung.
Student tetap memakai endpoint daily attendance, teacher tetap legacy jika backend teacher belum dimigrasi.


Output yang Saya Inginkan dari Agent

Tolong hasilkan:

analisis perubahan Android yang dibutuhkan,
daftar file yang harus diubah,
model data yang perlu ditambah/diubah,
perubahan repository,
perubahan ViewModel,
perubahan Fragment/layout,
mapping status dan override ke UI,
contoh implementasi final yang mengikuti struktur project saya.

Jika membuat kode, tuliskan path file dengan jelas di setiap snippet.