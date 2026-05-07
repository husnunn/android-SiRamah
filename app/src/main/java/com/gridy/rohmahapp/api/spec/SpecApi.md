# Attendance API Specification

Yang berubah di API (ringkas)
Siswa

GET /api/v1/student/daily-attendance/today → sekarang ada attendance_sites di akar JSON (bukan di dalam data saja).
GET /api/v1/student/me → sama, attendance_sites di akar respons (bersanding dengan data).
Guru

GET /api/v1/teacher/me → attendance_sites di akar.
GET /api/v1/teacher/attendance/today → attendance_sites di akar.
Isi tiap elemen: id, name, latitude, longitude, radius_m. id dipakai sebagai attendance_site_id di body POST …/check-in dan check-out (legacy guru/siswa maupun daily siswa).

Yang perlu di Android (checklist)
Area	Penyesuaian
Model / parsing -> Baca field attendance_sites dari root response (bukan hanya data).
Pemilih titik  -> Saat akan absen: isi attendance_site_id dari attendance_sites[].id (dropdown/map), bukan hardcode saja.
Cache -> Bisa simpan daftar dari /me lalu refresh dengan …/today bila perlu.
Retrofit / Gson -> Pastikan DTO tidak ignore unknown lalu membuang attendance_sites, atau tambahkan property opsional agar tidak error.
Satu titik -> Jika attendance_sites.size == 1, bisa auto-pilih id itu untuk UX.

## Overview

Spesifikasi ini mencakup kontrak request/response untuk fitur absensi dengan **dua jalur untuk siswa**:

| Jalur | Base path (siswa) | Penyimpanan | Penentu waktu / aturan utama |
|--------|---------------------|-------------|------------------------------|
| **Legacy (jadwal mapel)** | `/api/v1/student/attendance/*` | `attendance_records` (+ `schedule_id`) | Jadwal aktif + window ± menit dari `start_time` |
| **Absensi harian sekolah** | `/api/v1/student/daily-attendance/*` | `daily_attendances` | Jam operasional harian (`config/school_daily_attendance.php`, timezone sekolah) — **tanpa** syarat slot mapel |

Keduanya memakai validasi yang sama untuk:

- jaringan Wi-Fi (SSID, BSSID, subnet/IP)
- lokasi (lat/lng + radius titik absensi)
- kalender akademik (libur / event yang menutup absensi)

Backend adalah penentu final: legacy memakai `approved` / `rejected` pada `attendance_records`; absensi harian memakai **`status` respons** `approved` / `rejected` pada payload JSON, dengan **`attendance_status` fisik** `present` / `late` pada baris sukses check-in (lihat bawah).

**Guru** hanya memakai jalur legacy: `/api/v1/teacher/attendance/*` (berbasis jadwal), sama seperti sebelumnya.

---

## General Conventions

- Base path: `/api/v1`
- Auth: `Authorization: Bearer <sanctum_token>`
- Content type: `application/json`
- Datetime format: ISO8601 (contoh: `2026-04-28T07:15:30Z` atau offset lokal `2026-04-28T07:15:30+07:00`)

### Legacy (jadwal mapel)

- `attendance_type` (request): `check_in`, `check_out`
- Status rekaman: `approved`, `rejected`

### Absensi harian (siswa)

- Request **tanpa** field `attendance_type` (check-in dan check-out memakai URL berbeda).
- Status **fisik** pada rekaman sukses: `present`, `late` (field `record.status` dan `attendance_status` pada respons check-in).
- Status **akhir hari** pada `GET …/daily-attendance/today`: `present`, `late`, `excused`, `sick`, `dispensation`, `absent`, `holiday` (lihat bagian Daily `today`).

### Timezone sekolah (`SCHOOL_TIMEZONE`)

Backend memakai **satu timezone dinding sekolah** (nama IANA, contoh `Asia/Jakarta`) dari konfigurasi `SCHOOL_TIMEZONE` (biasanya `config('app.school_timezone')`). Timezone ini dipakai untuk interpretasi `client_time`, tanggal kalender, dan serialisasi waktu ke klien.

#### A. Legacy siswa & guru (`/attendance/*`)

