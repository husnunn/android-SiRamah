// File: /app/src/main/java/com/gridy/rohmahapp/repository/AttendanceRepository.kt
package com.gridy.rohmahapp.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.data.local.dao.AttendanceDao
import com.gridy.rohmahapp.data.local.entity.AttendanceTodayEntity
import com.gridy.rohmahapp.data.model.AttendanceDeviceRequest
import com.gridy.rohmahapp.data.model.AttendanceLocationRequest
import com.gridy.rohmahapp.data.model.AttendanceMapUi
import com.gridy.rohmahapp.data.model.AttendanceNetworkRequest
import com.gridy.rohmahapp.data.model.AttendanceSiteGeo
import com.gridy.rohmahapp.utils.AttendanceCoordinates
import com.gridy.rohmahapp.data.model.AttendanceSiteOptionUi
import com.gridy.rohmahapp.data.model.AttendanceSubmitRequest
import com.gridy.rohmahapp.data.model.AttendanceSubmitUi
import com.gridy.rohmahapp.data.model.AttendanceTodayResponse
import com.gridy.rohmahapp.data.model.AttendanceTodayUi
import com.gridy.rohmahapp.data.model.DailyAttendanceEffectivePolicy
import com.gridy.rohmahapp.data.model.DailyAttendanceSubmitRequest
import com.gridy.rohmahapp.data.model.DailyAttendanceTodayData
import com.gridy.rohmahapp.data.model.DailyAttendanceTodayResponse
import com.gridy.rohmahapp.utils.AttendanceMapComposer
import com.gridy.rohmahapp.utils.CapturedLocationInfo
import com.gridy.rohmahapp.utils.CapturedNetworkInfo
import com.gridy.rohmahapp.utils.DailyAttendanceTimeUtil
import com.gridy.rohmahapp.utils.DateTimeUtil
import com.gridy.rohmahapp.utils.DeviceInfoProvider
import com.gridy.rohmahapp.utils.LocationProvider
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.WifiInfoProvider
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

/**
 * Menggabungkan `attendance_sites` sesuai spec (akar JSON) atau varian nested di dalam `data`.
 */
private fun mergedDailyAttendanceSites(resp: DailyAttendanceTodayResponse): List<AttendanceSiteGeo>? =
    resp.attendance_sites?.takeIf { it.isNotEmpty() }
        ?: resp.data?.attendance_sites?.takeIf { it.isNotEmpty() }

class AttendanceRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val wifiInfoProvider: WifiInfoProvider,
    private val locationProvider: LocationProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val attendanceDao: AttendanceDao
) {

    private val gson = Gson()

    suspend fun getTodayAttendanceUi(
        hasLocationPermission: Boolean,
        forceRemoteRefresh: Boolean,
    ): AttendanceTodayUi {
        val role = pref.getString(PreferenceClass.KEY_USER_ROLE)
        val wifiReady = wifiInfoProvider.isWifiConnected()
        val locationReady =
            hasLocationPermission && locationProvider.isLocationEnabled()

        val locationStatusText =
            locationStatusMessage(hasLocationPermission, wifiReady, locationReady)
        val wifiBssidDisplay = formatWifiBssidDisplay()

        return when (role) {
            "student" ->
                resolveStudentAttendance(
                    forceRemoteRefresh,
                    locationStatusText,
                    wifiReady,
                    locationReady,
                    hasLocationPermission,
                    wifiBssidDisplay,
                )

            "teacher" ->
                resolveTeacherAttendance(
                    forceRemoteRefresh,
                    locationStatusText,
                    wifiReady,
                    locationReady,
                    hasLocationPermission,
                    wifiBssidDisplay,
                )

            else -> throw IllegalArgumentException("Role user tidak valid")
        }
    }

    /** BSSID AP yang sedang terhubung (null jika tidak Wi‑Fi atau tidak tersedia). */
    private fun formatWifiBssidDisplay(): String? {
        val raw = wifiInfoProvider.capture()?.bssid ?: return null
        val t = raw.trim()
        if (t.isEmpty()) return null
        return t.uppercase(Locale.getDefault())
    }

    /** Menyimpan pilihan pengguna saat ada banyak titik (SpecApi.md). */
    fun persistSelectedAttendanceSite(siteId: Int) {
        pref.putInt(PreferenceClass.KEY_SELECTED_ATTENDANCE_SITE_ID, siteId)
    }

    private fun locationStatusMessage(
        hasLocationPermission: Boolean,
        wifiReady: Boolean,
        locationReady: Boolean
    ): String = when {
        !hasLocationPermission -> "Izin lokasi belum diberikan."
        !wifiReady -> "Belum terhubung ke Wi-Fi sekolah."
        !locationReady -> "GPS/lokasi belum aktif."
        else -> "Wi-Fi terhubung dan lokasi siap. Menunggu validasi server."
    }


    private suspend fun resolveStudentAttendance(
        forceRemoteRefresh: Boolean,
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        if (!forceRemoteRefresh) {
            return buildStudentDailyTodayFromRoomOnly(
                locationStatusText,
                wifiReady,
                locationReady,
                hasLocationPermission,
                wifiBssidDisplay,
            )
        }
        return tryRemoteStudentDailyThenStaleFallback(
            locationStatusText,
            wifiReady,
            locationReady,
            hasLocationPermission,
            wifiBssidDisplay,
        )
    }

    private suspend fun resolveTeacherAttendance(
        forceRemoteRefresh: Boolean,
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        if (!forceRemoteRefresh) {
            return buildTeacherDailyFromRoomOnly(
                locationStatusText,
                wifiReady,
                locationReady,
                hasLocationPermission,
                wifiBssidDisplay,
            )
        }
        return fetchTeacherAttendanceFromNetwork(
            locationStatusText,
            wifiReady,
            locationReady,
            hasLocationPermission,
            wifiBssidDisplay,
        )
    }

    private suspend fun buildStudentDailyTodayFromRoomOnly(
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        val today = DateTimeUtil.todayDate()
        val row = attendanceDao.getAttendanceToday(today)
        val cachedResp =
            row?.rawJson?.let { json ->
                runCatching { gson.fromJson(json, DailyAttendanceTodayResponse::class.java) }
                    .getOrNull()
            }
        return if (cachedResp != null) {
            buildStudentDailyTodayUiFromResponse(
                resp = cachedResp,
                locationStatusText = locationStatusText,
                wifiReady = wifiReady,
                locationReady = locationReady,
                hasLocationPermission = hasLocationPermission,
                wifiBssidDisplay = wifiBssidDisplay,
            )
        } else {
            buildEmptyStudentTodayUi(
                locationStatusText =
                    "$locationStatusText\nData belum tersedia. Hubungkan internet untuk memuat data pertama kali.",
                wifiBssidDisplay = wifiBssidDisplay,
                hasLocationPermission = hasLocationPermission,
            )
        }
    }

    private suspend fun buildTeacherDailyFromRoomOnly(
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        val today = DateTimeUtil.todayDate()
        val row = attendanceDao.getAttendanceToday(today)
        val resp =
            row?.rawJson?.let {
                runCatching { gson.fromJson(it, AttendanceTodayResponse::class.java) }
                    .getOrNull()
            }
        return if (resp != null) {
            mapTeacherResponseToAttendanceTodayUi(
                response = resp,
                locationStatusText = locationStatusText,
                wifiReady = wifiReady,
                locationReady = locationReady,
                hasLocationPermission = hasLocationPermission,
                wifiBssidDisplay = wifiBssidDisplay,
            )
        } else {
            mapTeacherResponseToAttendanceTodayUi(
                response = AttendanceTodayResponse(emptyList(), null),
                locationStatusText =
                    "$locationStatusText\nTarik untuk refresh untuk memuat data terbaru.",
                wifiReady = wifiReady,
                locationReady = locationReady,
                hasLocationPermission = hasLocationPermission,
                wifiBssidDisplay = wifiBssidDisplay,
            )
        }
    }

    /** Memanggil endpoint harian siswa ketika boleh refresh jarak-jauh. */
    private suspend fun tryRemoteStudentDailyThenStaleFallback(
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?
    ): AttendanceTodayUi {
        return try {
            val resp = apiService.studentDailyAttendanceToday()

            cacheStudentDailyTodayResponse(resp)

            buildStudentDailyTodayUiFromResponse(
                resp = resp,
                locationStatusText = locationStatusText,
                wifiReady = wifiReady,
                locationReady = locationReady,
                hasLocationPermission = hasLocationPermission,
                wifiBssidDisplay = wifiBssidDisplay,
            )
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException) {
                throw e
            }

            val today = DateTimeUtil.todayDate()
            val cached = attendanceDao.getAttendanceToday(today)

            val cachedResp = cached?.rawJson?.let { json ->
                runCatching {
                    gson.fromJson(json, DailyAttendanceTodayResponse::class.java)
                }.getOrNull()
            }

            if (cachedResp != null) {
                buildStudentDailyTodayUiFromResponse(
                    resp = cachedResp,
                    locationStatusText = "$locationStatusText\nMenampilkan data terakhir tersimpan.",
                    wifiReady = wifiReady,
                    locationReady = locationReady,
                    hasLocationPermission = hasLocationPermission,
                    wifiBssidDisplay = wifiBssidDisplay,
                )
            } else {
                buildEmptyStudentTodayUi(
                    locationStatusText = "$locationStatusText\nData belum tersedia. Periksa koneksi internet.",
                    wifiBssidDisplay = wifiBssidDisplay,
                    hasLocationPermission = hasLocationPermission
                )
            }
        }
    }

    private suspend fun buildStudentDailyTodayUiFromResponse(
        resp: DailyAttendanceTodayResponse,
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        mergedDailyAttendanceSites(resp)?.takeIf { it.isNotEmpty() }?.let { sites ->
            pref.putString(PreferenceClass.KEY_ATTENDANCE_SITES_JSON_CACHE, gson.toJson(sites))
        }

        val mergedGeo =
            mergeAttendanceSites(mergedDailyAttendanceSites(resp), pref)
        val sitesUi = mergedGeo.map { geoToUi(it) }
        val recordSiteHint = resp.data?.site?.id
        val resolvedSiteId = resolveAttendanceSiteId(mergedGeo, recordSiteHint)
        val siteReady = resolvedSiteId != null

        val mapUi = buildAttendanceMapUi(mergedGeo, resolvedSiteId, hasLocationPermission)

        val d =
            resp.data
                ?: return AttendanceTodayUi(
                    locationStatusText = locationStatusText,
                    checkInHistoryText = "Belum Presensi Masuk",
                    checkOutHistoryText = "Belum Presensi Pulang",
                    canCheckIn = false,
                    canCheckOut = false,
                    dailyStatusCode = null,
                    dailyStatusLabel = null,
                    infoMessage = null,
                    showStatusBadge = false,
                    overrideActive = false,
                    overrideEventType = null,
                    dismissStudentsEarly = false,
                    waiveCheckOut = false,
                    overrideUiNotes = null,
                    attendanceSites = sitesUi,
                    resolvedAttendanceSiteId = resolvedSiteId,
                    wifiBssidDisplay = wifiBssidDisplay,
                    policyInfoText = null,
                    mapUi = mapUi,
                )

        val label = d.label.orEmpty().ifBlank { dailyStatusLabelFromCode(d.status) }

        val o = d.attendanceOverride
        val overrideActive = o?.active == true
        val waiveCheckOut = o?.waive_check_out == true
        val dismissEarly = o?.dismiss_students_early == true
        val overrideUiNotes = buildOverrideUiNotes(d)

        val canPhysical = wifiReady && locationReady && siteReady
        val canCheckIn = (d.can_check_in == true) && canPhysical
        val canCheckOut =
            (d.can_check_out == true) && canPhysical && !waiveCheckOut

        val showBadge = !d.status.isNullOrBlank() || label.isNotBlank()

        val policyInfoText =
            buildPolicyInfoText(d.effectivePolicy ?: d.policyAlternative)

        return AttendanceTodayUi(
            locationStatusText = locationStatusText,
            checkInHistoryText = formatDailyCheckInHistory(d),
            checkOutHistoryText = formatDailyCheckOutHistory(d),
            canCheckIn = canCheckIn,
            canCheckOut = canCheckOut,
            dailyStatusCode = d.status,
            dailyStatusLabel = label.ifBlank { null },
            infoMessage = d.message,
            showStatusBadge = showBadge,
            overrideActive = overrideActive,
            overrideEventType = o?.event_type,
            dismissStudentsEarly = dismissEarly,
            waiveCheckOut = waiveCheckOut,
            overrideUiNotes = overrideUiNotes,
            attendanceSites = sitesUi,
            resolvedAttendanceSiteId = resolvedSiteId,
            wifiBssidDisplay = wifiBssidDisplay,
            policyInfoText = policyInfoText,
            mapUi = mapUi,
        )
    }

    private suspend fun buildEmptyStudentTodayUi(
        locationStatusText: String,
        wifiBssidDisplay: String?,
        hasLocationPermission: Boolean,
    ): AttendanceTodayUi {
        val mergedGeo = mergeAttendanceSites(null, pref)
        val sitesUi = mergedGeo.map { geoToUi(it) }
        val resolvedSiteId = resolveAttendanceSiteId(mergedGeo, recordSiteId = null)
        val mapUi = buildAttendanceMapUi(mergedGeo, resolvedSiteId, hasLocationPermission)
        return AttendanceTodayUi(
            locationStatusText = locationStatusText,
            checkInHistoryText = "Belum Presensi Masuk",
            checkOutHistoryText = "Belum Presensi Pulang",
            canCheckIn = false,
            canCheckOut = false,
            dailyStatusCode = null,
            dailyStatusLabel = null,
            infoMessage = null,
            showStatusBadge = false,
            overrideActive = false,
            overrideEventType = null,
            dismissStudentsEarly = false,
            waiveCheckOut = false,
            overrideUiNotes = null,
            attendanceSites = sitesUi,
            resolvedAttendanceSiteId = resolvedSiteId,
            wifiBssidDisplay = wifiBssidDisplay,
            policyInfoText = null,
            mapUi = mapUi,
        )
    }

    private suspend fun cacheStudentDailyTodayResponse(resp: DailyAttendanceTodayResponse) {
        val data = resp.data ?: return

        val today = data.date ?: DateTimeUtil.todayDate()

        attendanceDao.upsertAttendanceToday(
            AttendanceTodayEntity(
                date = today,
                checkInTime = data.check_in_at,
                checkOutTime = data.check_out_at,
                status = data.status ?: "unknown",
                rawJson = gson.toJson(resp),
                cachedAt = DateTimeUtil.nowIsoLocal(),
                synced = true
            )
        )

        attendanceDao.deleteOldAttendanceToday(today)
    }

    /**
     * Menyusun teks tampilan policy dari backend tanpa menghitung rule jam di client.
     */
    private fun buildPolicyInfoText(policy: DailyAttendanceEffectivePolicy?): String? {
        if (policy == null) return null
        fun norm(s: String?) = s?.trim()?.takeIf { it.isNotEmpty() }

        val open = norm(policy.check_in_open_at)
        val onTimeUntil = norm(policy.check_in_on_time_until)
        val closeIn = norm(policy.check_in_close_at)
        val outOpen = norm(policy.check_out_open_at)
        val outClose = norm(policy.check_out_close_at)

        val lines = mutableListOf<String>()
        when {
            open != null && onTimeUntil != null ->
                lines.add("Masuk: $open–$onTimeUntil")
            open != null ->
                lines.add("Masuk mulai $open")
            onTimeUntil != null ->
                lines.add("Tepat waktu sampai $onTimeUntil")
        }
        if (closeIn != null) {
            lines.add("Terlambat sampai $closeIn")
        }
        when {
            outOpen != null && outClose != null ->
                lines.add("Pulang: $outOpen–$outClose")
            outOpen != null ->
                lines.add("Pulang mulai $outOpen")
            outClose != null ->
                lines.add("Pulang sampai $outClose")
        }

        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    /**
     * Teks kartu override dari backend (`override.*`). Pesan umum tetap di [DailyAttendanceTodayData.message]
     * → [AttendanceTodayUi.infoMessage].
     */
    private fun buildOverrideUiNotes(d: DailyAttendanceTodayData): String? {
        val o = d.attendanceOverride ?: return null
        if (o.active != true) return null

        val lines = mutableListOf<String>()
        mapOverrideEventTypeLine(o.event_type)?.let { lines.add(it) }
        if (o.dismiss_students_early == true) {
            lines.add("Hari ini siswa dipulangkan lebih awal.")
        }
        if (o.waive_check_out == true) {
            lines.add("Check-out manual tidak diwajibkan hari ini.")
        }
        if (lines.isEmpty()) {
            lines.add("Ada override absensi untuk hari ini.")
        }
        return lines.joinToString("\n")
    }

    private fun mapOverrideEventTypeLine(eventType: String?): String? {
        val raw = eventType?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val label = when (raw) {
            "teacher_meeting" -> "Rapat guru"
            "early_dismissal" -> "Pemulangan lebih awal"
            "special_event" -> "Acara khusus"
            "attendance_closed" -> "Absensi ditutup"
            "holiday_override" -> "Penyesuaian hari libur"
            else -> raw.replace('_', ' ')
        }
        return "Jenis acara: $label"
    }

    private fun formatDailyCheckInHistory(d: DailyAttendanceTodayData): String {
        val time = d.check_in_at ?: return "Belum Presensi Masuk"
        val base = "Hadir Masuk: ${DailyAttendanceTimeUtil.toDisplayTime(time)}"
        val lateMin = d.late_minutes
        return if (d.status == "late" && lateMin != null && lateMin > 0) {
            "$base (Terlambat $lateMin menit)"
        } else {
            base
        }
    }

    private fun formatDailyCheckOutHistory(d: DailyAttendanceTodayData): String {
        val time = d.check_out_at ?: return "Belum Presensi Pulang"
        return "Hadir Pulang: ${DailyAttendanceTimeUtil.toDisplayTime(time)}"
    }

    private fun dailyStatusLabelFromCode(status: String?): String = when (status) {
        "present" -> "Hadir"
        "late" -> "Terlambat"
        "excused" -> "Izin"
        "sick" -> "Sakit"
        "dispensation" -> "Dispensasi"
        "absent" -> "Alpa"
        "holiday" -> "Libur"
        else -> status ?: "—"
    }

    private suspend fun fetchTeacherAttendanceFromNetwork(
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        return try {
            val response = apiService.teacherAttendanceToday()
            cacheTeacherAttendanceTodayResponse(response)
            mapTeacherResponseToAttendanceTodayUi(
                response = response,
                locationStatusText = locationStatusText,
                wifiReady = wifiReady,
                locationReady = locationReady,
                hasLocationPermission = hasLocationPermission,
                wifiBssidDisplay = wifiBssidDisplay,
            )
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException) {
                throw e
            }
            buildTeacherDailyFromRoomOnly(
                "$locationStatusText\nTidak dapat memperbarui; menggunakan data lokal jika ada.",
                wifiReady,
                locationReady,
                hasLocationPermission,
                wifiBssidDisplay,
            )
        }
    }

    private suspend fun cacheTeacherAttendanceTodayResponse(response: AttendanceTodayResponse) {
        val today = DateTimeUtil.todayDate()
        attendanceDao.upsertAttendanceToday(
            AttendanceTodayEntity(
                date = today,
                checkInTime = null,
                checkOutTime = null,
                status = "teacher",
                rawJson = gson.toJson(response),
                cachedAt = DateTimeUtil.nowIsoLocal(),
                synced = true,
            )
        )
        attendanceDao.deleteOldAttendanceToday(today)
    }

    private suspend fun mapTeacherResponseToAttendanceTodayUi(
        response: AttendanceTodayResponse,
        locationStatusText: String,
        wifiReady: Boolean,
        locationReady: Boolean,
        hasLocationPermission: Boolean,
        wifiBssidDisplay: String?,
    ): AttendanceTodayUi {
        response.attendance_sites?.takeIf { it.isNotEmpty() }?.let { sites ->
            pref.putString(PreferenceClass.KEY_ATTENDANCE_SITES_JSON_CACHE, gson.toJson(sites))
        }

        val mergedGeo = mergeAttendanceSites(response.attendance_sites, pref)
        val sitesUi = mergedGeo.map { geoToUi(it) }

        val approvedCheckIn = response.data.firstOrNull {
            it.attendance_type == "check_in" && it.status == "approved"
        }

        val approvedCheckOut = response.data.firstOrNull {
            it.attendance_type == "check_out" && it.status == "approved"
        }

        val recordSiteHint =
            approvedCheckIn?.site?.id ?: approvedCheckOut?.site?.id
        val resolvedSiteId = resolveAttendanceSiteId(mergedGeo, recordSiteHint)
        val siteReady = resolvedSiteId != null

        val mapUi = buildAttendanceMapUi(mergedGeo, resolvedSiteId, hasLocationPermission)

        val canPhysical = wifiReady && locationReady && siteReady

        return AttendanceTodayUi(
            locationStatusText = locationStatusText,
            checkInHistoryText = approvedCheckIn?.attendance_time?.let {
                "Hadir Masuk: ${DateTimeUtil.toDisplayTime(it)}"
            } ?: "Belum Presensi Masuk",
            checkOutHistoryText = approvedCheckOut?.attendance_time?.let {
                "Hadir Pulang: ${DateTimeUtil.toDisplayTime(it)}"
            } ?: "Belum Presensi Pulang",
            canCheckIn = canPhysical && approvedCheckIn == null,
            canCheckOut = canPhysical && approvedCheckIn != null && approvedCheckOut == null,
            dailyStatusCode = null,
            dailyStatusLabel = null,
            infoMessage = null,
            showStatusBadge = false,
            overrideActive = false,
            overrideEventType = null,
            dismissStudentsEarly = false,
            waiveCheckOut = false,
            overrideUiNotes = null,
            attendanceSites = sitesUi,
            resolvedAttendanceSiteId = resolvedSiteId,
            wifiBssidDisplay = wifiBssidDisplay,
            policyInfoText = null,
            mapUi = mapUi,
        )
    }

    suspend fun submitAttendanceUi(
        attendanceType: String,
        attendanceSiteId: Int,
        hasLocationPermission: Boolean
    ): AttendanceSubmitUi {
        if (!hasLocationPermission) {
            throw IllegalStateException("Izin lokasi belum diberikan.")
        }

        val network = wifiInfoProvider.capture()
            ?: throw IllegalStateException("Perangkat belum terhubung ke Wi-Fi sekolah.")

        val location = locationProvider.captureCurrentLocation()
            ?: throw IllegalStateException("Lokasi belum tersedia. Aktifkan GPS/lokasi terlebih dahulu.")

        val role = pref.getString(PreferenceClass.KEY_USER_ROLE)

        return when (role) {
            "student" -> submitStudentDailyAttendance(attendanceType, attendanceSiteId, network, location)
            "teacher" -> submitTeacherLegacyAttendance(attendanceType, attendanceSiteId, network, location)
            else -> throw IllegalArgumentException("Role user tidak valid")
        }
    }

    private suspend fun submitStudentDailyAttendance(
        attendanceType: String,
        attendanceSiteId: Int,
        network: CapturedNetworkInfo,
        location: CapturedLocationInfo
    ): AttendanceSubmitUi {
        val request = DailyAttendanceSubmitRequest(
            attendance_site_id = attendanceSiteId,
            client_time = DateTimeUtil.nowIsoLocal(),
            network = AttendanceNetworkRequest(
                ssid = network.ssid,
                bssid = network.bssid,
                local_ip = network.localIp,
                gateway_ip = network.gatewayIp,
                subnet_prefix = network.subnetPrefix,
                transport = network.transport
            ),
            location = AttendanceLocationRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy_m = location.accuracyM,
                provider = location.provider,
                is_mock = location.isMock,
                captured_at = location.capturedAt
            ),
            device = AttendanceDeviceRequest(
                platform = "android",
                app_version = deviceInfoProvider.getAppVersion(),
                os_version = deviceInfoProvider.getOsVersion()
            )
        )

        return when (attendanceType) {
            "check_in" -> {
                val response = apiService.studentDailyCheckIn(request)
                val ok = response.status == "approved"
                val message = if (ok) {
                    response.message
                } else {
                    mapDailyReasonToMessage(response.reason_code, response.reason_detail)
                }
                AttendanceSubmitUi(success = ok, message = message)
            }
            "check_out" -> {
                val response = apiService.studentDailyCheckOut(request)
                val ok = response.status == "approved"
                val message = if (ok) {
                    response.message
                } else {
                    mapDailyReasonToMessage(response.reason_code, response.reason_detail)
                }
                AttendanceSubmitUi(success = ok, message = message)
            }
            else -> throw IllegalArgumentException("Jenis absensi tidak dikenal")
        }
    }

    private suspend fun submitTeacherLegacyAttendance(
        attendanceType: String,
        attendanceSiteId: Int,
        network: CapturedNetworkInfo,
        location: CapturedLocationInfo
    ): AttendanceSubmitUi {
        val request = AttendanceSubmitRequest(
            attendance_site_id = attendanceSiteId,
            attendance_type = attendanceType,
            client_time = DateTimeUtil.nowIsoLocal(),
            network = AttendanceNetworkRequest(
                ssid = network.ssid,
                bssid = network.bssid,
                local_ip = network.localIp,
                gateway_ip = network.gatewayIp,
                subnet_prefix = network.subnetPrefix,
                transport = network.transport
            ),
            location = AttendanceLocationRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy_m = location.accuracyM,
                provider = location.provider,
                is_mock = location.isMock,
                captured_at = location.capturedAt
            ),
            device = AttendanceDeviceRequest(
                platform = "android",
                app_version = deviceInfoProvider.getAppVersion(),
                os_version = deviceInfoProvider.getOsVersion()
            )
        )

        val response = when (attendanceType) {
            "check_in" -> apiService.teacherCheckIn(request)
            "check_out" -> apiService.teacherCheckOut(request)
            else -> throw IllegalArgumentException("Jenis absensi tidak dikenal")
        }

        val message = if (response.status == "approved") {
            response.message
        } else {
            mapLegacyReasonToMessage(response.reason_code, response.reason_detail)
        }

        return AttendanceSubmitUi(
            success = response.status == "approved",
            message = message
        )
    }

    private fun mergeAttendanceSites(
        remote: List<AttendanceSiteGeo>?,
        pref: PreferenceClass
    ): List<AttendanceSiteGeo> {
        if (!remote.isNullOrEmpty()) return remote
        return loadCachedSitesFromPref(pref)
    }

    private fun loadCachedSitesFromPref(pref: PreferenceClass): List<AttendanceSiteGeo> {
        val json = pref.getString(PreferenceClass.KEY_ATTENDANCE_SITES_JSON_CACHE, "").trim()
        if (json.isEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<ArrayList<AttendanceSiteGeo>>() {}.type
            gson.fromJson<ArrayList<AttendanceSiteGeo>>(json, type)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun geoToUi(g: AttendanceSiteGeo): AttendanceSiteOptionUi {
        val (la, lo) =
            AttendanceCoordinates.normalizeSchoolLatLng(g.latitude, g.longitude)
        return AttendanceSiteOptionUi(
            id = g.id,
            name = g.name?.takeIf { !it.isBlank() } ?: "Titik ${g.id}",
            latitude = la,
            longitude = lo,
            radiusM = g.radius_m
        )
    }

    private suspend fun buildAttendanceMapUi(
        mergedGeo: List<AttendanceSiteGeo>,
        resolvedSiteId: Int?,
        hasLocationPermission: Boolean
    ): AttendanceMapUi {
        val siteGeo = mergedGeo.firstOrNull { it.id == resolvedSiteId }
        val siteOpt = siteGeo?.let { geoToUi(it) }

        val locationEnabled = locationProvider.isLocationEnabled()
        val userPair =
            if (hasLocationPermission && locationEnabled) {
                locationProvider.captureCurrentLocation()
                    ?.let { Pair(it.latitude, it.longitude) }
            } else {
                null
            }

        return AttendanceMapComposer.compose(
            site = siteOpt,
            userLat = userPair?.first,
            userLng = userPair?.second,
            hasLocationPermission = hasLocationPermission,
            locationEnabled = locationEnabled
        )
    }

    /**
     * Satu titik → otomatis; banyak titik → preferensi lokal, lalu rekaman hari ini, lalu pertama.
     */
    private fun resolveAttendanceSiteId(
        sites: List<AttendanceSiteGeo>,
        recordSiteId: Int?
    ): Int? {
        if (sites.isEmpty()) return null
        if (sites.size == 1) return sites.first().id

        val saved = pref.getInt(PreferenceClass.KEY_SELECTED_ATTENDANCE_SITE_ID, -1)
        if (saved > 0 && sites.any { it.id == saved }) return saved

        if (recordSiteId != null && sites.any { it.id == recordSiteId }) return recordSiteId

        return sites.first().id
    }

    private fun mapDailyReasonToMessage(reasonCode: String?, reasonDetail: String?): String {
        if (!reasonDetail.isNullOrBlank()) return reasonDetail

        return when (reasonCode) {
            "WIFI_NOT_CONNECTED" -> "Perangkat belum terhubung ke Wi-Fi sekolah."
            "WIFI_NOT_MATCHED" -> "Wi-Fi yang digunakan tidak sesuai."
            "OUT_OF_RADIUS" -> "Anda berada di luar area absensi sekolah."
            "ACADEMIC_EVENT_BLOCK" -> "Absensi ditutup karena kalender akademik."
            "MANUAL_STATUS_EXISTS" ->
                "Status hari ini sudah diatur oleh sekolah (izin/sakit/dispensasi)."
            "CHECK_IN_NOT_OPEN_YET", "CHECK_OUT_NOT_OPEN_YET" -> "Belum dalam jam absensi."
            "CHECK_IN_CLOSED", "CHECK_OUT_CLOSED" -> "Jam absensi telah berakhir."
            "ALREADY_CHECKED_IN", "ALREADY_CHECKED_OUT" -> "Absensi sudah tercatat."
            "NO_CHECK_IN" -> "Lakukan absen masuk terlebih dahulu."
            "NO_ACTIVE_CLASS", "PROFILE_NOT_FOUND" ->
                "Data kelas atau profil tidak lengkap. Hubungi admin atau periksa akun Anda."
            "MOCK_LOCATION_DETECTED" -> "Lokasi palsu terdeteksi."
            "SITE_NOT_ACTIVE" -> "Titik absensi sedang tidak aktif."
            "LATE_CHECK_IN" -> "Check-in setelah batas hadir tepat waktu."
            else -> reasonDetail ?: "Absensi ditolak sistem. Silakan coba lagi."
        }
    }

    private fun mapLegacyReasonToMessage(reasonCode: String?, reasonDetail: String?): String {
        if (!reasonDetail.isNullOrBlank()) return reasonDetail

        return when (reasonCode) {
            "WIFI_NOT_CONNECTED" -> "Perangkat belum terhubung ke Wi-Fi sekolah."
            "WIFI_NOT_MATCHED" -> "Wi-Fi yang digunakan tidak sesuai."
            "OUT_OF_RADIUS" -> "Anda berada di luar area absensi sekolah."
            "ACADEMIC_EVENT_BLOCK" -> "Absensi ditutup karena kalender akademik."
            "NO_ACTIVE_SCHEDULE" -> "Tidak ada jadwal aktif pada waktu ini."
            "MOCK_LOCATION_DETECTED" -> "Lokasi palsu terdeteksi."
            "SITE_NOT_ACTIVE" -> "Titik absensi sedang tidak aktif."
            "NO_ACTIVE_CLASS" -> "Tidak ada kelas aktif."
            "PROFILE_NOT_FOUND" -> "Profil absensi tidak ditemukan."
            else -> reasonDetail ?: "Absensi ditolak sistem. Silakan coba lagi."
        }
    }
}
