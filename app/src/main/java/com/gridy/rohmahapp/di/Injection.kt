// File: /app/src/main/java/com/gridy/rohmahapp/di/Injection.kt
package com.gridy.rohmahapp.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.gridy.rohmahapp.api.ApiClient
import com.gridy.rohmahapp.data.local.RohMahAppDatabase
import com.gridy.rohmahapp.data.local.UserLocalCacheCleaner
import com.gridy.rohmahapp.repository.AttendanceRepository
import com.gridy.rohmahapp.repository.AuthRepository
import com.gridy.rohmahapp.repository.DeviceTokenRepository
import com.gridy.rohmahapp.repository.ProfileRepository
import com.gridy.rohmahapp.repository.ScheduleRepository
import com.gridy.rohmahapp.utils.DeviceInfoProvider
import com.gridy.rohmahapp.utils.LocationProvider
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.WifiInfoProvider
import com.gridy.rohmahapp.viewmodel.factory.AttendanceViewModelFactory
import java.util.concurrent.atomic.AtomicReference

object Injection {

    private val databaseRef = AtomicReference<RohMahAppDatabase?>(null)

    fun provideDatabase(context: Context): RohMahAppDatabase {
        val existing = databaseRef.get()
        if (existing != null) return existing
        val created = RohMahAppDatabase.create(context.applicationContext)
        databaseRef.compareAndSet(null, created)
        return databaseRef.get()!!
    }

    fun provideUserLocalCacheCleaner(context: Context): UserLocalCacheCleaner {
        val app = context.applicationContext
        return UserLocalCacheCleaner(
            db = provideDatabase(context),
            pref = PreferenceClass(app),
        )
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        val app = context.applicationContext
        return AuthRepository(
            apiService = ApiClient.create(app),
            pref = PreferenceClass(app),
            userLocalCacheCleaner = provideUserLocalCacheCleaner(context),
            deviceTokenRepository = provideDeviceTokenRepository(context),
        )
    }

    fun provideDeviceTokenRepository(context: Context): DeviceTokenRepository {
        val app = context.applicationContext
        return DeviceTokenRepository(
            apiService = ApiClient.create(app),
            pref = PreferenceClass(app),
            deviceInfoProvider = DeviceInfoProvider(app),
        )
    }

    fun provideProfileRepository(context: Context): ProfileRepository {
        val app = context.applicationContext
        return ProfileRepository(
            apiService = ApiClient.create(app),
            pref = PreferenceClass(app),
            profileDao = provideDatabase(context).profileDao(),
        )
    }

    fun provideScheduleRepository(context: Context): ScheduleRepository {
        val app = context.applicationContext
        return ScheduleRepository(
            apiService = ApiClient.create(app),
            pref = PreferenceClass(app),
            scheduleDao = provideDatabase(context).scheduleDao(),
        )
    }

    fun provideAttendanceViewModelFactory(context: Context): AttendanceViewModelFactory {
        val appContext = context.applicationContext

        val apiService = ApiClient.create(appContext)
        val pref = PreferenceClass(appContext)
        val wifiInfoProvider = WifiInfoProvider(appContext)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
        val locationProvider = LocationProvider(appContext, fusedLocationClient)
        val deviceInfoProvider = DeviceInfoProvider(appContext)

        val repository = AttendanceRepository(
            apiService = apiService,
            pref = pref,
            wifiInfoProvider = wifiInfoProvider,
            locationProvider = locationProvider,
            deviceInfoProvider = deviceInfoProvider,
            attendanceDao = provideDatabase(context).attendanceDao(),
        )

        return AttendanceViewModelFactory(repository)
    }
}
