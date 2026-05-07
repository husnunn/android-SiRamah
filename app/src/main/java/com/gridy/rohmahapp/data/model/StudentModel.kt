// File: /app/src/main/java/com/gridy/rohmahapp/data/model/StudentProfileResponse.kt
package com.gridy.rohmahapp.data.model

import com.gridy.rohmahapp.api.moshi.FlexibleString
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentProfileResponse(
    val data: StudentProfileData,
    val attendance_sites: List<AttendanceSiteGeo>?
)

@JsonClass(generateAdapter = true)
data class StudentProfileData(
    val id: Int,
    val nis: String?,
    val nisn: String?,
    val full_name: String?,
    val gender: String?,
    val birth_date: String?,
    val birth_place: String?,
    val phone: String?,
    val address: String?,
    val parent_name: String?,
    val parent_phone: String?,
    val photo_url: String?,
    val current_class: StudentCurrentClass?,
    val user: StudentUserProfile
)

@JsonClass(generateAdapter = true)
data class StudentCurrentClass(
    val id: Int,
    @FlexibleString val name: String? = null,
    @FlexibleString val level: String? = null,
    @FlexibleString val homeroom_teacher: String? = null,
)

@JsonClass(generateAdapter = true)
data class StudentUserProfile(
    val id: Int,
    val username: String,
    val is_active: Boolean
)