package com.gridy.rohmahapp.api

import android.content.Context
// import com.chuckerteam.chucker.api.ChuckerCollector
// import com.chuckerteam.chucker.api.ChuckerInterceptor
// import com.chuckerteam.chucker.api.RetentionManager
import com.gridy.rohmahapp.BuildConfig
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.Utils
import okhttp3.OkHttpClient

import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun create(context: Context): ApiService {
        val pref = PreferenceClass(context)
        val utils = Utils(context)
        val moshi = AppMoshi.build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

//        val chuckerCollector = ChuckerCollector(
//            context = context,
//            showNotification = true,
//            retentionPeriod = RetentionManager.Period.ONE_HOUR
//        )

//        val chuckerInterceptor = ChuckerInterceptor.Builder(context)
//            .collector(chuckerCollector)
//            .maxContentLength(250_000L)
//            .redactHeaders(emptySet())
//            .alwaysReadResponseBody(true)
//            .build()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(RequestInterceptor(utils, pref))
            .addInterceptor(
                ResponseInterceptor(
                    moshi,
                    pref,
                    utils,
                    context.applicationContext as android.app.Application
                )
            )
            .addInterceptor(LogFileInterceptor(utils))
//            .addInterceptor(chuckerInterceptor)
//            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}