package com.gridy.rohmahapp.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gridy.rohmahapp.api.ApiClient
import com.gridy.rohmahapp.repository.DeviceTokenRepository
import com.gridy.rohmahapp.utils.DeviceInfoProvider
import com.gridy.rohmahapp.utils.NotificationHelper
import com.gridy.rohmahapp.utils.PreferenceClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val pref = PreferenceClass(applicationContext)
        pref.putString(PreferenceClass.KEY_FCM_TOKEN, token)

        serviceScope.launch {
            DeviceTokenRepository(
                apiService = ApiClient.create(applicationContext),
                pref = pref,
                deviceInfoProvider = DeviceInfoProvider(applicationContext),
            ).registerStoredOrProvidedTokenIfLoggedIn(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"].orEmpty()
        val title = data["title"] ?: message.notification?.title ?: "RohMah App"
        val body = data["body"] ?: message.notification?.body ?: "Anda memiliki notifikasi baru."

        if (type.isBlank()) {
            Timber.w("FCM ignored: missing type field")
            return
        }

        val pref = PreferenceClass(applicationContext)
        val role = pref.getString(PreferenceClass.KEY_USER_ROLE).ifBlank { null }
        val isLoggedIn = pref.getString(PreferenceClass.KEY_USER_TOKEN).isNotBlank()

        if (!isLoggedIn && (type.startsWith("teacher_") || type.startsWith("student_"))) {
            Timber.w("FCM ignored: role specific message while logged out")
            return
        }

        if (!isAllowedForCurrentRole(type, role)) {
            Timber.w("FCM ignored: role mismatch for type=%s", type)
            return
        }

        NotificationHelper.showScheduleNotification(
            context = this,
            type = type,
            title = title,
            body = body,
            data = data,
        )
    }

    private fun isAllowedForCurrentRole(type: String, role: String?): Boolean {
        return when {
            type.startsWith("teacher_") -> role == "teacher"
            type.startsWith("student_") -> role == "student"
            else -> true
        }
    }
}