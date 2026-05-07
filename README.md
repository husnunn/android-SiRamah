# RohMahApp

Aplikasi Android berbasis Kotlin yang dibangun menggunakan Android Studio dan Gradle.

## Persiapan

Pastikan perangkat pengembangan sudah memiliki:

- Android Studio (versi terbaru disarankan)
- JDK 17 atau sesuai konfigurasi project
- Android SDK dan emulator/perangkat fisik

## Cara Menjalankan

1. Clone repository:

   ```bash
   git clone <url-repository>
   cd RohMahApp
   ```

2. Buka project di Android Studio.
3. Tunggu proses Gradle sync selesai.
4. Jalankan aplikasi ke emulator atau perangkat Android.

## Build APK (Debug)

Melalui terminal project:

```bash
./gradlew assembleDebug
```

Output APK ada di:

`app/build/outputs/apk/debug/`

## Struktur Singkat

- `app/` - Modul utama aplikasi Android
- `gradle/` - Konfigurasi Gradle wrapper
- `build.gradle.kts` / `settings.gradle.kts` - Konfigurasi build project

## Catatan

File lokal seperti `local.properties` dan folder hasil build sudah diabaikan lewat `.gitignore`.
