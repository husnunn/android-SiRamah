package com.gridy.rohmahapp.utils

import android.location.Location
import com.gridy.rohmahapp.data.model.AttendanceMapUi
import com.gridy.rohmahapp.data.model.AttendanceSiteOptionUi
import kotlin.math.roundToInt

object AttendanceMapComposer {

    fun compose(
        site: AttendanceSiteOptionUi?,
        userLat: Double?,
        userLng: Double?,
        hasLocationPermission: Boolean,
        locationEnabled: Boolean
    ): AttendanceMapUi {
        if (site == null) {
            return AttendanceMapUi(
                siteName = null,
                siteLatitude = null,
                siteLongitude = null,
                radiusMeter = null,
                userLatitude = userLat.takeIfCoords(),
                userLongitude = userLng.takeIfCoords(),
                distanceMeters = null,
                distanceText = "",
                radiusStatusText = "Titik absensi dengan koordinat belum tersedia.",
                isInsideRadius = null
            )
        }

        val sLat = site.latitude
        val sLng = site.longitude
        if (sLat == null || sLng == null) {
            return AttendanceMapUi(
                siteName = site.name,
                siteLatitude = null,
                siteLongitude = null,
                radiusMeter = site.radiusM,
                userLatitude = userLat.takeIfCoords(),
                userLongitude = userLng.takeIfCoords(),
                distanceMeters = null,
                distanceText = "",
                radiusStatusText = "Koordinat titik absensi \"${site.name}\" belum diatur dari server.",
                isInsideRadius = null
            )
        }

        if (!hasLocationPermission) {
            return AttendanceMapUi(
                siteName = site.name,
                siteLatitude = sLat,
                siteLongitude = sLng,
                radiusMeter = site.radiusM,
                userLatitude = null,
                userLongitude = null,
                distanceMeters = null,
                distanceText = "",
                radiusStatusText = "Aktifkan izin lokasi agar aplikasi dapat menampilkan posisi Anda di peta.",
                isInsideRadius = null
            )
        }

        if (!locationEnabled) {
            return AttendanceMapUi(
                siteName = site.name,
                siteLatitude = sLat,
                siteLongitude = sLng,
                radiusMeter = site.radiusM,
                userLatitude = null,
                userLongitude = null,
                distanceMeters = null,
                distanceText = "",
                radiusStatusText = "Aktifkan GPS atau layanan lokasi untuk menampilkan posisi Anda.",
                isInsideRadius = null
            )
        }

        val ulat = userLat.takeIfCoords()
        val ulng = userLng.takeIfCoords()
        if (ulat == null || ulng == null) {
            return AttendanceMapUi(
                siteName = site.name,
                siteLatitude = sLat,
                siteLongitude = sLng,
                radiusMeter = site.radiusM,
                userLatitude = null,
                userLongitude = null,
                distanceMeters = null,
                distanceText = "",
                radiusStatusText = "Lokasi Anda belum tersedia.",
                isInsideRadius = null
            )
        }

        val out = FloatArray(1)
        Location.distanceBetween(ulat, ulng, sLat, sLng, out)
        val distance = out[0]

        val radius = site.radiusM
        val isInside =
            radius?.takeIf { it > 0 }?.let { distance <= it.toDouble() }

        val radiusText = when {
            radius == null || radius <= 0 ->
                "Jarak ke titik tercatat; radius tidak tersedia di data server."

            isInside == true -> "Dalam jangkauan sekolah"
            else -> "Di luar jangkauan sekolah"
        }

        return AttendanceMapUi(
            siteName = site.name,
            siteLatitude = sLat,
            siteLongitude = sLng,
            radiusMeter = radius,
            userLatitude = ulat,
            userLongitude = ulng,
            distanceMeters = distance,
            distanceText = formatDistanceLine(distance),
            radiusStatusText = radiusText,
            isInsideRadius = isInside
        )
    }

    private fun formatDistanceLine(distanceMeters: Float): String {
        val m = distanceMeters.roundToInt().coerceAtLeast(1)
        val label = when {
            m < 1000 -> "$m m"
            else -> String.format(java.util.Locale.getDefault(), "%.2f km", m / 1000.0)
        }
        return "Jarak Anda ke titik sekolah: $label"
    }

    private fun Double?.takeIfCoords(): Double? {
        val v = this ?: return null
        if (v.isNaN()) return null
        return v
    }
}