| Konteks | Perilaku |
|----------|-----------|
| **`client_time`** | Divalidasi lalu **dikonversi ke timezone sekolah** sebelum eligibility, jadwal, dan kalender akademik dievaluasi. |
| **Tanpa `client_time`** | Waktu absensi = **`now()` di timezone sekolah**. |
| **Hari kerja (`isoWeekday`)** | Dari tanggal & jam lokal sekolah (Senin–Sabtu = 1–6; Minggu tidak dianggap hari jadwal). |
| **Jam jadwal (`start_time`, `end_time`)** | Jam dinding di DB; digabung dengan **tanggal kalender sekolah** pada momen absensi. |
| **Window check-in** | **`start_time − 15 menit`** … **`start_time + 20 menit`** (inklusif), timezone sekolah. |
| **Window check-out** | **`start_time`** … **`end_time`** pada tanggal kalender sekolah yang sama (inklusif). |
| **Kalender akademik** | Overlap `start_date` … `end_date` dengan **tanggal kalender sekolah** dari momen absensi. |
| **`GET …/attendance/today`** | Rekaman yang **`attendance_at`** jatuh di antara awal dan akhir **hari kalender** timezone sekolah. |

#### B. Absensi harian siswa (`/daily-attendance/*`)

Jam dinding diambil dari **`config/school_daily_attendance.php`** (semua string `H:i` atau `H:i:s`, diinterpretasikan di **timezone sekolah** pada **tanggal kalender** momen absensi):

| Kunci | Default | Env (opsional) |
|--------|---------|----------------|
| Buka check-in | `06:00` | `DAILY_CHECK_IN_OPEN` |
| Batas hadir tepat waktu | `07:00` | `DAILY_CHECK_IN_ON_TIME_UNTIL` |
| Tutup check-in | `09:00` | `DAILY_CHECK_IN_CLOSE` |
| Buka check-out | `12:00` | `DAILY_CHECK_OUT_OPEN` |
| Tutup check-out | `18:00` | `DAILY_CHECK_OUT_CLOSE` |

| Konteks | Perilaku |
|----------|-----------|
| **`client_time`** | Sama seperti legacy: di-resolve ke momen di timezone sekolah untuk cek jendela & tanggal `date` pada `daily_attendances`. |
| **Check-in hadir tepat waktu** | `open` ≤ waktu ≤ `on_time_until` → `present`, `late_minutes = 0`. |
| **Check-in terlambat** | `on_time_until` < waktu ≤ `close` → `late`, `late_minutes` = menit setelah `on_time_until`. |
| **Kalender akademik** | Sama pola blok dengan legacy (`ACADEMIC_EVENT_BLOCK` bila event menutup absensi). |
| **Status manual admin** | Jika ada entri manual **approved** untuk siswa + tanggal tersebut, check-in/out fisik ditolak (`MANUAL_STATUS_EXISTS`). |
| **`GET …/daily-attendance/today`** | Satu objek **status akhir hari ini** (gabungan: libur → manual → fisik → alpa) + `can_check_in` / `can_check_out`. |

Endpoint jadwal JSON (bukan path absensi; aturan tanggal sama dengan legacy):

- `GET /api/v1/student/schedule?date=YYYY-MM-DD`
- `GET /api/v1/teacher/schedule?date=YYYY-MM-DD`

Parameter `date` = tanggal kalender di timezone sekolah.

---

## Student attendance — legacy (jadwal mapel)

Endpoint: `POST /api/v1/student/attendance/check-in`, `POST …/check-out`, `GET …/attendance/today`.

Perilaku mengikuti tabel **Legacy** di atas. Request memuat `attendance_type`. Respons memakai bentuk `AttendanceRecordResource` (`attendance_time`, `schedule_id`, dll.).

### POST `/api/v1/student/attendance/check-in`

#### Request Body

```json
{
  "attendance_site_id": 1,
  "attendance_type": "check_in",
  "client_time": "2026-04-28T07:15:30Z",
  "network": {
    "ssid": "SCHOOL-WIFI",
    "bssid": "AA:BB:CC:11:22:33",
    "local_ip": "192.168.1.25",
    "gateway_ip": "192.168.1.1",
    "subnet_prefix": 24,
    "transport": "WIFI"
  },
  "location": {
    "latitude": -6.20001,
    "longitude": 106.81668,
    "accuracy_m": 10,
    "provider": "fused",
    "is_mock": false,
    "captured_at": "2026-04-28T07:15:28Z"
  },
  "device": {
    "platform": "android",
    "app_version": "1.0.0",
    "os_version": "14"
  }
}
```

#### Response 200 (Approved)

