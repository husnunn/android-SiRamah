# Teacher schedule API — mobile (guru)

Base path: `/api/v1/teacher`  
Auth: `Authorization: Bearer <sanctum_token>` (user harus punya role `teacher`).

Semua waktu jadwal di database adalah jam dinding; tanggal kalender opsional memakai timezone sekolah (sama dengan endpoint jadwal lain di aplikasi ini).

---

## Daftar endpoint terkait jadwal

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/schedule` | Daftar flat semua slot jadwal guru (legacy / daftar penuh) |
| GET | `/schedule/by-day` | **Baru** — hanya hari yang ada jadwal, dikelompokkan per hari (untuk UI tab/filter Android) |

---

## GET `/api/v1/teacher/schedule/by-day`

Mengembalikan **hanya hari Senin–Sabtu yang punya minimal satu jadwal aktif** untuk guru yang login. Contoh: jika guru hanya mengajar Senin dan Jumat, array `data` berisi **2** elemen (Senin dan Jumat), bukan 6 hari kosong.

### Query string (opsional)

| Parameter | Tipe | Deskripsi |
|-----------|------|-----------|
| `semester` | integer | Filter semester (`1` atau `2`). Jika tidak diisi, semua semester pada tahun ajaran aktif tetap dipertimbangkan. |
| `date` | `YYYY-MM-DD` | Jika tanggal ini jatuh pada **event akademik** yang menutup jadwal / override jadwal (aturan sama dengan `GET /schedule`), respons berisi `data: []`. |

**Tidak** ada parameter `day` — pemfilteran per hari tidak dipakai di endpoint ini karena tujuan endpoint justru mengembalikan **per beda hari**.

### Response 200

Root berisi **`data`**: array objek, **urut naik menurut `day_of_week`** (1 = Senin … 6 = Sabtu).

Setiap objek:

| Field | Tipe | Deskripsi |
|-------|------|-----------|
| `day_of_week` | integer | 1–6 (Minggu tidak dipakai di sistem jadwal ini) |
| `day_name` | string | Label Indonesia: `Senin`, `Selasa`, … |
| `schedules` | array | Slot jadwal hari itu, **urut `start_time` naik**. Bentuk tiap elemen **sama** dengan resource pada `GET /schedule` (`id`, `day`, `day_of_week`, `start_time`, `end_time`, `room`, `semester`, `notes`, `subject`, `class`, `school_year`). Field `teacher` tidak diisi pada resource internal ini (relasi tidak diload). |

#### Contoh (guru mengajar Senin & Jumat saja)

```json
{
  "data": [
    {
      "day_of_week": 1,
      "day_name": "Senin",
      "schedules": [
        {
          "id": 12,
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
          "school_year": "2025/2026"
        }
      ]
    },
    {
      "day_of_week": 5,
      "day_name": "Jumat",
      "schedules": [
        {
          "id": 44,
          "day": "Jumat",
          "day_of_week": 5,
          "start_time": "10:00",
          "end_time": "11:00",
          "room": "Ruang 202",
          "semester": 1,
          "notes": null,
          "subject": { "id": 1, "code": "BIND", "name": "Bahasa Indonesia" },
          "class": { "id": 5, "name": "X IPA 1", "level": "10" },
          "school_year": "2025/2026"
        }
      ]
    }
  ]
}
```

### Response lain

- **401** — tidak terautentikasi
- **403** — token bukan role guru
- **404** — user guru **tanpa** `teacher_profile`

---

## GET `/api/v1/teacher/schedule` (referensi cepat)

Tetap tersedia; mengembalikan **`data`** sebagai array datar semua slot (bukan dikelompokkan per hari). Mendukung query `semester`, `day` (filter `day_of_week`), dan `date` (pengecualian event akademik) seperti sebelum refactor.

Perbedaan utama untuk app Android:

- **`/schedule`** — satu daftar panjang; klien harus mengelompokkan sendiri per hari.
- **`/schedule/by-day`** — **hanya hari yang relevan**, siap untuk tab / chip hari (`data.length` = jumlah hari mengajar).

---

## Konvensi umum

- Content-Type: `application/json`
- Nama route Laravel: `api.teacher.schedule.by-day`
