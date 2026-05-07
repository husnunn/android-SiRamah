package com.gridy.rohmahapp.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreTestRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun saveTestData(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "name" to "RohMahApp Test",
            "status" to "connected",
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("test_connection")
            .document("android_first_test")
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}