```json
{
  "message": "Absensi berhasil disetujui.",
  "status": "approved",
  "reason_code": null,
  "reason_detail": null,
  "validation": {
    "eligibility": {
      "allowed": true,
      "reason_code": null,
      "reason_detail": null,
      "schedule_id": 10,
      "matched_event_id": null
    },
    "evidence": {
      "valid": true,
      "reason_code": null,
      "reason_detail": null,
      "distance_m": 24.3,
      "wifi_rule_id": 5,
      "site_id": 1
    }
  },
  "record": {
    "id": 1001,
    "attendance_type": "check_in",
    "status": "approved",
    "attendance_time": "2026-04-28T07:15:30+07:00",
    "reason_code": null,
    "reason_detail": null,
    "distance_m": 24.3,
    "site": {
      "id": 1,
      "name": "SMA Negeri 1"
    },
    "schedule_id": 10,
    "network": {
      "ssid": "SCHOOL-WIFI"
    },
    "location": {
      "latitude": -6.20001,
      "longitude": 106.81668
    },
    "created_at": "2026-04-28T07:15:31+07:00"
  }
}
```

#### Response 422 (Rejected)

Backend tetap dapat membuat baris `attendance_records` dengan `status: rejected` (mirror implementasi). Respons mencakup `record` + `validation`.

```json
{
  "message": "Absensi ditolak oleh sistem validasi.",
  "status": "rejected",
  "reason_code": "ACADEMIC_EVENT_BLOCK",
  "reason_detail": "Absensi tidak diizinkan karena event akademik: Libur Nasional.",
  "validation": {
    "eligibility": {
      "allowed": false,
      "reason_code": "ACADEMIC_EVENT_BLOCK",
      "reason_detail": "Absensi tidak diizinkan karena event akademik: Libur Nasional.",
      "schedule_id": null,
      "matched_event_id": 7
    },
    "evidence": {
      "valid": true,
      "reason_code": null,
      "reason_detail": null,
      "distance_m": 14.1,
      "wifi_rule_id": 5,
      "site_id": 1
    }
  },
  "record": {
    "id": 1002,
    "attendance_type": "check_in",
    "status": "rejected",
    "attendance_time": "2026-04-28T07:20:00+07:00",
    "reason_code": "ACADEMIC_EVENT_BLOCK",
    "reason_detail": "Absensi tidak diizinkan karena event akademik: Libur Nasional.",
    "distance_m": 14.1,
    "site": {
      "id": 1,
      "name": "SMA Negeri 1"
    },
    "schedule_id": null,
    "network": {
      "ssid": "SCHOOL-WIFI"
    },
    "location": {
      "latitude": -6.20001,
      "longitude": 106.81668
    },
    "created_at": "2026-04-28T07:20:01+07:00"
  }
}
```

### POST `/api/v1/student/attendance/check-out`

Struktur request/response sama seperti `check-in`, dengan `attendance_type = check_out`.

### GET `/api/v1/student/attendance/today`

Filter **hari ini** = timezone sekolah; mengembalikan **array** rekaman hari tersebut (bentuk resource legacy).

```json
{
  "data": [
    {
      "id": 1001,
      "attendance_type": "check_in",
      "status": "approved",
      "attendance_time": "2026-04-28T07:15:30+07:00",
      "reason_code": null,
      "reason_detail": null,
      "distance_m": 24.3,
      "site": {
        "id": 1,
        "name": "SMA Negeri 1"
      },
      "schedule_id": 10,
      "network": {
        "ssid": "SCHOOL-WIFI"
      },
      "location": {
        "latitude": -6.20001,
        "longitude": 106.81668
      },
      "created_at": "2026-04-28T07:15:31+07:00"
    }
  ]
}
```

---

## Student attendance — daily (absensi harian)

Endpoint:

- `POST /api/v1/student/daily-attendance/check-in`
- `POST /api/v1/student/daily-attendance/check-out`
- `GET /api/v1/student/daily-attendance/today`

**Tidak** memakai `attendance_type` di body. Eligibility **tidak** memakai jadwal mapel; tetap memerlukan profil siswa dan kelas aktif (tahun ajaran aktif), serta aturan kalender / manual / jendela jam di atas.

Detail bisnis & contoh tambahan: `spec/AbsenBaru.md`.

### POST `/api/v1/student/daily-attendance/check-in`

#### Request Body

