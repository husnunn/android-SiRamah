package com.gridy.rohmahapp.di.module

import android.content.Context
import com.gridy.rohmahapp.BuildConfig
import com.gridy.rohmahapp.api.AppMoshi
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.api.LogFileInterceptor
import com.gridy.rohmahapp.api.RequestInterceptor
import com.gridy.rohmahapp.api.ResponseInterceptor
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.Utils
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return AppMoshi.build()
    }

    @Provides
    @Singleton
    fun provideRequestInterceptor(
        utils: Utils,
        pref: PreferenceClass
    ): RequestInterceptor {
        return RequestInterceptor(utils, pref)
    }

    @Provides
    @Singleton
    fun provideResponseInterceptor(
        moshi: Moshi,
        pref: PreferenceClass,
        utils: Utils,
        context: Context
    ): ResponseInterceptor {
        return ResponseInterceptor(
            moshi,
            pref,
            utils,
            context.applicationContext as android.app.Application
        )
    }

    @Provides
    @Singleton
    fun provideLogFileInterceptor(
        utils: Utils
    ): LogFileInterceptor {
        return LogFileInterceptor(utils)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        requestInterceptor: RequestInterceptor,
        responseInterceptor: ResponseInterceptor,
        logFileInterceptor: LogFileInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(requestInterceptor)
            .addInterceptor(responseInterceptor)
            .addInterceptor(logFileInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(
        retrofit: Retrofit
    ): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}