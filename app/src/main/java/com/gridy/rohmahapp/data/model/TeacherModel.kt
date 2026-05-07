// File: /app/src/main/java/com/gridy/rohmahapp/data/model/TeacherProfileResponse.kt
package com.gridy.rohmahapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TeacherProfileResponse(
    val data: TeacherProfileData,
    val attendance_sites: List<AttendanceSiteGeo>?
)

@JsonClass(generateAdapter = true)
data class TeacherProfileData(
    val id: Int,
    val nip: String?,
    val full_name: String?,
    val gender: String?,
    val phone: String?,
    val address: String?,
    val photo_url: String?,
    val subjects: List<TeacherSubjectProfile> = emptyList(),
    val user: TeacherUserProfile
)

@JsonClass(generateAdapter = true)
data class TeacherSubjectProfile(
    val id: Int,
    val code: String?,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class TeacherUserProfile(
    val id: Int,
    val username: String,
    val email: String?,
    val is_active: Boolean
)