# Global API Local Cache Requirement

## Tujuan

Saya ingin flow data di aplikasi Rohmah App menggunakan pola local cache dengan Room Database untuk semua endpoint yang datanya perlu disimpan secara lokal.

Konsep utama yang saya inginkan adalah:

```text
Panggil API
↓
Ambil response
↓
Simpan / update ke Room
↓
UI membaca data dari Room
Repository tetap menjadi tempat utama untuk mengatur proses data, yaitu mengambil data dari API lalu menyimpan atau memperbarui data ke Room.


Scope

Requirement ini berlaku untuk semua endpoint yang datanya perlu ditampilkan di UI dan memungkinkan untuk disimpan secara lokal.

Contoh data yang cocok menggunakan pola ini:
- data profile user
- data sekolah
- data kelas
- data mata pelajaran
- data jadwal
- data absensi
- data riwayat
- data konfigurasi aplikasi
- data master / referensi
- data lain yang perlu ditampilkan cepat di UI
Tidak semua endpoint harus masuk Room.

Endpoint yang bersifat aksi langsung seperti submit, create, update, delete, check-in, check-out, upload, atau pembayaran tetap harus diproses online ke server terlebih dahulu.

Alur Utama

Saat aplikasi membutuhkan data dari server, proses yang diinginkan adalah:

Panggil API.
Ambil response dari API.
Simpan atau update response tersebut ke Room Database.
UI tidak bergantung langsung ke response API.
UI mengambil data dari Room sebagai local source of truth.

Dengan pola ini, data tetap bisa ditampilkan dari cache lokal ketika aplikasi tidak sedang melakukan request API atau saat koneksi internet bermasalah.


Trigger Pemanggilan API

Pemanggilan API tidak boleh dilakukan terlalu sering.

API hanya boleh dipanggil pada kondisi berikut:

1. Saat aplikasi pertama kali dibuka

Ketika user membuka aplikasi dan aplikasi mulai berjalan, sistem boleh melakukan request API untuk mengambil data terbaru.

Contoh kondisi:
User membuka aplikasi
↓
Repository memanggil API
↓
Response API disimpan / update ke Room
↓
UI membaca data dari Room


2. Saat aplikasi sudah ditutup lalu dibuka lagi

Jika aplikasi sebelumnya sudah ditutup, lalu user membuka aplikasi kembali, sistem boleh melakukan request API ulang.

Contoh kondisi:
Aplikasi ditutup
↓
User membuka aplikasi lagi
↓
Repository memanggil API
↓
Response terbaru disimpan / update ke Room
↓
UI membaca data terbaru dari Room


3. Saat user melakukan swipe refresh

User dapat mengambil data terbaru secara manual melalui fitur swipe refresh.

Contoh kondisi:
User melakukan swipe refresh
↓
Repository memanggil API
↓
Response API diterima
↓
Data di Room diperbarui
↓
UI otomatis menampilkan data terbaru dari Room

Yang Tidak Diinginkan

Saya tidak ingin API dipanggil setiap kali halaman dibuka atau setiap kali screen onResume() dipanggil.

Contoh yang tidak diinginkan:
User pindah halaman
↓
Balik ke halaman sebelumnya
↓
API dipanggil lagi
Hal seperti ini tidak diinginkan karena bisa membuat request terlalu sering, boros data, dan membuat aplikasi tidak efisien.


Pola yang Diharapkan

Pola yang saya inginkan adalah:
API hanya dipanggil ketika:
- aplikasi pertama kali dibuka
- aplikasi ditutup lalu dibuka kembali
- user melakukan swipe refresh
Selain kondisi tersebut, UI cukup membaca data dari Room.


Alur Data
API
↓
Repository
↓
Room Database
↓
ViewModel
↓
UI

Penjelasan:

API digunakan sebagai sumber data terbaru dari server.
Repository bertanggung jawab memanggil API dan menyimpan hasilnya ke Room.
Room menjadi local source of truth.
ViewModel mengambil data dari Room.
UI hanya menampilkan data dari ViewModel.

Saat API Berhasil

Jika API berhasil dipanggil:
Repository menerima response API
↓
Repository menyimpan / update data ke Room
↓
Room menyimpan data terbaru
↓
UI membaca data terbaru dari Room
UI tidak perlu langsung memakai response API sebagai sumber utama tampilan.

Response API cukup digunakan untuk memperbarui isi Room.

Saat API Gagal

Jika API gagal karena internet mati atau server bermasalah:
Repository gagal mengambil data dari API
↓
Repository tidak menghapus data lama di Room
↓
UI tetap menampilkan data terakhir dari Room
Jika Room belum memiliki data, UI boleh menampilkan pesan bahwa data belum tersedia.

Contoh pesan:
Data belum tersedia. Hubungkan internet untuk memuat data pertama kali.

Prinsip Update Data ke Room

Saat response API diterima, data di Room harus disimpan atau diperbarui.

Pola yang disarankan:
Jika data belum ada:
insert ke Room

Jika data sudah ada:
update data di Room

Jika data lama tidak ada lagi dari server:
hapus atau tandai tidak aktif sesuai kebutuhan data

Untuk data master, sebaiknya gunakan primary key dari server agar proses update lebih aman.

Contoh:
server_id
uuid
code
date
user_id
school_id
Hindari menghapus semua data lalu insert ulang jika tidak diperlukan.
Lebih baik update berdasarkan data yang berubah.


nb : Dan pastikan kalau user logout semua data local di hapus termasuk pref!