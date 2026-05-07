// File: /app/build.gradle.kts
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}

val localPropertiesFile = rootProject.file("local.properties")
val rawLocalProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}
val mapsApiKey =
    (rawLocalProperties.getProperty("MAPS_API_KEY", "") ?: "").removeSurrounding("\"")

if (mapsApiKey.isBlank()) {
    logger.warn(
        "MAPS_API_KEY kosong di local.properties — placeholder manifest akan kosong dan " +
            "Maps biasanya gagal dengan pale blank tiles. Isi MAPS_API_KEY dan daftarkan " +
            "package com.gridy.rohmahapp dengan SHA dari ./gradlew :app:signingReport untuk debug/release."
    )
}

android {
    namespace = "com.gridy.rohmahapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gridy.rohmahapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            val apiUrlProd =
                (rawLocalProperties.getProperty("API_URL_PROD", "") ?: "").removeSurrounding("\"")

            buildConfigField("String", "API_URL", "\"$apiUrlProd\"")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            val apiUrlDev =
                (rawLocalProperties.getProperty("API_URL_DEV", "") ?: "").removeSurrounding("\"")
            buildConfigField("String", "API_URL", "\"$apiUrlDev\"")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // --- Jetpack Compose + pendukung: core KTX, Lifecycle; BOM menyamakan versi lib Compose; Material3; preview IDE ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Google Play In-App Updates (cek/pembaruan APK dari Play) ---
    implementation(libs.app.update.ktx)
    // --- UI klasik XML: AppCompat, Material Components, aktivitas ConstraintLayout ---
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // --- Navigasi antar Fragment/Activity dengan graph & destination ---
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // --- Uji unit & instrumentasi; Compose BOM + tes UI untuk layar Compose ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- Glide + KAPT annotation processor — muat & cache gambar (URL, dll.) ---
    implementation(libs.glide)
    kapt(libs.compiler)

    // --- HTTP: OkHttp (klien) + interceptor logging untuk debug ---
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // --- Retrofit (REST) + Moshi JSON; kapt codegen adapter Moshi ---
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // --- Timber (log structured); Gson (pars JSON legacy/cache); Dagger DI + codegen ---
    implementation(libs.timber)
    implementation(libs.gson)
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    // --- Chucker: dinonaktifkan (notifikasi / UI inspektur HTTP tidak ditampilkan) ---
    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // --- Play services: lokasi (GPS fused), Maps (peta absensi), OAuth Google Sign-In ---
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.auth)
    // --- Credential/account helper terkait identitas Google (pendukung login) ---
    implementation(libs.play.services.identity)

    // --- MultiDex — melewati limit 65K method ketika kombinasi lib besar ---
    implementation(libs.androidx.multidex)

    // --- Firebase (BOM = versi sejajar): Firestore backend; FCM push notification ---
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    // --- Room — database lokal SQLite + codegen SQL/Kotlin ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // --- Dexter — permintaan permission runtime lebih ringkas; WorkManager — tugas latar ---
    implementation(libs.dexter)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)

    // --- Lottie animasi JSON; RecyclerView list; pull-to-refresh ---
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- SDP/SSP Intuit — dimensi/sp teks scalable per lebar layar ---
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)
}