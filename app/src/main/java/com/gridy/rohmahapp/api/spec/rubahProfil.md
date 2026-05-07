
---

## Prompt Agent Android

```md
# Task: Tambahkan Fitur Ubah Foto Profil dan Password untuk Role Student dan Teacher di Android

Kamu bekerja pada project Android Kotlin `com.gridy.rohmahapp`.

## Tujuan

Tambahkan fitur edit profil terbatas untuk user dengan role:

- student
- teacher

User hanya boleh mengubah:

1. Foto profil
2. Password

Field lain seperti nama, email, role, sekolah, kelas, NIS/NIP, nomor HP, alamat, dan data identitas lain hanya ditampilkan sebagai read-only dan tidak bisa diedit.

Jangan membuat ulang sistem login.
Jangan membuat ulang arsitektur project.
Ikuti struktur existing project: ApiService, Repository, ViewModel, UI, model, util, dan pattern yang sudah dipakai.

---

## Scope Android

Tambahkan fitur di halaman profil / setting akun:

1. Menampilkan data profil user login.
2. Mengubah foto profil.
3. Mengubah password.

Jika halaman profil sudah ada, modifikasi halaman existing.
Jika belum ada, buat screen baru sesuai pola project.

---

## API yang Digunakan

Gunakan endpoint backend berikut:

```http
GET /api/mobile/profile
POST /api/mobile/profile/photo
POST /api/mobile/profile/password

Jika endpoint di project existing berbeda, sesuaikan dengan endpoint backend yang tersedia.
Alur Data yang Diinginkan

Gunakan pola repository.
UI
↓
ViewModel
↓
Repository
↓
ApiService
↓
Backend
Untuk data profil, jika project sudah memakai Room local cache, maka flow yang diharapkan:
API profile berhasil
↓
Simpan/update profile ke Room
↓
UI membaca data profile dari Room
Jika local cache untuk profile belum tersedia, implementasikan minimal tanpa merusak flow existing, lalu beri TODO untuk integrasi Room.

1. Tampilkan Profile User

Data yang ditampilkan:
- foto profil
- nama
- email
- role
- sekolah
- kelas jika student
Semua field selain foto dan password harus read-only.

Contoh tampilan:
Foto Profil       [bisa diganti]
Nama              readonly
Email             readonly
Role              readonly
Sekolah           readonly
Kelas             readonly jika student
Button: Ganti Password
Jangan sediakan input untuk nama/email/role/sekolah/kelas.

2. Fitur Ganti Foto Profil

User dapat mengganti foto profil dari:

Gallery
Camera jika project sudah punya util camera existing

Jika project sudah punya util image picker / permission / FileProvider existing, gunakan yang sudah ada.

Flow:
User klik foto profil / tombol ganti foto
↓
Pilih gambar dari gallery atau camera
↓
Preview gambar di UI
↓
Upload gambar ke API
↓
Jika sukses, update tampilan foto profil
↓
Jika project memakai Room, update cache profile lokal
Request:
photo: file
Validasi Android sebelum upload:

file tidak boleh null
file harus image
ukuran maksimal mengikuti backend, misalnya 2MB
jika file terlalu besar, tampilkan pesan error
jika permission belum diberikan, minta permission sesuai standar Android

Response sukses:
{
  "success": true,
  "message": "Foto profil berhasil diperbarui.",
  "data": {
    "profile_photo_url": "https://example.com/storage/profile/photo.jpg"
  }
}
Setelah sukses:

tampilkan toast/dialog sukses
refresh profile dari API atau update local profile cache
tampilkan foto terbaru

3. Fitur Ganti Password

Buat form ganti password berisi:
Password Lama
Password Baru
Konfirmasi Password Baru
Button Simpan
Validasi di Android:

Password lama wajib diisi.
Password baru wajib diisi.
Konfirmasi password wajib diisi.
Password baru minimal 8 karakter.
Password baru dan konfirmasi harus sama.

Request JSON:
{
  "current_password": "password_lama",
  "password": "password_baru",
  "password_confirmation": "password_baru"
}
Flow:
User buka form ganti password
↓
User input password lama dan password baru
↓
Android validasi input
↓
Kirim request ke backend
↓
Jika sukses, tampilkan pesan berhasil
↓
Kosongkan form
↓
Tutup dialog / kembali ke halaman profil
Jika gagal:

tampilkan message dari backend
jika ada error per-field, tampilkan pada input terkait

Contoh error:
{
  "success": false,
  "message": "Password lama tidak sesuai.",
  "errors": {
    "current_password": [
      "Password lama tidak sesuai."
    ]
  }
}

File / Komponen yang Mungkin Dibuat atau Diubah

Sesuaikan dengan struktur project existing.

