package com.gridy.rohmahapp.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class FirebaseTokenRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun saveUserDeviceToken(
        userId: String,
        deviceId: String,
        fcmToken: String,
        deviceName: String,
        appVersion: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf(
            "fcmToken" to fcmToken,
            "platform" to "android",
            "deviceName" to deviceName,
            "appVersion" to appVersion,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(userId)
            .collection("devices")
            .document(deviceId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}