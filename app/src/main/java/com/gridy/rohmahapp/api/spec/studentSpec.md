# API mobile siswa (`/api/v1/student`)

Dokumen ini mencerminkan **kontrak aktual** backend: field request (validasi) dan bentuk response per endpoint.  
Detail aturan bisnis absensi (kode penolakan, contoh panjang) tetap dirujuk ke `spec/ATTENDANCE_API_SPEC.md`.

---

## Konvensi umum

| Item | Nilai |
|------|--------|
| Base URL (relatif) | `/api/v1/student` |
| Prefix global | Seluruh rute di bawah ini diawali `/api` (Laravel) → path penuh contoh: `/api/v1/student/me` |
| Content-Type | `application/json` |
| Auth (kecuali login) | Header `Authorization: Bearer <plainTextToken>` (Sanctum). User harus punya role **`student`** di kolom `users.roles` (JSON). |
| Waktu | `client_time` dan timestamp di response memakai **ISO 8601**; interpretasi kalender mengikuti **timezone sekolah** (`SCHOOL_TIMEZONE` / `config('app.school_timezone')`). |
| Validasi gagal | **422** + body validasi Laravel (`message`, `errors` object). Login gagal memakai **422** + `errors.username`. |

---

## 1. Autentikasi

### `POST /api/v1/student/login`

**Auth:** tidak perlu.

#### Request body (JSON)

| Field | Wajib | Keterangan |
|--------|--------|------------|
| `username` | ya | string |
| `password` | ya | string |

#### Response `200 OK`

```json
{
  "message": "Login berhasil.",
  "token": "<sanctum_plain_text_token>",
  "must_change_password": false,
  "user": {
    "id": 1,
    "name": "Nama di akun",
    "username": "siswa01",
    "role": ["student"]
  }
}
```

- `must_change_password`: boolean; jika `true`, klien disarankan memaksa alur ganti password (`POST /change-password`).
- `role`: array isi role user (bukan string tunggal).

#### Response `422 Unprocessable Entity`

- Kredensial salah atau bukan akun siswa: `errors.username` berisi pesan (mis. *Username atau password salah.*).
- Akun nonaktif: pesan pada `errors.username` bahwa akun dinonaktifkan.

---

### `POST /api/v1/student/logout`

**Auth:** Bearer (token yang akan dicabut).

#### Request body

Kosong (body tidak dipakai).

#### Response `200 OK`

```json
{
  "message": "Logout berhasil."
}
```

---

## 2. Profil

### `GET /api/v1/student/me`

**Auth:** Bearer, role `student`.

#### Request

Tanpa body. Tanpa query wajib.

#### Response `200 OK`

Resource Laravel: objek profil di **`data`**, plus **`attendance_sites`** di root (untuk pemilih titik absensi).

```json
{
  "data": {
    "id": 10,
    "nis": "00123",
    "nisn": "0123456789",
    "full_name": "Nama Lengkap Siswa",
    "gender": "male",
    "birth_date": "2008-05-05",
    "birth_place": "Jakarta",
    "phone": "081234567890",
    "address": "...",
    "parent_name": "...",
    "parent_phone": "...",
    "photo_url": "https://example.com/storage/...",
    "current_class": {
      "id": 5,
      "name": "X IPA 1",
      "level": "10",
      "homeroom_teacher": "Nama Guru Wali"
    },
    "user": {
      "id": 1,
      "username": "siswa01",
      "is_active": true
    }
  },
  "attendance_sites": [
    {
      "id": 1,
      "name": "Gerbang Utama",
      "latitude": -6.2,
      "longitude": 106.81,
      "radius_m": 200
    }
  ]
}
```

- `current_class`: `null` jika tidak ada kelas aktif untuk tahun ajaran terkait (relasi `activeClass` kosong).
- `photo_url`: `null` jika tidak ada file foto.
- `gender` dan field lain mengikuti nilai di DB (string sesuai model).

#### Response `404`

Profil siswa (`student_profiles`) belum ada untuk user ini.

---

## 3. Jadwal