Contoh file:
/app/src/main/java/com/gridy/rohmahapp/api/ApiService.kt
/app/src/main/java/com/gridy/rohmahapp/repository/ProfileRepository.kt
/app/src/main/java/com/gridy/rohmahapp/data/model/ProfileResponse.kt
/app/src/main/java/com/gridy/rohmahapp/data/model/UpdatePasswordRequest.kt
/app/src/main/java/com/gridy/rohmahapp/ui/profile/ProfileViewModel.kt
/app/src/main/java/com/gridy/rohmahapp/ui/profile/ProfileFragment.kt
/app/src/main/res/layout/fragment_profile.xml
Jika project sudah memiliki repository/profile/settings page, gunakan yang existing.
Jangan membuat duplikasi screen jika sudah ada.

ApiService

Tambahkan endpoint Retrofit jika belum ada:
@GET("api/mobile/profile")
suspend fun getProfile(): ProfileResponse

@Multipart
@POST("api/mobile/profile/photo")
suspend fun updateProfilePhoto(
    @Part photo: MultipartBody.Part
): ProfilePhotoResponse

@POST("api/mobile/profile/password")
suspend fun updateProfilePassword(
    @Body request: UpdatePasswordRequest
): BasicResponse
Sesuaikan base path dengan project existing.

Model Request Password

Buat model:
data class UpdatePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String
)
Ikuti naming JSON backend. Jika project menggunakan Gson tanpa SerializedName, gunakan snake_case seperti di atas. Jika project menggunakan Moshi/Gson dengan annotation, sesuaikan pattern existing.

Repository

Buat atau update repository profile.

Tanggung jawab repository:

Ambil profile dari API.
Upload foto profil.
Update password.
Jika local cache tersedia, simpan/update profile ke Room setelah API berhasil.
Jangan menaruh logic UI di repository.

Contoh flow:
ProfileRepository.getProfile()
↓
apiService.getProfile()
↓
simpan/update Room jika tersedia
↓
return result ke ViewModel
ViewModel

ViewModel bertanggung jawab:

Load profile.
Handle loading state.
Handle error state.
Handle upload foto.
Handle update password.
Memberikan state ke UI.

State minimal:
loading
profile data
success message
error message
field errors jika ada
Jangan panggil ApiService langsung dari Fragment/Activity.
Fragment/Activity harus lewat ViewModel.

UI Requirement

Halaman profile harus:

Menampilkan foto profile.
Menampilkan data user read-only.
Menyediakan tombol ganti foto.
Menyediakan tombol ganti password.
Menampilkan loading saat request berjalan.
Menampilkan error jika request gagal.
Menampilkan pesan sukses jika update berhasil.

Field read-only tidak boleh bisa diketik.
Permission dan File Handling

Untuk upload foto:

Gunakan Activity Result API jika project sudah memakai pendekatan modern.
Gunakan permission sesuai versi Android.
Untuk Android 13+, gunakan permission media yang sesuai jika diperlukan.
Untuk camera, gunakan FileProvider jika project sudah memiliki config.
Convert Uri ke MultipartBody.Part dengan cara aman.
Jangan memakai real path yang rawan null dari Uri.
Gunakan ContentResolver untuk membaca file dari Uri.

Jika project sudah punya util untuk upload file, gunakan util existing.
Error Handling

Handle error berikut:

Tidak ada internet.
Server error.
Unauthorized/token expired.
File terlalu besar.
Format file tidak valid.
Password lama salah.
Password confirmation tidak sama.
Response backend gagal.

Tampilkan message yang mudah dipahami user.
Batasan Penting

Jangan lakukan hal berikut:

Jangan membuat fitur edit nama.
Jangan membuat fitur edit email.
Jangan membuat fitur edit role.
Jangan membuat fitur edit sekolah.
Jangan membuat fitur edit kelas.
Jangan mengubah flow login.
Jangan menyimpan password di local storage.
Jangan menampilkan password di log.
Jangan upload file tanpa validasi.
Jangan panggil API langsung dari UI.
Jangan membuat repository baru jika sudah ada repository profile/settings existing.

Testing Manual Android

Test sebagai student:

Login sebagai student.
Buka halaman profil.
Pastikan data tampil.
Pastikan nama/email/sekolah/kelas readonly.
Ganti foto profil dari gallery.
Pastikan foto berubah.
Ganti password dengan password lama benar.
Pastikan berhasil.
Login ulang dengan password baru.

Test sebagai teacher:

Login sebagai teacher.
Buka halaman profil.
Pastikan data tampil.
Pastikan field identitas readonly.
Ganti foto profil.
Ganti password.

Negative case:

Password lama salah.
Password baru kurang dari 8 karakter.
Password confirmation tidak sama.
Upload file bukan image.
Upload file terlalu besar.
Internet mati saat upload.
Token expired.

Expected Result

Setelah implementasi Android selesai:

Student bisa mengubah foto profil.
Student bisa mengubah password.
Teacher bisa mengubah foto profil.
Teacher bisa mengubah password.
Field lain hanya read-only.
Tidak ada field identitas yang bisa diedit dari Android.
Request dilakukan melalui Repository dan ViewModel.
UI menampilkan loading, success, dan error dengan jelas.
Build project berhasil.