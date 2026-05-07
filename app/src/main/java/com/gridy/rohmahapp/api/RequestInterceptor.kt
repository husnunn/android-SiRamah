// File: /app/src/main/java/com/gridy/rohmahapp/api/RequestInterceptor.kt
package com.gridy.rohmahapp.api

import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.Utils
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RequestInterceptor @Inject constructor(
    private val utils: Utils,
    private val pref: PreferenceClass
) : Interceptor {

    private val defaultErrorMsg by lazy {
        "Terjadi gangguan pada koneksi internet Anda, silahkan ulangi beberapa saat lagi"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!utils.isInternetAvailable()) {
            throw ApiException(
                message = defaultErrorMsg,
                responseCode = 0,
                validationErrors = emptyMap(),
            )
        }

        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath
        val token = pref.getString(PreferenceClass.KEY_USER_TOKEN)

        val publicPaths = setOf(
            "/api/v1/student/login",
            "/api/v1/teacher/login"
        )

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")

        val needsAuth = path !in publicPaths

        if (needsAuth && token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}