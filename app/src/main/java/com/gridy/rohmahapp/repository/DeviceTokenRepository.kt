package com.gridy.rohmahapp.repository

import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.data.model.RegisterDeviceTokenRequest
import com.gridy.rohmahapp.utils.DeviceInfoProvider
import com.gridy.rohmahapp.utils.PreferenceClass
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class DeviceTokenRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val deviceInfoProvider: DeviceInfoProvider,
) {
    suspend fun registerCurrentTokenIfLoggedIn() {
        if (pref.getString(PreferenceClass.KEY_USER_TOKEN).isBlank()) return

        runCatching {
            val fcmToken = awaitFcmToken()
            pref.putString(PreferenceClass.KEY_FCM_TOKEN, fcmToken)
            registerTokenToBackend(fcmToken)
        }.onFailure {
            Timber.w(it, "Gagal register current FCM token")
        }
    }

    suspend fun registerStoredOrProvidedTokenIfLoggedIn(providedToken: String? = null) {
        if (pref.getString(PreferenceClass.KEY_USER_TOKEN).isBlank()) return

        val token = providedToken?.takeIf { it.isNotBlank() }
            ?: pref.getString(PreferenceClass.KEY_FCM_TOKEN).takeIf { it.isNotBlank() }
            ?: return

        runCatching {
            registerTokenToBackend(token)
        }.onFailure {
            Timber.w(it, "Gagal kirim FCM token tersimpan")
        }
    }

    private suspend fun awaitFcmToken(): String =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(token) }
                .addOnFailureListener { error -> cont.resumeWithException(error) }
        }

    private suspend fun registerTokenToBackend(token: String) {
        apiService.registerDeviceToken(
            RegisterDeviceTokenRequest(
                token = token,
                device_name = "${Build.MANUFACTURER} ${Build.MODEL}",
                app_version = deviceInfoProvider.getAppVersion(),
                os_version = "Android ${deviceInfoProvider.getOsVersion()}",
            ),
        )
    }
}