```json
{
  "attendance_site_id": 1,
  "client_time": "2026-04-29T06:45:00+07:00",
  "network": {
    "ssid": "SCHOOL-WIFI",
    "bssid": "AA:BB:CC:11:22:33",
    "local_ip": "192.168.1.25",
    "gateway_ip": "192.168.1.1",
    "subnet_prefix": 24,
    "transport": "WIFI"
  },
  "location": {
    "latitude": -6.20001,
    "longitude": 106.81668,
    "accuracy_m": 10,
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
```

#### Response 200 — hadir tepat waktu (`present`)

```json
{
  "message": "Absensi masuk berhasil.",
  "status": "approved",
  "attendance_status": "present",
  "late_minutes": 0,
  "reason_code": null,
  "reason_detail": null,
  "record": {
    "id": 2001,
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
```

#### Response 200 — terlambat (`late`)

Saat check-in setelah `on_time_until` tetapi masih sebelum `close`, `reason_code` pada respons sukses dapat berisi `LATE_CHECK_IN`.

```json
{
  "message": "Absensi masuk berhasil, tercatat terlambat.",
  "status": "approved",
  "attendance_status": "late",
  "late_minutes": 18,
  "reason_code": "LATE_CHECK_IN",
  "reason_detail": "Check-in setelah batas hadir tepat waktu.",
  "record": {
    "id": 2002,
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
```

#### Response 422 (Rejected)

Penolakan **tidak** membuat/mengubah baris `daily_attendances`. Respons **tanpa** field `record`. Isi `validation.eligibility` mengikuti layanan eligibility (untuk beberapa penolakan **check-out**, field `attendance_status` / `late_minutes` bisa tidak ada).

```json
{
  "message": "Absensi ditolak oleh sistem validasi.",
  "status": "rejected",
  "reason_code": "MANUAL_STATUS_EXISTS",
  "reason_detail": "Sudah ada status manual untuk tanggal ini.",
  "validation": {
    "eligibility": {
      "allowed": false,
      "reason_code": "MANUAL_STATUS_EXISTS",
      "reason_detail": "Sudah ada status manual untuk tanggal ini.",
      "attendance_status": null,
      "late_minutes": null,
      "calendar_event_id": null
    },
    "evidence": {
      "valid": true,
      "reason_code": null,
      "reason_detail": null,
      "distance_m": 12.5,
      "wifi_rule_id": 5,
      "site_id": 1
    }
  }
}
```

### POST `/api/v1/student/daily-attendance/check-out`

Body sama seperti check-in (tanpa `attendance_type`). Backend mengisi `check_out_at` pada baris `daily_attendances` hari yang sama (setelah check-in sukses sebelumnya).

#### Response 200

```json
{
  "message": "Absensi pulang berhasil.",
  "status": "approved",
  "record": {
    "id": 2001,
    "date": "2026-04-29",
    "check_in_at": "2026-04-29T06:45:00+07:00",
    "check_out_at": "2026-04-29T15:02:00+07:00",
    "status": "present",
    "late_minutes": 0,
    "site": {
      "id": 1,
      "name": "Kantor EBD"
    }
  }
}
```

#### Response 422

Sama bentuknya dengan check-in ditolak (`message`, `status`, `reason_code`, `reason_detail`, `validation`), tanpa `record`.

### GET `/api/v1/student/daily-attendance/today`

Satu objek **`data`** untuk **hari kalender** saat ini (timezone sekolah): status akhir, label Bahasa Indonesia, sumber, waktu check-in/out bila ada, serta flag UI.

Di **akar respons** (bersanding dengan `data`) ada array **`attendance_sites`**: semua titik absensi **`is_active = true`**, urut nama — dipakai klien untuk memilih **`attendance_site_id`** pada `POST …/daily-attendance/check-in` dan `check-out` (isi yang sama dengan field `site` pada `data` bila sudah pernah check-in).

Setiap elemen: `id`, `name`, `latitude`, `longitude`, `radius_m`.

#### Contoh — sudah check-in hadir

```json
{
  "data": {
    "date": "2026-04-29",
    "status": "present",
    "label": "Hadir",
    "source": "daily_attendance",
    "check_in_at": "2026-04-29T06:45:00+07:00",
    "check_out_at": null,
    "late_minutes": 0,
    "message": null,
    "site": {
      "id": 1,
      "name": "Kantor EBD"
    },
    "can_check_in": false,
    "can_check_out": true
  },
  "attendance_sites": [
    {
      "id": 1,
      "name": "Kantor EBD",
      "latitude": -6.2,
      "longitude": 106.816666,
      "radius_m": 150
    }
  ]
}
```

#### Contoh — izin manual (`excused`)

