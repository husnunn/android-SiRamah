// File: /app/src/main/java/com/gridy/rohmahapp/utils/LocationProvider.kt
package com.gridy.rohmahapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CapturedLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val provider: String?,
    val isMock: Boolean?,
    val capturedAt: String
)

class LocationProvider(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun captureCurrentLocation(): CapturedLocationInfo? {
        if (!isLocationEnabled()) return null

        return suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()

            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (location == null) {
                        cont.resume(null)
                        return@addOnSuccessListener
                    }

                    cont.resume(
                        CapturedLocationInfo(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyM = location.accuracy.toDouble(),
                            provider = location.provider,
                            isMock = location.isFromMockProvider,
                            capturedAt = DateTimeUtil.nowIsoLocal()
                        )
                    )
                }
                .addOnFailureListener {
                    cont.resume(null)
                }

            cont.invokeOnCancellation {
                tokenSource.cancel()
            }
        }
    }
}