### `GET /api/v1/student/schedule`

**Auth:** Bearer, role `student`.

#### Query string (opsional)

| Parameter | Contoh | Keterangan |
|-----------|--------|------------|
| `date` | `2026-05-05` | Tanggal kalender (timezone sekolah). Jika ada **event akademik aktif** yang overlap tanggal tersebut dan (`allow_attendance = false` **atau** `override_schedule = true`), response berisi **`data`: []**. |
| `semester` | `1` | Filter `schedules.semester`. |
| `day` | `1` … `6` | Filter `day_of_week` (1 = Senin … 6 = Sabtu). |

#### Response `200 OK`

```json
{
  "data": [
    {
      "id": 100,
      "day": "Senin",
      "day_of_week": 1,
      "start_time": "08:00",
      "end_time": "09:00",
      "room": "Ruang 101",
      "semester": 1,
      "notes": null,
      "subject": {
        "id": 3,
        "code": "MTK",
        "name": "Matematika"
      },
      "class": {
        "id": 5,
        "name": "X IPA 1",
        "level": "10"
      },
      "teacher": {
        "id": 2,
        "full_name": "Budi Guru",
        "nip": "198001012010011001"
      },
      "school_year": "2025/2026"
    }
  ]
}
```

- Jika siswa **tanpa kelas aktif** pada tahun ajaran aktif: **`data`: []** (bukan error).
- Urutan: `day_of_week` naik, lalu `start_time` naik.

#### Response `404`

Belum ada `student_profile`.

---

## 4. Absensi legacy (berbasis jadwal mapel)

Path: `/api/v1/student/attendance/...`

Menggunakan **`StoreAttendanceRequest`**: siswa mengirim `attendance_type` danbukti lokasi/jaringan. Rekaman disimpan di `attendance_records` (terikat jadwal bila eligibility lolos).

Detail contoh 200/422 dan semua `reason_code` lihat **`spec/ATTENDANCE_API_SPEC.md`** bagian siswa legacy.

### `POST /api/v1/student/attendance/check-in`
### `POST /api/v1/student/attendance/check-out`

#### Request body (JSON)

| Field | Wajib | Keterangan |
|--------|--------|------------|
| `attendance_site_id` | ya | integer, harus `exists:attendance_sites,id` |
| `attendance_type` | ya | `check_in` atau `check_out` (meski server juga memaksa tipe dari URL, field ini tetap divalidasi) |
| `client_time` | tidak | datetime (nullable) |
| `network` | ya | object |
| `network.ssid` | tidak | string, max 255 |
| `network.bssid` | tidak | string, max 17 |
| `network.local_ip` | tidak | format IP |
| `network.gateway_ip` | tidak | format IP |
| `network.subnet_prefix` | tidak | integer 0–32 |
| `network.transport` | tidak | string, max 20 |
| `location` | ya | object |
| `location.latitude` | ya | numeric, -90 … 90 |
| `location.longitude` | ya | numeric, -180 … 180 |
| `location.accuracy_m` | tidak | numeric ≥ 0 |
| `location.provider` | tidak | string, max 50 |
| `location.is_mock` | tidak | boolean |
| `location.captured_at` | tidak | datetime |
| `device` | tidak | object |
| `device.platform` | tidak | string, max 20 |
| `device.app_version` | tidak | string, max 30 |
| `device.os_version` | tidak | string, max 30 |

#### Response `200` / `422`

Body selalu memuat setidaknya:

```json
{
  "message": "Absensi berhasil disetujui. | Absensi ditolak oleh sistem validasi.",
  "status": "approved | rejected",
  "reason_code": null,
  "reason_detail": null,
  "validation": {
    "eligibility": {},
    "evidence": {}
  },
  "record": {}
}
```

- **`record`**: bentuk `AttendanceRecordResource` setelah relasi `attendanceSite` diload:

| Field | Keterangan |
|-------|------------|
| `id` | integer |
| `attendance_type` | `check_in` / `check_out` |
| `status` | `approved` / `rejected` |
| `attendance_time` | ISO8601 (mirror `attendance_at` di timezone sekolah) |
| `reason_code` | string atau null |
| `reason_detail` | string atau null |
| `distance_m` | float atau null |
| `site` | `{ "id", "name" }` atau null |
| `schedule_id` | integer atau null |
| `network` | object (payload tersimpan) |
| `location` | object (payload tersimpan) |
| `created_at` | ISO8601 (timezone sekolah) |

HTTP status: **200** jika `status === approved`, **422** jika `rejected`.

---

### `GET /api/v1/student/attendance/today`

**Auth:** Bearer.

#### Request

Tanpa body. Tanpa query wajib.

#### Response `200 OK`

Hanya rekaman **hari kalender saat ini** (timezone sekolah) untuk user login.

```json
{
  "data": [
    {
      "id": 1001,
      "attendance_type": "check_in",
      "status": "approved",
      "attendance_time": "2026-05-05T07:15:00+07:00",
      "reason_code": null,
      "reason_detail": null,
      "distance_m": 12.5,
      "site": { "id": 1, "name": "Gerbang A" },
      "schedule_id": 10,
      "network": { "ssid": "SCHOOL-WIFI" },
      "location": { "latitude": -6.2, "longitude": 106.81 },
      "created_at": "2026-05-05T07:15:01+07:00"
    }
  ]
}
```

**Tidak** ada field `attendance_sites` di endpoint ini (daftar titik dari `GET /me` atau `GET .../daily-attendance/today`).

---

## 5. Absensi harian sekolah (non-jadwal mapel)

Path: `/api/v1/student/daily-attendance/...`

Request memakai **`StoreDailyAttendanceRequest`** — **tanpa** field `attendance_type` (check-in vs check-out ditentukan oleh URL).

### `POST /api/v1/student/daily-attendance/check-in`
### `POST /api/v1/student/daily-attendance/check-out`

#### Request body (JSON)

| Field | Wajib | Keterangan |
|--------|--------|------------|
| `attendance_site_id` | ya | integer, `exists:attendance_sites,id` |
| `client_time` | tidak | datetime |
| `network` | ya | sama struktur dengan legacy (lihat tabel di atas) |
| `location` | ya | sama struktur dengan legacy |
| `device` | tidak | sama struktur opsional dengan legacy |

#### Response check-in `200 OK` (disetujui)

```json
{
  "message": "Absensi masuk berhasil. | Absensi masuk berhasil, tercatat terlambat.",
  "status": "approved",
  "attendance_status": "present | late",
  "late_minutes": 0,
  "reason_code": null,
  "reason_detail": null,
  "record": {
    "id": 50,
    "date": "2026-05-05",
    "check_in_at": "2026-05-05T06:55:00+07:00",
    "check_out_at": null,
    "status": "present | late",
    "late_minutes": 0,
    "site": { "id": 1, "name": "Gerbang A" }
  }
}
```

#### Response check-out `200 OK` (disetujui)

```json
{
  "message": "Absensi pulang berhasil.",
  "status": "approved",
  "record": {
    "id": 50,
    "date": "2026-05-05",
    "check_in_at": "2026-05-05T06:55:00+07:00",
    "check_out_at": "2026-05-05T14:00:00+07:00",
    "status": "present",
    "late_minutes": 0,
    "site": { "id": 1, "name": "Gerbang A" }
  }
}
```

#### Response `422` (ditolak)

```json
{
  "message": "Absensi ditolak oleh sistem validasi.",
  "status": "rejected",
  "reason_code": "KODE",
  "reason_detail": "…",
  "validation": {}
}
```

Struktur `validation` mengikuti layanan internal (eligibility + bukti). Katalog kode: **`spec/ATTENDANCE_API_SPEC.md`**.

---

### `GET /api/v1/student/daily-attendance/today`

**Auth:** Bearer.

#### Response `200 OK`

Gabungan **status akhir hari ini** + petunjuk UI + daftar titik untuk pemilih.

Root:

