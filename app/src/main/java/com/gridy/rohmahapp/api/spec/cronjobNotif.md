# Task: Implement Android Push Notification untuk Jadwal Guru dan Siswa

Kamu bekerja pada project Android Kotlin `com.gridy.rohmahapp`.

Backend Laravel sudah / akan membuat cron job untuk mengirim notifikasi Android berdasarkan jadwal pelajaran. Sekarang Android harus siap menerima dan menampilkan notifikasi tersebut.

## Tujuan

Tambahkan / rapikan fitur push notification Android agar aplikasi bisa menerima notifikasi dari backend untuk:

1. Guru H-15 menit sebelum mulai mengajar.
2. Guru H-15 menit sebelum jadwal mengajar selesai.
3. Guru saat waktu mengajar sudah selesai.
4. Siswa H-15 menit sebelum jam pelajaran pertama dimulai.

Jangan membuat ulang arsitektur aplikasi.
Jangan merusak fitur login existing.
Jangan membuat ulang Retrofit / Repository jika sudah ada.
Gunakan struktur existing project: `ApiService`, `Repository`, `ViewModel`, `PreferenceClass`, `NotificationHelper`, `FirebaseMessagingService`, dan util yang sudah ada.

---

## Event Notification dari Backend

Backend akan mengirim payload FCM dengan field `type`.

Tipe notifikasi yang harus didukung Android:

```text
teacher_schedule_start_reminder
teacher_schedule_end_reminder
teacher_schedule_ended
student_first_schedule_reminder
```

Contoh payload guru akan mengajar:

```json
{
  "type": "teacher_schedule_start_reminder",
  "title": "Jadwal Mengajar Akan Dimulai",
  "body": "Anda akan mengajar PJOK pukul 07:00 - 09:15.",
  "schedule_id": "123",
  "subject_name": "PJOK",
  "start_time": "07:00",
  "end_time": "09:15"
}
```

Contoh payload guru akan selesai mengajar:

```json
{
  "type": "teacher_schedule_end_reminder",
  "title": "Waktu Mengajar Hampir Selesai",
  "body": "Mata pelajaran PJOK akan selesai pukul 09:15.",
  "schedule_id": "123",
  "subject_name": "PJOK",
  "start_time": "07:00",
  "end_time": "09:15"
}
```

Contoh payload guru selesai mengajar:

```json
{
  "type": "teacher_schedule_ended",
  "title": "Waktu Mengajar Selesai",
  "body": "Waktu mengajar PJOK telah selesai.",
  "schedule_id": "123",
  "subject_name": "PJOK",
  "start_time": "07:00",
  "end_time": "09:15"
}
```

Contoh payload siswa:

```json
{
  "type": "student_first_schedule_reminder",
  "title": "Kegiatan Belajar Akan Dimulai",
  "body": "Kegiatan belajar hari ini dimulai pukul 07:00. Persiapkan diri Anda.",
  "class_id": "10",
  "first_schedule_id": "123",
  "start_time": "07:00"
}
```

---

## Scope Android

Implementasikan:

1. Firebase Messaging Service untuk menerima FCM.
2. Notification Channel untuk Android 8+.
3. Tampilkan notifikasi di status bar.
4. Handle foreground dan background notification.
5. Handle klik notifikasi agar membuka halaman yang sesuai.
6. Simpan / update FCM token ke backend setelah login.
7. Refresh token jika Firebase memberikan token baru.
8. Pastikan notifikasi hanya relevan untuk user login saat ini.

---

## File yang Mungkin Dibuat / Diubah

Sesuaikan dengan struktur project existing.

Contoh file:

```text
/app/src/main/java/com/gridy/rohmahapp/service/RohmahFirebaseMessagingService.kt
/app/src/main/java/com/gridy/rohmahapp/utils/NotificationHelper.kt
/app/src/main/java/com/gridy/rohmahapp/api/ApiService.kt
/app/src/main/java/com/gridy/rohmahapp/repository/DeviceTokenRepository.kt
/app/src/main/java/com/gridy/rohmahapp/data/model/RegisterDeviceTokenRequest.kt
/app/src/main/java/com/gridy/rohmahapp/data/model/BasicResponse.kt
/app/src/main/AndroidManifest.xml
```

Jika project sudah punya `FirebaseMessagingService` atau `NotificationHelper`, gunakan dan modifikasi file existing. Jangan membuat duplikasi.

---

## 1. Firebase Messaging Service

Buat atau update service FCM.

