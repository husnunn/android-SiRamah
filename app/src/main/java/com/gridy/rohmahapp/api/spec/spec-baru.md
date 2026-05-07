# Spesifikasi Fitur Absensi Harian Sekolah

## Tujuan
Saya ingin mengubah konsep absensi menjadi **absensi harian masuk/pulang sekolah**, bukan absensi utama berbasis per mapel.

Fitur ini harus mendukung:
1. Absensi **masuk sekolah** harian.
2. Absensi **pulang sekolah** harian.
3. Penentuan status:
    - Hadir
    - Terlambat
    - Izin
    - Sakit
    - Dispensasi
    - Alpa
4. Validasi tetap memakai:
    - Wi-Fi sekolah
    - lokasi/radius sekolah
    - kalender akademik
5. Guru/admin dapat menginput **izin manual** untuk siswa.
6. Android tetap dipakai untuk absensi fisik masuk/pulang, sedangkan izin manual diinput dari admin/web terlebih dahulu.

---

# 1. Perubahan Konsep Bisnis

## Model lama
Model lama berbasis:
- jadwal aktif
- window dari `start_time`
- check-in valid jika cocok dengan schedule tertentu

## Model baru yang diinginkan
Model baru berbasis:
- **absensi harian sekolah**
- tidak tergantung slot mapel sebagai syarat utama check-in
- yang menjadi acuan utama adalah **jam operasional absensi sekolah**

## Artinya
Absensi siswa **bukan per mapel**, tetapi:
- datang ke sekolah
- pulang dari sekolah

Mapel/jadwal tetap boleh ada untuk informasi dan fitur lain, tetapi **bukan penentu utama absensi masuk harian**.

---

# 2. Rule Jam Absensi Harian

## Check-in (Masuk)
Aturan yang diinginkan:

- Jam buka check-in: **06:00**
- Batas hadir tepat waktu: **07:00**
- Setelah jam **07:00** siswa tetap **boleh check-in**
- Tetapi statusnya menjadi **terlambat**

## Rekomendasi tambahan agar rule lengkap
Agar sistem tidak ambigu, backend perlu juga punya:
- `check_in_close_at`

Contoh default:
- `check_in_open_at = 06:00`
- `check_in_on_time_until = 07:00`
- `check_in_close_at = 09:00`

## Hasil status check-in
- 06:00–07:00 → `present`
- >07:00–09:00 → `late`
- >09:00 → ditolak atau masuk rule khusus (sesuai kebijakan sekolah)

## Check-out (Pulang)
Perlu dibuat rule juga. Contoh:
- `check_out_open_at = 12:00`
- `check_out_close_at = 18:00`

Jika belum ada kebijakan final, agent harus membuat konfigurasi supaya jam pulang bisa diatur per sekolah.

---

# 3. Validasi Wajib Saat Absensi Fisik

Absensi fisik dari Android tetap harus divalidasi dengan syarat:

1. HP terhubung ke Wi-Fi sekolah.
2. Posisi user berada di radius sekolah.
3. Hari tersebut bukan hari libur / event akademik yang menutup absensi.
4. Status siswa pada hari itu **belum** ditandai manual sebagai:
    - izin
    - sakit
    - dispensasi
5. Check-in/check-out harus berada dalam window waktu yang valid.

## Catatan penting
Pada model baru:
- **schedule/mapel tidak lagi dipakai untuk menentukan check-in masuk harian**
- sehingga reason `NO_ACTIVE_SCHEDULE` untuk absensi masuk harian **tidak lagi menjadi rule utama**
- jika tetap ada jadwal, jadwal itu hanya pelengkap untuk laporan, bukan penentu absensi masuk harian

---

# 4. Timezone

Backend harus memakai satu timezone sekolah, misalnya:
- `SCHOOL_TIMEZONE=Asia/Jakarta`

Timezone ini dipakai untuk:
- interpretasi `client_time`
- penentuan hari sekolah
- penentuan jam check-in/check-out
- kalender akademik
- filter “hari ini”

Semua waktu yang dikembalikan ke frontend/admin/API sebaiknya dikonversi ke timezone sekolah saat diserialisasi.

---

# 5. Status Kehadiran Harian

## Status final yang diperlukan
- `present` = hadir
- `late` = terlambat
- `excused` = izin
- `sick` = sakit
- `dispensation` = dispensasi
- `absent` = alpa
- `holiday` = libur (hasil kalender akademik, bukan status siswa)

## Prioritas penentuan status akhir harian
Untuk 1 siswa pada 1 tanggal, prioritasnya:

1. Jika kalender akademik menutup hari itu → `holiday`
2. Jika ada status manual admin/guru yang aktif:
    - `excused`
    - `sick`
    - `dispensation`
3. Jika ada check-in fisik:
    - `present` atau `late`
4. Jika tidak ada semuanya → `absent`

---

# 6. Fitur Izin Manual (Opsi A)

## Konsep
Izin tidak diajukan dari Android dulu.
Izin diinput langsung oleh guru/admin melalui web/admin panel.

## Jenis manual status
- `excused` = izin
- `sick` = sakit
- `dispensation` = dispensasi

