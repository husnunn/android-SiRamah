// File: /app/src/main/java/com/gridy/rohmahapp/repository/ProfileRepository.kt
package com.gridy.rohmahapp.repository

import com.gridy.rohmahapp.BuildConfig
import com.google.gson.Gson
import com.gridy.rohmahapp.api.ApiException
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.data.local.dao.ProfileDao
import com.gridy.rohmahapp.data.local.entity.ProfileCacheEntity
import com.gridy.rohmahapp.data.model.AttendanceSiteGeo
import com.gridy.rohmahapp.data.model.MobileProfileResponse
import com.gridy.rohmahapp.data.model.MobileProfileData
import com.gridy.rohmahapp.data.model.ProfileUi
import com.gridy.rohmahapp.data.model.StudentProfileResponse
import com.gridy.rohmahapp.data.model.TeacherProfileResponse
import com.gridy.rohmahapp.data.model.TeacherSubjectProfile
import com.gridy.rohmahapp.data.model.UpdatePasswordRequest
import com.gridy.rohmahapp.utils.DateTimeUtil
import com.gridy.rohmahapp.utils.PreferenceClass
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException

class ProfileRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val profileDao: ProfileDao,
) {

    private val gson = Gson()

    private fun buildTeacherRoleLabel(subjects: List<TeacherSubjectProfile>): String {
        val firstSubject = subjects.firstOrNull()?.name
        return if (firstSubject.isNullOrBlank()) "Guru" else "Guru • $firstSubject"
    }

    suspend fun getMyProfile(forceRemoteRefresh: Boolean): ProfileUi {
        val role = pref.getString(PreferenceClass.KEY_USER_ROLE)

        if (!forceRemoteRefresh) {
            val row = profileDao.getProfileRow()
            if (row != null && row.userRole == role) {
                profileUiFromCachedJson(role, row.payloadJson)?.let { return it }
            }
        }

        return try {
            when (role) {
                "student", "teacher" -> loadProfileForUserRole(role)
                else -> fallbackProfileUi(role)
            }
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException) throw e
            val row = profileDao.getProfileRow()
            if (row != null && row.userRole == role) {
                profileUiFromCachedJson(role, row.payloadJson) ?: fallbackProfileUi(role)
            } else {
                fallbackProfileUi(role)
            }
        }
    }

    private fun absoluteApiUrl(pathOrUrl: String?): String? {
        val raw = pathOrUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true)
        ) {
            return raw
        }
        val base = BuildConfig.API_URL.trimEnd('/')
        return "$base/${raw.trimStart('/')}"
    }

    private fun isMobileProfileDataUsable(d: MobileProfileData): Boolean {
        if (!d.name.isNullOrBlank()) return true
        if (!d.full_name.isNullOrBlank() || !d.fullName.isNullOrBlank()) return true
        if (d.profile != null && !d.profile.full_name.isNullOrBlank()) return true
        if (!d.nis.isNullOrBlank() || !d.nisn.isNullOrBlank() || !d.nip.isNullOrBlank()) return true
        if (d.profile != null && (
                !d.profile.nis.isNullOrBlank() ||
                    !d.profile.nisn.isNullOrBlank() ||
                    !d.profile.nip.isNullOrBlank()
                )
        ) {
            return true
        }
        if (!d.email.isNullOrBlank()) return true
        if (d.user != null && (
                !d.user.username.isNullOrBlank() ||
                    !d.user.email.isNullOrBlank()
                )
        ) {
            return true
        }
        if (d.current_class != null) return true
        if (d.class_obj != null) return true
        if (!d.class_name.isNullOrBlank() || !d.className.isNullOrBlank()) return true
        if (!d.school_name.isNullOrBlank() || !d.school.isNullOrBlank()) return true
        if (!d.profile_photo_url.isNullOrBlank() || !d.photo_url.isNullOrBlank()) return true
        if (!d.extension?.profile_photo_url.isNullOrBlank()) return true
        return false
    }

    private suspend fun loadProfileForUserRole(role: String): ProfileUi {
        try {
            val mobile = apiService.getMobileProfile()
            val data = mobile.data
            if (data != null && isMobileProfileDataUsable(data)) {
                persistProfileCache(role, gson.toJson(mobile))
                cacheAttendanceSites(data.attendance_sites)
                return mapMobileProfile(mobile, role)
            }
        } catch (e: ApiException) {
            if (e.responseCode != 404) throw e
        }
        return when (role) {
            "student" -> {
                val response = apiService.getStudentProfile()
                persistProfileCache(role, gson.toJson(response))
                cacheAttendanceSites(response.attendance_sites)
                mapStudentProfile(response)
            }
            "teacher" -> {
                val response = apiService.getTeacherProfile()
                persistProfileCache(role, gson.toJson(response))
                cacheAttendanceSites(response.attendance_sites)
                mapTeacherProfile(response)
            }
            else -> fallbackProfileUi(role)
        }
    }

    suspend fun updateProfilePhoto(part: MultipartBody.Part) {
        apiService.updateProfilePhoto(part)
    }

    suspend fun updatePassword(request: UpdatePasswordRequest) {
        apiService.updateProfilePassword(request)
    }

    private suspend fun persistProfileCache(role: String, json: String) {
        profileDao.upsertProfile(
            ProfileCacheEntity(
                id = 1,
                userRole = role,
                payloadJson = json,
                cachedAt = DateTimeUtil.nowIsoLocal(),
            )
        )
    }

    private fun profileUiFromCachedJson(role: String, json: String): ProfileUi? {
        val mobile = runCatching {
            gson.fromJson(json, MobileProfileResponse::class.java)
        }.getOrNull()
        if (mobile?.data != null) {
            return mapMobileProfile(mobile, role)
        }
        return runCatching {
            when (role) {
                "student" -> mapStudentProfile(
                    gson.fromJson(json, StudentProfileResponse::class.java),
                )
                "teacher" -> mapTeacherProfile(
                    gson.fromJson(json, TeacherProfileResponse::class.java),
                )
                else -> null
            }
        }.getOrNull()
    }

    private fun mapMobileProfile(response: MobileProfileResponse, role: String): ProfileUi {
        val d = response.data ?: return fallbackProfileUi(role)
        val normalized = d.role?.lowercase().orEmpty()
        val roleType = when {
            normalized.contains("student") || normalized.contains("siswa") -> "Siswa"
            normalized.contains("teacher") || normalized.contains("guru") -> "Guru"
            role == "student" -> "Siswa"
            role == "teacher" -> "Guru"
            else ->
                d.role?.replaceFirstChar { c -> c.titlecase() }
                    ?: if (role == "student") "Siswa" else "Guru"
        }
        val subtitle = d.role_subtitle?.takeIf { it.isNotBlank() } ?: roleType
        val ident = listOfNotNull(d.identity_id, d.nis, d.nisn, d.nip)
            .firstOrNull { !it.isNullOrBlank() }
            ?: listOfNotNull(d.profile?.nis, d.profile?.nisn, d.profile?.nip)
            .firstOrNull { !it.isNullOrBlank() } ?: "-"
        val displayName =
            d.name?.takeIf { it.isNotBlank() }
                ?: d.profile?.full_name?.takeIf { it.isNotBlank() }
                ?: d.full_name?.takeIf { it.isNotBlank() }
                ?: d.fullName?.takeIf { it.isNotBlank() }
                ?: pref.getString(PreferenceClass.KEY_USER_NAME, "Pengguna")
        val displayEmail =
            d.email?.takeIf { it.isNotBlank() }
                ?: d.user?.email?.takeIf { it.isNotBlank() }
                ?: d.user?.username?.takeIf { it.isNotBlank() }
                ?: "-"
        val school =
            d.school_name?.takeIf { it.isNotBlank() }
                ?: d.school?.takeIf { it.isNotBlank() }
        val klass =
            d.class_name?.takeIf { it.isNotBlank() }
                ?: d.className?.takeIf { it.isNotBlank() }
                ?: d.class_obj?.name?.takeIf { it.isNotBlank() }
                ?: d.current_class?.name?.takeIf { it.isNotBlank() }
        val photoRaw = d.profile_photo_url ?: d.extension?.profile_photo_url ?: d.photo_url
        return ProfileUi(
            name = displayName,
            roleLabel = subtitle,
            email = displayEmail,
            userIdLabel = ident,
            photoUrl = absoluteApiUrl(photoRaw),
            schoolName = school,
            className = klass,
            roleTypeLabel = roleType,
        )
    }

    private fun mapStudentProfile(response: StudentProfileResponse): ProfileUi {
        val data = response.data
        val className = data.current_class?.name?.takeIf { it.isNotBlank() }
        return ProfileUi(
            name = data.full_name ?: "Siswa",
            roleLabel = buildStudentRoleLabel(
                className = className,
                level = data.current_class?.level,
            ),
            email = data.user.username,
            userIdLabel = data.nis ?: data.nisn ?: "-",
            photoUrl = absoluteApiUrl(data.photo_url),
            schoolName = null,
            className = className,
            roleTypeLabel = "Siswa",
        )
    }

    private fun mapTeacherProfile(response: TeacherProfileResponse): ProfileUi {
        val data = response.data
        return ProfileUi(
            name = data.full_name ?: "Guru",
            roleLabel = buildTeacherRoleLabel(data.subjects),
            email = data.user.email ?: data.user.username,
            userIdLabel = data.nip ?: "-",
            photoUrl = absoluteApiUrl(data.photo_url),
            schoolName = null,
            className = null,
            roleTypeLabel = "Guru",
        )
    }

    private fun fallbackProfileUi(role: String): ProfileUi =
        ProfileUi(
            name = pref.getString(PreferenceClass.KEY_USER_NAME, "Pengguna"),
            roleLabel =
                when (role) {
                    "student", "teacher" ->
                        "Data belum tersedia. Hubungkan internet untuk memuat data pertama kali."

                    else -> "-"
                },
            email = pref.getString(PreferenceClass.KEY_USERNAME, "-"),
            userIdLabel = "-",
            photoUrl = null,
            schoolName = null,
            className = null,
            roleTypeLabel =
                when (role) {
                    "student" -> "Siswa"
                    "teacher" -> "Guru"
                    else -> null
                },
        )

    private fun buildStudentRoleLabel(className: String?, level: String?): String {
        val parts = mutableListOf<String>()
        parts.add("Siswa")

        if (!className.isNullOrBlank()) {
            parts.add(className)
        }

        if (!level.isNullOrBlank()) {
            parts.add(level)
        }

        return parts.joinToString(" • ")
    }

    private fun cacheAttendanceSites(sites: List<AttendanceSiteGeo>?) {
        if (sites.isNullOrEmpty()) return
        pref.putString(
            PreferenceClass.KEY_ATTENDANCE_SITES_JSON_CACHE,
            gson.toJson(sites),
        )
    }
}