Contoh:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/service/RohmahFirebaseMessagingService.kt

package com.gridy.rohmahapp.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gridy.rohmahapp.utils.NotificationHelper
import com.gridy.rohmahapp.utils.PreferenceClass
import timber.log.Timber

class RohmahFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data

        val type = data["type"].orEmpty()
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "Rohmah App"

        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: "Anda memiliki notifikasi baru."

        if (type.isBlank()) {
            Timber.w("FCM ignored: type kosong")
            return
        }

        NotificationHelper.showScheduleNotification(
            context = this,
            type = type,
            title = title,
            body = body,
            data = data
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Simpan token sementara ke PreferenceClass.
        // Jika user sudah login dan access token tersedia, kirim token ke backend.
        // Jangan mengirim token tanpa authentication jika endpoint membutuhkan Bearer token.

        val pref = PreferenceClass(this)
        pref.putString(PreferenceClass.KEY_FCM_TOKEN, token)

        Timber.d("FCM token refreshed")
    }
}
```

Catatan:

- Jika sudah ada service FCM, cukup tambahkan handling `type` baru.
- Jangan menampilkan token utuh di log production.
- Jangan menampilkan data sensitif di log.
- Jangan crash jika payload tidak lengkap.

---

## 2. Register Service di AndroidManifest

Pastikan service FCM terdaftar.

```xml
<!-- File: /app/src/main/AndroidManifest.xml -->

<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application>
    <service
        android:name=".service.RohmahFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
</application>
```

Jika service FCM sudah ada di manifest, jangan buat duplikasi.

---

## 3. Notification Channel

Buat channel khusus jadwal pelajaran.

Channel ID:

```text
schedule_reminder_channel
```

Channel name:

```text
Pengingat Jadwal
```

Contoh helper:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/utils/NotificationHelper.kt

package com.gridy.rohmahapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gridy.rohmahapp.MainActivity
import com.gridy.rohmahapp.R

object NotificationHelper {

    private const val CHANNEL_ID = "schedule_reminder_channel"
    private const val CHANNEL_NAME = "Pengingat Jadwal"

    fun showScheduleNotification(
        context: Context,
        type: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        if (!isNotificationAllowed(context)) return

        createScheduleChannel(context)

        val intent = buildNotificationIntent(context, type, data)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(type, data),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            notificationId(type, data),
            notification
        )
    }

    private fun createScheduleChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi pengingat jadwal pelajaran"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotificationIntent(
        context: Context,
        type: String,
        data: Map<String, String>
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)

            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }

    private fun notificationId(type: String, data: Map<String, String>): Int {
        val base = when (type) {
            "teacher_schedule_start_reminder" ->
                "teacher_start_${data["schedule_id"]}"

            "teacher_schedule_end_reminder" ->
                "teacher_end_reminder_${data["schedule_id"]}"

            "teacher_schedule_ended" ->
                "teacher_ended_${data["schedule_id"]}"

            "student_first_schedule_reminder" ->
                "student_first_${data["class_id"]}_${data["first_schedule_id"]}_${data["start_time"]}"

            else ->
                "general_${System.currentTimeMillis()}"
        }

        return base.hashCode()
    }

    private fun isNotificationAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

Sesuaikan `MainActivity` dengan activity utama project.

Jika project sudah punya `NotificationHelper`, cukup tambahkan channel dan fungsi untuk tipe schedule reminder.

---

## 4. Handling Klik Notifikasi

Saat user klik notifikasi:

### Untuk guru

Jika `type`:

```text
teacher_schedule_start_reminder
teacher_schedule_end_reminder
teacher_schedule_ended
```

Arahkan ke halaman jadwal guru atau halaman attendance / schedule yang relevan.

Contoh tujuan:

```text
TeacherScheduleFragment
```

atau halaman existing yang menampilkan jadwal hari ini.

### Untuk siswa

Jika `type`:

```text
student_first_schedule_reminder
```

Arahkan ke halaman jadwal siswa / dashboard siswa.

Contoh tujuan:

```text
StudentScheduleFragment
```

Jika project belum punya deep link / navigation handler, cukup buka `MainActivity`, lalu biarkan dashboard tampil. Tambahkan TODO untuk navigasi spesifik.

---

## 5. Handle Intent di MainActivity

Jika project memakai single activity, tangani extra dari notifikasi.

Contoh:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/MainActivity.kt

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
}

private fun handleNotificationIntent(intent: Intent?) {
    val type = intent?.getStringExtra("notification_type") ?: return

    when (type) {
        "teacher_schedule_start_reminder",
        "teacher_schedule_end_reminder",
        "teacher_schedule_ended" -> {
            // TODO: navigate to teacher schedule / attendance page existing
        }

        "student_first_schedule_reminder" -> {
            // TODO: navigate to student schedule / dashboard existing
        }
    }
}
```

