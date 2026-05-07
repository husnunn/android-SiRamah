# API mobile profile (`/api/mobile`)

Dokumen ini mencerminkan kontrak aktual endpoint profil mobile untuk user login role `student` dan `teacher`.

---

## Konvensi umum

| Item | Nilai |
|------|-------|
| Base URL (relatif) | `/api/mobile` |
| Content-Type | `application/json` |
| Auth | `Authorization: Bearer <plainTextToken>` (Sanctum) |
| Role yang diizinkan | `student`, `teacher` |
| Role lain | `403` + pesan akses ditolak |

Semua response endpoint ini memakai format:

```json
{
  "success": true,
  "message": "string",
  "data": {}
}
```

---

## 1) GET `/api/mobile/profile`

Ambil profil lengkap user login untuk kebutuhan aplikasi Android.

### Response `200 OK` (student)

```json
{
  "success": true,
  "message": "Profile loaded successfully",
  "data": {
    "id": 10,
    "name": "Nama Akun",
    "email": "student@example.com",
    "role": "student",
    "profile_photo_url": "https://example.com/storage/profile-photos/10/avatar.jpg",
    "school": {
      "id": 1,
      "name": "SMK Contoh"
    },
    "class": {
      "id": 5,
      "name": "X RPL 1"
    },
    "user": {
      "id": 10,
      "name": "Nama Akun",
      "username": "siswa01",
      "email": "student@example.com",
      "roles": ["student"],
      "is_active": true,
      "must_change_password": false,
      "last_login_at": "2026-05-06T06:10:00+07:00",
      "email_verified_at": null
    },
    "profile": {
      "id": 77,
      "nis": "00123",
      "nisn": "0123456789",
      "full_name": "Nama Lengkap Siswa",
      "gender": "male",
      "birth_date": "2008-05-05",
      "birth_place": "Mojokerto",
      "phone": "0812xxxx",
      "address": "Alamat inti",
      "parent_name": "Wali Lama",
      "parent_phone": "0813xxxx"
    },
    "extension": {
      "street_address": "Jl. Mawar 1",
      "rt": "001",
      "rw": "002",
      "village": "Trowulan",
      "district": "Trowulan",
      "city": "Kabupaten Mojokerto",
      "province": "Jawa Timur",
      "postal_code": "61362",
      "wilayah_village_id": "3516120009",
      "religion": "Islam",
      "blood_type": "O",
      "profile_photo_url": "https://example.com/storage/student-profile-extensions/77/photo.jpg"
    },
    "parents": [
      {
        "relation": "father",
        "full_name": "Ayah Siswa",
        "occupation": "Wiraswasta",
        "monthly_income_band": "m3_to_8m",
        "nik": "1234567890123456",
        "birth_date": "1980-01-01"
      },
      {
        "relation": "mother",
        "full_name": "Ibu Siswa",
        "occupation": "ASN",
        "monthly_income_band": "m3_to_8m",
        "nik": "1234567890123457",
        "birth_date": "1985-02-02"
      }
    ]
  }
}
```

### Response `200 OK` (teacher)

```json
{
  "success": true,
  "message": "Profile loaded successfully",
  "data": {
    "id": 20,
    "name": "Nama Akun Guru",
    "email": "teacher@example.com",
    "role": "teacher",
    "profile_photo_url": "https://example.com/storage/profile-photos/20/avatar.jpg",
    "school": {
      "id": 1,
      "name": "SMK Contoh"
    },
    "class": null,
    "user": {
      "id": 20,
      "name": "Nama Akun Guru",
      "username": "guru01",
      "email": "teacher@example.com",
      "roles": ["teacher"],
      "is_active": true,
      "must_change_password": false,
      "last_login_at": "2026-05-06T06:10:00+07:00",
      "email_verified_at": null
    },
    "profile": {
      "id": 15,
      "nip": "198001012010011001",
      "full_name": "Nama Lengkap Guru",
      "gender": "female",
      "phone": "0812xxxx",
      "address": "Alamat inti"
    },
    "extension": {
      "birth_date": "1980-01-01",
      "birth_place": "Mojokerto",
      "street_address": "Jl. Melati 1",
      "rt": "001",
      "rw": "002",
      "village": "Trowulan",
      "district": "Trowulan",
      "city": "Kabupaten Mojokerto",
      "province": "Jawa Timur",
      "postal_code": "61362",
      "wilayah_village_id": "3516120009",
      "religion": "Islam",
      "blood_type": "A",
      "profile_photo_url": "https://example.com/storage/teacher-profile-extensions/15/photo.jpg"
    },
    "subjects": [
      {
        "id": 3,
        "code": "MTK",
        "name": "Matematika"
      }
    ],
    "homeroom_classes": [
      {
        "id": 8,
        "name": "9A",
        "level": 9
      }
    ]
  }
}
```

### Response error

- `401`: token invalid / tidak login.
- `403`:

```json
{
  "success": false,
  "message": "Anda tidak memiliki akses untuk mengubah profil."
}
```

- `404`:
    - student tanpa `student_profile`: `"Profil siswa belum dibuat."`
    - teacher tanpa `teacher_profile`: `"Profil guru belum dibuat."`

---

## 2) POST `/api/mobile/profile/photo`

Update foto profil user login (student/teacher).

### Request

`multipart/form-data`:

| Field | Wajib | Keterangan |
|------|------|-----------|
| `photo` | ya | file gambar (`jpg`, `jpeg`, `png`, `webp`), max ukuran sesuai validasi backend aktif |

### Response `200 OK`

```json
{
  "success": true,
  "message": "Foto profil berhasil diperbarui.",
  "data": {
    "profile_photo_url": "https://example.com/storage/profile-photos/20/new-photo.jpg"
  }
}
```

### Response gagal validasi `422`

```json
{
  "success": false,
  "message": "Foto profil tidak valid.",
  "errors": {
    "photo": ["..."]
  }
}
```

---

## 3) POST `/api/mobile/profile/password`

Ganti password akun login.

### Request JSON

```json
{
  "current_password": "password_lama",
  "password": "password_baru",
  "password_confirmation": "password_baru"
}
```

### Response `200 OK`

```json
{
  "success": true,
  "message": "Password berhasil diperbarui."
}
```

Efek samping setelah sukses:
- password user di-hash ulang.
- `plain_password` di-clear (`null`).
- `must_change_password` di-set `false`.

### Response gagal `422`

Contoh jika password lama salah:

```json
{
  "success": false,
  "message": "Password lama tidak sesuai.",
  "errors": {
    "current_password": ["Password lama tidak sesuai."]
  }
}
```

---

## Ringkasan route

| Method | Path | Keterangan |
|------|------|-----------|
| GET | `/api/mobile/profile` | Ambil profil lengkap user login |
| POST | `/api/mobile/profile/photo` | Update foto profil |
| POST | `/api/mobile/profile/password` | Update password |