## Perilaku
Jika guru/admin sudah menginput status manual untuk tanggal tertentu:
- siswa tidak perlu check-in fisik
- status hari itu mengikuti input manual
- tombol absensi di Android nantinya bisa dinonaktifkan atau hanya menampilkan status informasi

---

# 7. Desain Database yang Disarankan

## A. Tabel utama absensi harian
Nama disarankan:
- `daily_attendances`

### Kolom
- `id`
- `user_id`
- `student_profile_id`
- `attendance_site_id`
- `date`
- `check_in_at` nullable
- `check_out_at` nullable
- `status`
- `late_minutes` nullable
- `check_in_reason_code` nullable
- `check_in_reason_detail` nullable
- `network_payload` nullable json
- `location_payload` nullable json
- `device_payload` nullable json
- `created_at`
- `updated_at`

### Catatan
- satu siswa maksimal satu `daily_attendance` per tanggal
- `status` final untuk absensi fisik bisa:
    - `present`
    - `late`

## B. Tabel manual status
Nama disarankan:
- `attendance_manual_statuses`

### Kolom
- `id`
- `user_id`
- `student_profile_id`
- `attendance_site_id` nullable
- `date`
- `type` (`excused`, `sick`, `dispensation`)
- `reason`
- `notes` nullable
- `status` (`approved`, `cancelled`)
- `created_by`
- `updated_by` nullable
- `created_at`
- `updated_at`

---

# 8. Endpoint API yang Diperlukan

