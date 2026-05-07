package com.gridy.rohmahapp.repository

import com.gridy.rohmahapp.api.PrayerApiService
import com.gridy.rohmahapp.data.model.PrayerScheduleUi
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PrayerRepository(
    private val prayerApiService: PrayerApiService
) {

    suspend fun getPrayerSchedule(
        latitude: Double,
        longitude: Double
    ): PrayerScheduleUi {
        val response = prayerApiService.getPrayerTimesByCoordinates(
            latitude = latitude,
            longitude = longitude
        )

         val DEFAULT_DHUHA_TIME = "07:00"

        val timings = response.data.timings
//        val dhuha = calculateDhuhaFromSunrise(timings.sunrise)

        return PrayerScheduleUi(
            fajr = cleanTime(timings.fajr),
            sunrise = cleanTime(timings.sunrise),
            dhuha = DEFAULT_DHUHA_TIME,
            dhuhr = cleanTime(timings.dhuhr),
            asr = cleanTime(timings.asr),
            maghrib = cleanTime(timings.maghrib),
            isha = cleanTime(timings.isha)
        )
    }

//    private fun calculateDhuhaFromSunrise(sunrise: String): String {
//        val formatter = DateTimeFormatter.ofPattern("HH:mm")
//        val cleanSunrise = cleanTime(sunrise)
//        val time = LocalTime.parse(cleanSunrise, formatter)
//        return time.plusMinutes(20).format(formatter)
//    }

    private fun cleanTime(value: String): String {
        // API kadang mengembalikan format seperti "04:31 (+07)"
        return value.substringBefore(" ").trim()
    }
}