```json
{
  "data": {
    "date": "2026-04-29",
    "status": "excused",
    "label": "Izin",
    "source": "manual_status",
    "check_in_at": null,
    "check_out_at": null,
    "late_minutes": null,
    "message": "Anda telah diberi status izin oleh guru/admin.",
    "site": null,
    "can_check_in": false,
    "can_check_out": false
  },
  "attendance_sites": [
    {
      "id": 1,
      "name": "Kantor EBD",
      "latitude": -6.2,
      "longitude": 106.816666,
      "radius_m": 150
    }
  ]
}
```

Nilai `status` yang mungkin pada objek ini: `present`, `late`, `excused`, `sick`, `dispensation`, `absent`, `holiday`.  
Nilai `source`: `holiday`, `manual_status`, `daily_attendance`, `absent`.

### GET `/api/v1/student/me`

Respons profil siswa (objek utama di `data`) disertai **`attendance_sites`** di akar respons dengan bentuk yang **sama** seperti pada `GET …/daily-attendance/today`, agar aplikasi bisa cache daftar titik saat login tanpa memanggil `today` dulu.

---

## Teacher Attendance Endpoints

- `GET /api/v1/teacher/me` — profil guru di `data`, plus **`attendance_sites`** di akar respons (sama bentuknya seperti di API siswa) untuk memilih `attendance_site_id` saat check-in/check-out.
- `POST /api/v1/teacher/attendance/check-in`
- `POST /api/v1/teacher/attendance/check-out`
- `GET /api/v1/teacher/attendance/today` — array rekaman hari ini di `data`, plus **`attendance_sites`** di akar respons.

Struktur request/response check-in/out sama dengan **student legacy**. Aturan **timezone sekolah** dan jadwal sama seperti bagian **Legacy** di atas. **Tidak** ada endpoint `daily-attendance` untuk guru pada implementasi saat ini.

---

## Validation Rules

### Legacy (`StoreAttendanceRequest` — siswa & guru)

- `attendance_site_id`: required, integer, exists
- `attendance_type`: required, `check_in|check_out`
- `client_time`: nullable, datetime (disarankan ISO8601); dipakai dalam **timezone sekolah**
- `network`, `location`, `device`: seperti sebelumnya

### Daily siswa (`StoreDailyAttendanceRequest`)

- `attendance_site_id`: required, integer, exists
- **`attendance_type`**: tidak digunakan (tidak dikirim)
- `client_time`: nullable, datetime
- `network`: required object (`ssid`, `bssid`, `local_ip`, `gateway_ip`, `subnet_prefix`, `transport` — sesuai aturan yang sama dengan legacy)
- `location`: required object (`latitude`, `longitude`, `accuracy_m`, `provider`, `is_mock`, `captured_at`)
- `device`: nullable object (`platform`, `app_version`, `os_version`)

---

## Reason Code Catalog

### Bukti lokasi / Wi-Fi / titik (kedua jalur)

- `WIFI_NOT_CONNECTED`
- `WIFI_NOT_MATCHED`
- `SITE_NOT_ACTIVE`
- `MOCK_LOCATION_DETECTED`
- `OUT_OF_RADIUS`

### Legacy — eligibility jadwal & profil

- `ACADEMIC_EVENT_BLOCK`
- `NO_ACTIVE_SCHEDULE` — tidak ada slot jadwal dalam window check-in (**H−15 … H+20** dari `start_time`) atau check-out di luar `start_time`–`end_time`, atau Minggu / tanpa jadwal cocok
- `NO_ACTIVE_CLASS`
- `PROFILE_NOT_FOUND`

### Daily — eligibility & jendela jam (hanya `/daily-attendance/*`)

- `ACADEMIC_EVENT_BLOCK` — event akademik menutup absensi (sama pola dengan legacy)
- `MANUAL_STATUS_EXISTS` — sudah ada status manual **approved** untuk tanggal tersebut
- `CHECK_IN_NOT_OPEN_YET` — sebelum jam buka check-in harian
- `CHECK_IN_CLOSED` — setelah jam tutup check-in harian
- `CHECK_OUT_NOT_OPEN_YET` — sebelum jam buka check-out harian
- `CHECK_OUT_CLOSED` — setelah jam tutup check-out harian
- `ALREADY_CHECKED_IN` — check-in hari ini sudah tercatat
- `ALREADY_CHECKED_OUT` — check-out hari ini sudah tercatat
- `NO_CHECK_IN` — check-out tanpa check-in hari yang sama
- `NO_ACTIVE_CLASS` — siswa tanpa kelas aktif (tahun ajaran aktif)
- `PROFILE_NOT_FOUND` — bukan role siswa atau tanpa profil siswa

