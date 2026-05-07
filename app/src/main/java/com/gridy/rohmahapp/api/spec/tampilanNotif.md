# Task: Rapikan tampilan push notification Android agar terlihat seperti notifikasi Android standar yang rapi

Project Android: `com.gridy.rohmahapp`

Saat ini push notification jadwal sudah berhasil masuk, tetapi tampilannya terasa kurang rapi / kurang seperti notifikasi Android pada umumnya.

## Tujuan

Perbaiki tampilan notification agar:

1. Terlihat seperti notifikasi Android standar yang rapi.
2. Tidak terlihat aneh / terlalu custom.
3. Title dan body mudah dibaca.
4. Small icon tampil dengan benar.
5. Nama aplikasi / label notifikasi lebih proper.
6. Ketika diklik, notifikasi membuka halaman yang sesuai.
7. Tetap mendukung role teacher dan student.

---

## Masalah yang ingin diperbaiki

Saat ini notification terlihat kurang bagus, misalnya:
- small icon terasa kurang pas
- title/body terpotong
- overall tampilannya tidak seperti notifikasi Android standar yang clean
- kemungkinan masih memakai konfigurasi yang kurang tepat

---

## Requirement Perbaikan

### 1. Gunakan NotificationCompat standar
Gunakan `NotificationCompat.Builder` standar.
Jangan gunakan custom notification layout / `RemoteViews` kecuali benar-benar diperlukan.
Prioritaskan tampilan Android default yang clean.

### 2. Gunakan small icon khusus notification
Jangan gunakan launcher icon biasa untuk notification.

Buat icon khusus notification:
- file: `/app/src/main/res/drawable/ic_stat_notification.xml`
- icon harus monochrome / putih / sederhana
- cocok untuk status bar Android

Gunakan:

```kotlin
.setSmallIcon(R.drawable.ic_stat_notification)

3. Gunakan title dan body yang lebih singkat dan natural

Hindari body terlalu panjang.

Contoh title/body yang diinginkan:

Teacher start reminder

Title:
Jadwal Mengajar Akan Dimulai

Body:
Seni Budaya dimulai pukul 10:15.

Teacher end reminder

Title:
Waktu Mengajar Hampir Selesai

Body:
Seni Budaya akan selesai pukul 12:00.

Teacher ended

Title:
Waktu Mengajar Selesai

Body:
Sesi mengajar Seni Budaya telah selesai.

Student first schedule reminder

Title:
Kegiatan Belajar Akan Dimulai

Body:
Pelajaran pertama dimulai pukul 07:00.

Gunakan body pendek agar tidak mudah terpotong.

4. Tetap gunakan BigTextStyle, tapi jangan berlebihan

Gunakan:
.setStyle(NotificationCompat.BigTextStyle().bigText(body))
Agar saat di-expand tetap nyaman dibaca.

5. Gunakan channel notifikasi yang benar

Gunakan channel khusus jadwal:

Channel ID: schedule_reminder_channel
Channel name: Pengingat Jadwal

Importance:

IMPORTANCE_HIGH untuk reminder jadwal
6. Gunakan pengaturan notification yang rapi

Set properti berikut jika relevan:
.setAutoCancel(true)
.setPriority(NotificationCompat.PRIORITY_HIGH)
.setCategory(NotificationCompat.CATEGORY_REMINDER)
.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
.setOnlyAlertOnce(true)

Tambahkan default sound jika dibutuhkan.

7. Perbaiki pending intent saat notif diklik

Saat notification diklik:

teacher notification buka halaman jadwal guru / attendance / dashboard guru yang relevan
student notification buka halaman jadwal siswa / dashboard siswa

Gunakan existing navigation project. Jangan membuat arsitektur navigasi baru jika sudah ada.

8. Rapikan helper notification

Fokus perubahan di file notification helper existing.

Kemungkinan file yang diubah:

/app/src/main/java/com/gridy/rohmahapp/utils/NotificationHelper.kt
/app/src/main/java/com/gridy/rohmahapp/service/RohmahFirebaseMessagingService.kt
/app/src/main/AndroidManifest.xml
/app/src/main/res/drawable/ic_stat_notification.xml

Jika nama file berbeda, sesuaikan dengan struktur project existing.

9. Jangan pakai custom layout notif

Kalau saat ini agent pernah memakai custom notification UI / RemoteViews, hapus dan ganti ke default Android notification style.

10. Pastikan role-specific notification tetap aman
teacher_* hanya tampil untuk role teacher
student_* hanya tampil untuk role student
jika role tidak cocok, abaikan notification
Contoh implementasi yang diharapkan

Gunakan pattern seperti ini:
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_stat_notification)
    .setContentTitle(title)
    .setContentText(body)
    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
    .setAutoCancel(true)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setCategory(NotificationCompat.CATEGORY_REMINDER)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setOnlyAlertOnce(true)
    .setContentIntent(pendingIntent)
    .build()

Catatan Penting

Saya paham bahwa bentuk card notification sebagian dipengaruhi oleh sistem Android / merek HP, jadi saya tidak meminta UI custom.
Saya hanya ingin agar konfigurasi notif dari sisi aplikasi dibuat serapi mungkin supaya hasil akhirnya terlihat seperti notifikasi Android standar yang clean dan profesional.

Output yang Saya Minta Setelah Implementasi

Setelah selesai, jelaskan:

File apa saja yang diubah.
Bagaimana small icon notifikasi diperbaiki.
Bagaimana title/body dibuat lebih singkat.
Bagaimana click action diarahkan.
Apakah custom layout dihapus / tidak digunakan.
Bagaimana hasil akhirnya menjadi lebih mirip notifikasi Android standar.

---