Jika project memakai Navigation Component, gunakan navController.
Jika project memakai manual Fragment transaction, ikuti pola existing.
Jangan membuat sistem navigasi baru jika project sudah punya pola sendiri.

---

## 6. Register FCM Token ke Backend

Setelah user login berhasil, Android harus mengambil FCM token dan mengirim ke backend.

Endpoint yang disarankan:

```http
POST /api/mobile/device-token
```

Request:

```json
{
  "token": "firebase_token",
  "platform": "android",
  "device_name": "Samsung A52",
  "app_version": "1.0.0",
  "os_version": "Android 14"
}
```

Jika backend sudah punya endpoint device token existing, gunakan endpoint existing.

Tambahkan ApiService jika belum ada:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/api/ApiService.kt

@POST("api/mobile/device-token")
suspend fun registerDeviceToken(
    @Body request: RegisterDeviceTokenRequest
): BasicResponse
```

Model request:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/data/model/RegisterDeviceTokenRequest.kt

data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "android",
    val device_name: String?,
    val app_version: String?,
    val os_version: String?
)
```

Repository:

```kotlin
// File: /app/src/main/java/com/gridy/rohmahapp/repository/DeviceTokenRepository.kt

package com.gridy.rohmahapp.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.data.model.RegisterDeviceTokenRequest
import com.gridy.rohmahapp.utils.DeviceInfoProvider
import com.gridy.rohmahapp.utils.PreferenceClass
import kotlinx.coroutines.tasks.await

class DeviceTokenRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    suspend fun registerCurrentToken() {
        val token = FirebaseMessaging.getInstance().token.await()

        pref.putString(PreferenceClass.KEY_FCM_TOKEN, token)

        apiService.registerDeviceToken(
            RegisterDeviceTokenRequest(
                token = token,
                platform = "android",
                device_name = deviceInfoProvider.getDeviceName(),
                app_version = deviceInfoProvider.getAppVersion(),
                os_version = deviceInfoProvider.getOsVersion()
            )
        )
    }
}
```

Jika project belum menggunakan coroutine `await()`, gunakan listener biasa atau tambahkan dependency yang sesuai:

```gradle
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:<version>"
```

Sesuaikan versi dengan dependency project.

---

## 7. Kapan Register FCM Token Dipanggil

Panggil register token setelah login sukses.

Flow:

```text
User login berhasil
↓
Simpan access token / session seperti flow existing
↓
Ambil FCM token
↓
Kirim FCM token ke backend
↓
Masuk dashboard
```

Jika token gagal dikirim:
- Jangan gagalkan login.
- Simpan error log secukupnya.
- Coba kirim ulang saat app dibuka / dashboard dimuat.

---

## 8. onNewToken Behavior

Saat `onNewToken(token)` terpanggil:

1. Simpan token ke Preference.
2. Jika user sedang login dan access token tersedia, kirim ke backend.
3. Jika user belum login, cukup simpan token.
4. Setelah login sukses, kirim token yang tersimpan.

Jangan mengirim token tanpa authentication jika endpoint membutuhkan Bearer token.

---

## 9. Permission Android 13+

Untuk Android 13 ke atas, minta permission:

```text
POST_NOTIFICATIONS
```

Flow:

```text
User login / masuk dashboard
↓
Cek permission notifikasi
↓
Jika belum diberikan, minta permission
↓
Jika diberikan, register FCM token ke backend
```

Jika user menolak permission, aplikasi tetap jalan. Tampilkan pesan ringan jika diperlukan.

Gunakan Activity Result API jika project sudah memakai pattern modern.

---

## 10. Role Guard di Android

Backend seharusnya sudah mengirim notifikasi ke role yang sesuai.

Namun Android tetap harus aman:

- `teacher_*` hanya untuk role `teacher`.
- `student_*` hanya untuk role `student`.
- Jika role tidak sesuai, abaikan notifikasi.
- Jika user belum login, abaikan notifikasi role-specific.
- Jangan crash jika role kosong.

Gunakan role dari `PreferenceClass.KEY_USER_ROLE` jika tersedia.

