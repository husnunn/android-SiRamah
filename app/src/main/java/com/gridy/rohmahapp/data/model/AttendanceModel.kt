package com.gridy.rohmahapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttendanceSubmitRequest(
    val attendance_site_id: Int,
    val attendance_type: String, // check_in / check_out
    val client_time: String?,
    val network: AttendanceNetworkRequest,
    val location: AttendanceLocationRequest,
    val device: AttendanceDeviceRequest
)

@JsonClass(generateAdapter = true)
data class AttendanceNetworkRequest(
    val ssid: String?,
    val bssid: String?,
    val local_ip: String?,
    val gateway_ip: String?,
    val subnet_prefix: Int?,
    val transport: String? = "WIFI"
)

@JsonClass(generateAdapter = true)
data class AttendanceLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Double?,
    val provider: String?,
    val is_mock: Boolean?,
    val captured_at: String?
)

@JsonClass(generateAdapter = true)
data class AttendanceDeviceRequest(
    val platform: String = "android",
    val app_version: String?,
    val os_version: String?
)


// RESPONSE

@JsonClass(generateAdapter = true)
data class AttendanceResponse(
    val message: String,
    val status: String, // approved / rejected
    val reason_code: String?,
    val reason_detail: String?,
    val validation: AttendanceValidation?,
    val record: AttendanceRecord?
)

@JsonClass(generateAdapter = true)
data class AttendanceValidation(
    val eligibility: AttendanceEligibility?,
    val evidence: AttendanceEvidence?
)

@JsonClass(generateAdapter = true)
data class AttendanceEligibility(
    val allowed: Boolean?,
    val reason_code: String?,
    val reason_detail: String?,
    val schedule_id: Int?,
    val matched_event_id: Int?,
    val attendance_status: String?,
    val late_minutes: Int?,
    val calendar_event_id: Int?
)

@JsonClass(generateAdapter = true)
data class AttendanceEvidence(
    val valid: Boolean?,
    val reason_code: String?,
    val reason_detail: String?,
    val distance_m: Double?,
    val wifi_rule_id: Int?,
    val site_id: Int?
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: Int,
    val attendance_type: String,
    val status: String,
    val attendance_time: String?,
    val reason_code: String?,
    val reason_detail: String?,
    val distance_m: Double?,
    val site: AttendanceSite?,
    val schedule_id: Int?,
    val network: AttendanceNetworkInfo?,
    val location: AttendanceLocationInfo?,
    val created_at: String?
)

@JsonClass(generateAdapter = true)
data class AttendanceSite(
    val id: Int,
    val name: String
)

/** Titik absensi lengkap di akar JSON (`/me`, `/today`) — SpecApi.md */
@JsonClass(generateAdapter = true)
data class AttendanceSiteGeo(
    val id: Int,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius_m: Double?
)

/** Untuk spinner / ringkasan UI */
data class AttendanceSiteOptionUi(
    val id: Int,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val radiusM: Double?
)

@JsonClass(generateAdapter = true)
data class AttendanceNetworkInfo(
    val ssid: String?
)

@JsonClass(generateAdapter = true)
data class AttendanceLocationInfo(
    val latitude: Double?,
    val longitude: Double?
)

@JsonClass(generateAdapter = true)
data class AttendanceTodayResponse(
    val data: List<AttendanceRecord>,
    val attendance_sites: List<AttendanceSiteGeo>?
)

/** Ringkasan peta radius absensi (visualisasi; keputusan akhir tetap backend). */
data class AttendanceMapUi(
    val siteName: String?,
    val siteLatitude: Double?,
    val siteLongitude: Double?,
    val radiusMeter: Double?,
    val userLatitude: Double?,
    val userLongitude: Double?,
    val distanceMeters: Float?,
    val distanceText: String,
    val radiusStatusText: String,
    val isInsideRadius: Boolean?
) {
    val showMapSection: Boolean
        get() = siteLatitude != null && siteLongitude != null
}

data class AttendanceTodayUi(
    val locationStatusText: String,
    val checkInHistoryText: String,
    val checkOutHistoryText: String,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean,
    /** Kode status harian dari backend: present, late, excused, sick, dispensation, absent, holiday, dll. */
    val dailyStatusCode: String?,
    /** Label siap tampil (Bahasa Indonesia). */
    val dailyStatusLabel: String?,
    /** Pesan informasi (mis. status manual dari admin). */
    val infoMessage: String?,
    /** True jika badge status harian perlu ditampilkan. */
    val showStatusBadge: Boolean,
    /** Override absensi harian dari backend (`override.active`). */
    val overrideActive: Boolean = false,
    val overrideEventType: String? = null,
    val dismissStudentsEarly: Boolean = false,
    val waiveCheckOut: Boolean = false,
    /** Teks kartu override (jenis acara + flag; bukan mengganti `infoMessage`). */
    val overrideUiNotes: String? = null,
    /** Daftar titik dari API root `attendance_sites` (+ fallback cache `/me`). */
    val attendanceSites: List<AttendanceSiteOptionUi> = emptyList(),
    /** ID untuk `attendance_site_id` pada POST check-in/out (auto jika satu titik). */
    val resolvedAttendanceSiteId: Int? = null,
    /** BSSID Wi‑Fi saat ini (untuk ditampilkan di samping nama titik absensi). */
    val wifiBssidDisplay: String? = null,
    /** Teks jam policy dari backend (`effective_policy`), multi-baris; null jika tidak ada policy. */
    val policyInfoText: String? = null,
    /** Lokasi pengguna titik radius vs sekolah (GPS + titik aktif dari backend). */
    val mapUi: AttendanceMapUi
)

@JsonClass(generateAdapter = true)
data class AttendanceSubmitUi(
    val success: Boolean,
    val message: String
)