## Endpoint Android - Student
### POST `/api/v1/student/daily-attendance/check-in`
Request body:
```json
{
  "attendance_site_id": 1,
  "client_time": "2026-04-29T06:45:00+07:00",
  "network": {
    "ssid": "SCHOOL-WIFI",
    "bssid": "AA:BB:CC:11:22:33",
    "local_ip": "192.168.1.10",
    "gateway_ip": "192.168.1.1",
    "subnet_prefix": 24,
    "transport": "WIFI"
  },
  "location": {
    "latitude": -7.297316,
    "longitude": 112.7583014,
    "accuracy_m": 20,
    "provider": "fused",
    "is_mock": false,
    "captured_at": "2026-04-29T06:44:58+07:00"
  },
  "device": {
    "platform": "android",
    "app_version": "1.0.0",
    "os_version": "14"
  }
}

Response approved on-time
{
  "message": "Absensi masuk berhasil.",
  "status": "approved",
  "attendance_status": "present",
  "late_minutes": 0,
  "reason_code": null,
  "reason_detail": null,
  "record": {
    "id": 1001,
    "date": "2026-04-29",
    "check_in_at": "2026-04-29T06:45:00+07:00",
    "check_out_at": null,
    "status": "present",
    "late_minutes": 0,
    "site": {
      "id": 1,
      "name": "Kantor EBD"
    }
  }
}

Response approved late
{
  "message": "Absensi masuk berhasil, tercatat terlambat.",
  "status": "approved",
  "attendance_status": "late",
  "late_minutes": 18,
  "reason_code": "LATE_CHECK_IN",
  "reason_detail": "Check-in setelah batas hadir tepat waktu.",
  "record": {
    "id": 1002,
    "date": "2026-04-29",
    "check_in_at": "2026-04-29T07:18:00+07:00",
    "check_out_at": null,
    "status": "late",
    "late_minutes": 18,
    "site": {
      "id": 1,
      "name": "Kantor EBD"
    }
  }
}

Response rejected

Kemungkinan reason code:
WIFI_NOT_CONNECTED
WIFI_NOT_MATCHED
OUT_OF_RADIUS
ACADEMIC_EVENT_BLOCK
MANUAL_STATUS_EXISTS
CHECK_IN_CLOSED
MOCK_LOCATION_DETECTED
SITE_NOT_ACTIVE

POST /api/v1/student/daily-attendance/check-out
Body hampir sama, tetapi tanpa attendance_type.
Backend tahu ini endpoint check-out.
Response
{
  "message": "Absensi pulang berhasil.",
  "status": "approved",
  "record": {
    "id": 1001,
    "date": "2026-04-29",
    "check_in_at": "2026-04-29T06:45:00+07:00",
    "check_out_at": "2026-04-29T15:02:00+07:00",
    "status": "present",
    "late_minutes": 0
  }
}

GET /api/v1/student/daily-attendance/today
Response:
{
  "data": {
    "date": "2026-04-29",
    "status": "present",
    "label": "Hadir",
    "check_in_at": "2026-04-29T06:45:00+07:00",
    "check_out_at": null,
    "late_minutes": 0,
    "can_check_in": false,
    "can_check_out": true,
    "site": {
      "id": 1,
      "name": "Kantor EBD"
    }
  }
}

Jika siswa sudah diberi izin:
{
  "data": {
    "date": "2026-04-29",
    "status": "excused",
    "label": "Izin",
    "check_in_at": null,
    "check_out_at": null,
    "late_minutes": null,
    "can_check_in": false,
    "can_check_out": false,
    "message": "Anda telah diberi status izin oleh guru/admin."
  }
}

9. Endpoint Admin / Web
Input manual status siswa
POST /admin/students/{student}/attendance-manual-statuses
Payload:
{
  "date": "2026-05-01",
  "type": "excused",
  "reason": "Izin menghadiri acara keluarga",
  "notes": "Disampaikan oleh wali murid",
  "attendance_site_id": 1
}

Update manual status
PUT /admin/students/{student}/attendance-manual-statuses/{manualStatus}
Cancel manual status
PATCH /admin/students/{student}/attendance-manual-statuses/{manualStatus}/cancel

10. Rule Harian yang Harus Diterapkan Backend
Check-in
Jika client_time < 06:00 → ditolak dengan CHECK_IN_NOT_OPEN_YET
Jika 06:00–07:00 → present
Jika >07:00–09:00 → late
Jika >09:00 → ditolak dengan CHECK_IN_CLOSED

Check-out
Jika sebelum jam buka pulang → ditolak dengan CHECK_OUT_NOT_OPEN_YET
Jika dalam rentang check-out → approved

Status manual
Jika ada manual status aktif untuk tanggal itu:
check-in/check-out fisik ditolak
reason: MANUAL_STATUS_EXISTS

Libur akademik
Jika tanggal termasuk event akademik yang menutup absensi:
check-in/check-out ditolak
reason: ACADEMIC_EVENT_BLOCK

11. Android Behavior
Android tetap fokus ke absensi fisik
Android cukup:
cek Wi-Fi
cek lokasi
kirim check-in/check-out
tampilkan status hari ini

Android tidak menangani izin manual
Untuk tahap awal:
siswa tidak submit izin dari Android
Android hanya membaca hasil status harian dari backend

Jika status hari ini adalah:
excused
sick
dispensation
holiday
maka:
tombol check-in dimatikan
tombol check-out dimatikan
tampilkan badge/status informasi

12. Tampilan UI Android yang Diinginkan
Halaman attendance

Tampilkan:

Jam sekarang
Tanggal
Status lokasi / Wi-Fi
Status kehadiran hari ini
Check-in history
Check-out history
Badge:
- Hadir
- Terlambat
- Izin
- Sakit
- Dispensasi
- Libur

Tombol
jika belum check-in dan status manual tidak ada → tombol Masuk aktif
jika sudah check-in dan belum check-out → tombol Pulang aktif
jika status manual ada → kedua tombol nonaktif

13. Web/Admin Behavior
Halaman detail siswa
Harus bisa menampilkan:
Riwayat absensi fisik
Riwayat status manual
Status akhir harian

Tabel riwayat
Kolom yang disarankan:
Tanggal
Check-in
Check-out
Status
Lokasi
Alasan
Sumber (Fisik / Manual)

14. Rekap Final Harian
Backend perlu service untuk menggabungkan:
kalender akademik
manual status
daily attendance fisik
Contoh output final per hari:
{
  "date": "2026-04-29",
  "status": "late",
  "label": "Terlambat",
  "source": "daily_attendance",
  "check_in_at": "2026-04-29T07:18:00+07:00",
  "check_out_at": null,
  "late_minutes": 18
}
Contoh lain:
{
  "date": "2026-05-01",
  "status": "excused",
  "label": "Izin",
  "source": "manual_status",
  "check_in_at": null,
  "check_out_at": null,
  "late_minutes": null
}

15. Acceptance Criteria
Agent harus memastikan:
Siswa bisa check-in antara 06:00–07:00 dan statusnya present.
Siswa tetap bisa check-in setelah 07:00 selama masih dalam batas akhir, dan statusnya late.
late_minutes dihitung dari selisih terhadap 07:00.
Validasi Wi-Fi dan radius tetap berjalan.
Hari libur akademik menutup absensi.
Status manual dari admin/guru mengalahkan absensi fisik.
Android cukup membaca status final dari backend.
Endpoint today mengembalikan status final harian yang siap ditampilkan di aplikasi.
Riwayat admin/web bisa membedakan antara absensi fisik dan status manual.
Sistem tidak lagi menggantungkan absensi masuk harian pada jadwal aktif per mapel.

16. Catatan Penting untuk Agent
Jangan pertahankan model lama sebagai basis utama
Jangan menjadikan:
jadwal mapel aktif
window start_time - 15 / +20
sebagai rule utama absensi masuk harian.
Jadwal tetap boleh dipakai untuk:
modul jadwal pelajaran
rekap pelajaran
absensi per mapel di masa depan
tetapi bukan untuk menentukan check-in masuk sekolah harian.

Fokus implementasi tahap ini
daily attendance
manual excuse
final daily status
Android tetap sederhana
admin bisa kelola izin manual

17. Output yang Saya Inginkan dari Agent

Tolong hasilkan:
analisis perubahan dari model absensi lama ke daily attendance,
desain database yang diperlukan,
migration Laravel,
model dan relasi,
request validation,
controller / service logic,
endpoint API student,
endpoint admin manual status,
aturan final status harian,
penyesuaian Android response contract,
penyesuaian halaman admin/web,
daftar file yang perlu dibuat/diubah.