### Informasi (bukan penolakan) — daily check-in sukses terlambat

- `LATE_CHECK_IN` — dapat muncul pada respons **200** bersama `attendance_status: late` (bukan `status: rejected`).

---

## Admin Web Payload Spec (Inertia Forms)

Catatan: endpoint admin berjalan di web routes (Inertia), bukan API JSON murni. Bagian ini menjelaskan bentuk payload yang dikirim form.

### Status manual absensi siswa

- `POST /admin/students/{student}/attendance-manual-statuses` — body JSON/urlencoded: `date` (YYYY-MM-DD), `type` (`excused`|`sick`|`dispensation`), `reason`, `notes` (opsional), `attendance_site_id` (opsional)
- `PUT /admin/students/{student}/attendance-manual-statuses/{manualStatus}` — perbarui field yang sama (parsial didukung sesuai validasi backend)
- `PATCH /admin/students/{student}/attendance-manual-statuses/{manualStatus}/cancel` — batalkan entri manual (status menjadi `cancelled`)

`{student}` = ID `student_profiles`. Hanya pengguna dengan akses admin web (middleware `role:admin`) yang dapat memanggil rute ini.

## Attendance Sites

### POST `/admin/attendance-sites`

```json
{
  "name": "SMA Negeri 1",
  "latitude": -6.2,
  "longitude": 106.81,
  "radius_m": 150,
  "is_active": true,
  "notes": "Gerbang utama",
  "wifi_rules": [
    {
      "ssid": "SCHOOL-WIFI",
      "bssid": "AA:BB:CC:11:22:33",
      "ip_subnet": "192.168.1.0/24",
      "is_active": true
    }
  ]
}
```

### PUT `/admin/attendance-sites/{attendanceSite}`

Payload sama seperti create.

### PATCH `/admin/attendance-sites/{attendanceSite}/toggle-active`

Tanpa body (toggle status aktif/nonaktif).

## Academic Calendar Events

Tanggal `start_date` / `end_date` adalah **tanggal kalender** (tanpa jam). Saat absensi, overlap dicek terhadap **tanggal kalender di timezone sekolah** pada momen `client_time` (atau `now` sekolah jika `client_time` kosong).

### POST `/admin/academic-calendar-events`

```json
{
  "name": "Libur Nasional",
  "start_date": "2026-05-01",
  "end_date": "2026-05-01",
  "event_type": "national_holiday",
  "is_active": true,
  "allow_attendance": false,
  "override_schedule": true,
  "notes": "Hari Buruh"
}
```

### PUT `/admin/academic-calendar-events/{academicCalendarEvent}`

Payload sama seperti create.

---

## Suggested Android Error Mapping

### Umum (kedua jalur siswa + guru)

- `WIFI_NOT_CONNECTED` → "Perangkat belum terhubung ke Wi-Fi sekolah."
- `LOCATION_DISABLED` → "Aktifkan GPS/lokasi terlebih dahulu." (jika klien mendeteksi sebelum kirim)
- `OUT_OF_RADIUS` → "Anda berada di luar area absensi sekolah."
- `ACADEMIC_EVENT_BLOCK` → "Absensi ditutup karena kalender akademik."

### Legacy siswa

- `NO_ACTIVE_SCHEDULE` → "Tidak ada jadwal aktif pada waktu ini."

### Daily siswa (disarankan gunakan endpoint `/daily-attendance/*` untuk UI masuk/pulang sekolah)

- `CHECK_IN_NOT_OPEN_YET` / `CHECK_OUT_NOT_OPEN_YET` → "Belum dalam jam absensi."
- `CHECK_IN_CLOSED` / `CHECK_OUT_CLOSED` → "Jam absensi telah berakhir."
- `MANUAL_STATUS_EXISTS` → "Status hari ini sudah diatur oleh sekolah (izin/sakit/dispensasi)."
- `ALREADY_CHECKED_IN` / `ALREADY_CHECKED_OUT` → "Absensi sudah tercatat."
- `NO_CHECK_IN` → "Lakukan absen masuk terlebih dahulu."
- `NO_ACTIVE_CLASS` / `PROFILE_NOT_FOUND` → hubungi admin / periksa data akun.

- fallback → "Absensi ditolak sistem. Silakan coba lagi."