| Key | Keterangan |
|-----|------------|
| `data` | Objek satu hari (lihat tabel field di bawah). |
| `attendance_sites` | Array titik aktif (sama bentuk dengan `GET /me`: `id`, `name`, `latitude`, `longitude`, `radius_m`). |

**Field pada `data`** (nilai pasti tergantung kondisi: libur, manual admin, rekaman harian, alpa):

| Field | Tipe | Keterangan |
|-------|------|------------|
| `date` | string `Y-m-d` | Tanggal hari ini (timezone sekolah). |
| `status` | string | Salah satu: `present`, `late`, `excused`, `sick`, `dispensation`, `absent`, `holiday`. |
| `label` | string | Label UI Indonesia (mis. *Hadir*, *Alpa*, *Libur*). |
| `source` | string | `holiday`, `manual_status`, `daily_attendance`, atau `absent`. |
| `check_in_at` | string ISO8601 \| null | Waktu check-in fisik (jika ada). |
| `check_out_at` | string ISO8601 \| null | Waktu check-out fisik (jika ada). |
| `late_minutes` | int \| null | Menit terlambat (jika relevan). |
| `message` | string \| null | Pesan informasi (libur, manual, override, dll.). |
| `site` | `{id,name}` \| null | Titik terkait (isi dapat diganti ke titik default kebijakan efektif untuk UI). |
| `override` | object \| null | Jika ada override hari aktif: `active`, `id`, `name`, `event_type`, `dismiss_students_early`, `waive_check_out`, `allow_check_in`, `allow_check_out`. |
| `can_check_in` | boolean | Hanya pada payload “hari ini” lengkap: apakah klien disarankan menampilkan aksi check-in. |
| `can_check_out` | boolean | Idem untuk check-out. |
| `effective_policy` | object | Jam efektif: `check_in_open_at`, `check_in_on_time_until`, `check_in_close_at`, `check_out_open_at`, `check_out_close_at` (string `H:i` / `H:i:s`). |

---

## 6. Ganti password

### `POST /api/v1/student/change-password`

**Auth:** Bearer.

#### Request body (JSON)

| Field | Wajib | Keterangan |
|--------|--------|------------|
| `current_password` | ya | Harus cocok dengan password akun (`current_password` rule). |
| `password` | ya | Password baru; aturan kompleksitas = **`Password::defaults()`** Laravel. |
| `password_confirmation` | ya | Harus sama dengan `password`. |

#### Response `200 OK`

```json
{
  "message": "Password berhasil diubah."
}
```

Setelah sukses, server mengatur `must_change_password` ke `false`.

#### Response `422`

Validasi gagal (password lemah, konfirmasi tidak cocok, password lama salah).

---

## 7. Ringkasan nama route (Laravel)

| Method | Path | `route()` name |
|--------|------|----------------|
| POST | `/api/v1/student/login` | `api.student.login` |
| POST | `/api/v1/student/logout` | `api.student.logout` |
| GET | `/api/v1/student/me` | `api.student.me` |
| GET | `/api/v1/student/schedule` | `api.student.schedule.index` |
| POST | `/api/v1/student/attendance/check-in` | `api.student.attendance.check-in` |
| POST | `/api/v1/student/attendance/check-out` | `api.student.attendance.check-out` |
| GET | `/api/v1/student/attendance/today` | `api.student.attendance.today` |
| POST | `/api/v1/student/daily-attendance/check-in` | `api.student.daily-attendance.check-in` |
| POST | `/api/v1/student/daily-attendance/check-out` | `api.student.daily-attendance.check-out` |
| GET | `/api/v1/student/daily-attendance/today` | `api.student.daily-attendance.today` |
| POST | `/api/v1/student/change-password` | `api.student.password.change` |

---

## 8. Kode HTTP ringkas

| Situasi | Kode |
|---------|------|
| Sukses | 200 |
| Validasi / login gagal | 422 |
| Belum login / token tidak valid | 401 |
| Bukan role siswa mengakses path siswa | 403 |
| Profil siswa tidak ada (`GET /me`, `GET /schedule`) | 404 |
