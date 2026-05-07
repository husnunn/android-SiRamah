package com.gridy.rohmahapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Body untuk check-in / check-out harian (tanpa `attendance_type`; endpoint menentukan jenis). */
@JsonClass(generateAdapter = true)
data class DailyAttendanceSubmitRequest(
    val attendance_site_id: Int,
    val client_time: String?,
    val network: AttendanceNetworkRequest,
    val location: AttendanceLocationRequest,
    val device: AttendanceDeviceRequest
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceCheckInResponse(
    val message: String,
    val status: String,
    val attendance_status: String?,
    val late_minutes: Int?,
    val reason_code: String?,
    val reason_detail: String?,
    /** Respons 422 penolakan dapat berisi validation tanpa record (SpecApi.md). */
    val validation: AttendanceValidation?,
    val record: DailyAttendanceDayRecord?
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceCheckOutResponse(
    val message: String,
    val status: String,
    val reason_code: String?,
    val reason_detail: String?,
    val validation: AttendanceValidation?,
    val record: DailyAttendanceDayRecord?
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceDayRecord(
    val id: Int?,
    val date: String?,
    val check_in_at: String?,
    val check_out_at: String?,
    val status: String?,
    val late_minutes: Int?,
    val site: AttendanceSite?
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceTodayResponse(
    val data: DailyAttendanceTodayData?,
    val attendance_sites: List<AttendanceSiteGeo>?
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceOverride(
    val active: Boolean?,
    val event_type: String?,
    val dismiss_students_early: Boolean?,
    val waive_check_out: Boolean?
)

/** Policy absensi aktif dari admin/backend (`effective_policy` / alternatif `policy`). */
@JsonClass(generateAdapter = true)
data class DailyAttendanceEffectivePolicy(
    val check_in_open_at: String? = null,
    val check_in_on_time_until: String? = null,
    val check_in_close_at: String? = null,
    val check_out_open_at: String? = null,
    val check_out_close_at: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyAttendanceTodayData(
    val date: String?,
    val status: String?,
    val label: String?,
    val source: String?,
    val check_in_at: String?,
    val check_out_at: String?,
    val late_minutes: Int?,
    val can_check_in: Boolean?,
    val can_check_out: Boolean?,
    val site: AttendanceSite?,
    val message: String?,
    /** Beberapa backend menaruh titik ini di dalam `data` meski SpecApi.md menyebut akar JSON. */
    val attendance_sites: List<AttendanceSiteGeo>? = null,
    /** Objek `override` dari JSON backend (nama property Kotlin tidak boleh `override`). */
    @Json(name = "override")
    val attendanceOverride: DailyAttendanceOverride?,
    /** Jam policy aktif (prioritas utama). */
    @Json(name = "effective_policy")
    val effectivePolicy: DailyAttendanceEffectivePolicy? = null,
    /** Nama alternatif jika backend sementara memakai kunci `policy`. */
    @Json(name = "policy")
    val policyAlternative: DailyAttendanceEffectivePolicy? = null
)
