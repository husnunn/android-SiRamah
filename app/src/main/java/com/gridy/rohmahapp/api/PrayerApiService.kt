package com.gridy.rohmahapp.api

import com.gridy.rohmahapp.data.model.PrayerTimeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerApiService {

    @GET("v1/timings")
    suspend fun getPrayerTimesByCoordinates(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20
    ): PrayerTimeResponse
}