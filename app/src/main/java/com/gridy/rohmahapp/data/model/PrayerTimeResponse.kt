package com.gridy.rohmahapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PrayerTimeResponse(
    val data: PrayerTimeData
)

@JsonClass(generateAdapter = true)
data class PrayerTimeData(
    val timings: PrayerTimings
)

@JsonClass(generateAdapter = true)
data class PrayerTimings(
    @Json(name = "Fajr")
    val fajr: String,

    @Json(name = "Sunrise")
    val sunrise: String,

    @Json(name = "Dhuhr")
    val dhuhr: String,

    @Json(name = "Asr")
    val asr: String,

    @Json(name = "Maghrib")
    val maghrib: String,

    @Json(name = "Isha")
    val isha: String
)

@JsonClass(generateAdapter = true)
data class PrayerScheduleUi(
    val fajr: String,
    val sunrise: String,
    val dhuha: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)