Contoh logic:

```kotlin
private fun isAllowedForCurrentRole(type: String, role: String?): Boolean {
    return when {
        type.startsWith("teacher_") -> role == "teacher"
        type.startsWith("student_") -> role == "student"
        else -> true
    }
}
```

Tambahkan pengecekan ini sebelum `showScheduleNotification()` jika project menyimpan role user secara lokal.

---

## 11. Foreground dan Background

Pastikan notifikasi tetap muncul saat:

1. App foreground.
2. App background.
3. App killed, jika FCM mengirim notification/data payload yang sesuai.

Untuk kontrol penuh, backend sebaiknya mengirim `data payload`.

Android harus membaca:

```kotlin
remoteMessage.data
```

Jika backend mengirim `notification payload`, pastikan title/body tetap bisa terbaca.

---

## 12. Error Handling

Handle kondisi berikut:

1. Payload type kosong.
2. Title/body kosong.
3. Permission notifikasi belum diberikan.
4. User belum login.
5. Token FCM gagal didapatkan.
6. Token gagal dikirim ke backend.
7. Backend token endpoint unauthorized.
8. User logout tetapi token masih tersimpan.
9. App sedang foreground.
10. App sedang background.

Saat logout:
- Jika backend mendukung unregister token, panggil endpoint unregister.
- Jika belum ada, hapus session lokal saja dan tambahkan TODO backend.

Jangan menyimpan password / data sensitif di notification payload.

---

## 13. Testing Manual

### Test Token

1. Login ke aplikasi.
2. Pastikan FCM token berhasil didapat.
3. Pastikan token terkirim ke backend.
4. Cek database backend apakah token tersimpan untuk user login.

### Test Guru

Kirim test FCM dengan payload:

```json
{
  "type": "teacher_schedule_start_reminder",
  "title": "Jadwal Mengajar Akan Dimulai",
  "body": "Anda akan mengajar PJOK pukul 07:00 - 09:15.",
  "schedule_id": "123",
  "subject_name": "PJOK",
  "start_time": "07:00",
  "end_time": "09:15"
}
```

Expected:

```text
Notifikasi muncul di Android guru.
Klik notifikasi membuka aplikasi.
```

### Test Siswa

Kirim test FCM dengan payload:

```json
{
  "type": "student_first_schedule_reminder",
  "title": "Kegiatan Belajar Akan Dimulai",
  "body": "Kegiatan belajar hari ini dimulai pukul 07:00. Persiapkan diri Anda.",
  "class_id": "10",
  "first_schedule_id": "123",
  "start_time": "07:00"
}
```

Expected:

```text
Notifikasi muncul di Android siswa.
Klik notifikasi membuka aplikasi.
```

### Test Role Mismatch

1. Login sebagai student.
2. Kirim payload `teacher_schedule_start_reminder`.
3. Pastikan notifikasi diabaikan atau tidak tampil.

1. Login sebagai teacher.
2. Kirim payload `student_first_schedule_reminder`.
3. Pastikan notifikasi diabaikan atau tidak tampil.

### Test Android 13+

1. Install app di Android 13+.
2. Pastikan permission notifikasi diminta.
3. Jika permission diberikan, notifikasi muncul.
4. Jika permission ditolak, app tidak crash.

---

## 14. Expected Result

Setelah implementasi selesai:

1. Android bisa menerima notifikasi jadwal dari backend.
2. Guru menerima notifikasi:
    - H-15 menit sebelum mulai mengajar.
    - H-15 menit sebelum selesai mengajar.
    - Saat waktu mengajar selesai.
3. Siswa menerima notifikasi H-15 menit sebelum jam pertama dimulai.
4. Notifikasi hanya relevan untuk role yang sesuai.
5. Token FCM dikirim ke backend setelah login.
6. Token baru dari Firebase disimpan dan dikirim ulang ke backend.
7. Notifikasi muncul saat foreground/background.
8. Klik notifikasi membuka aplikasi.
9. Build project berhasil.

---

## 15. Output yang Harus Dijelaskan Setelah Implementasi

Setelah selesai, jelaskan:

1. File apa saja yang dibuat/diubah.
2. Cara FCM token dikirim ke backend.
3. Cara notification channel dibuat.
4. Cara handling payload `type`.
5. Cara navigasi saat notification diklik.
6. Cara menangani Android 13 notification permission.
7. Cara test manual foreground/background.
8. TODO lanjutan jika ada.
