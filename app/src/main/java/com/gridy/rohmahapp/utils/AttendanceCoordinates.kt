package com.gridy.rohmahapp.utils

/**
 * Memperbaiki titik titik-absensi ketika lon/lat tertukar (sering dari backend/export),
 * serta kasus klasik seperti `latitude=112.xx, longitude=-7.xx` untuk lokasi Jawa.
 */
object AttendanceCoordinates {

    private val indonesiaRoughLat = -14.0..12.5
    private val indonesiaRoughLon = 92.5..157.5

    /**
     * @return Pair(latitude?, longitude?)
     */
    fun normalizeSchoolLatLng(latitude: Double?, longitude: Double?): Pair<Double?, Double?> {
        if (latitude == null || longitude == null) return latitude to longitude
        if (latitude.isNaN() || longitude.isNaN()) return latitude to longitude

        fun valid(lat: Double, lon: Double) =
            lat in -90.0..90.0 && lon in -180.0..180.0

        var lat = latitude
        var lon = longitude

        if (!valid(lat, lon)) {
            val swappedLon = latitude
            val swappedLat = longitude
            if (valid(swappedLat, swappedLon)) {
                lat = swappedLat
                lon = swappedLon
            }
            return lat to lon
        }

        val uprightIndonesia =
            lat in indonesiaRoughLat && lon in indonesiaRoughLon

        val looksSwapped =
            lon in indonesiaRoughLat && lat in indonesiaRoughLon

        if (!uprightIndonesia && looksSwapped) {
            return lon to lat
        }

        return lat to lon
    }
}
