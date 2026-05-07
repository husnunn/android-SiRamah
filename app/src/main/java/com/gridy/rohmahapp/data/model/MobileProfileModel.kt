package com.gridy.rohmahapp.data.model

import com.gridy.rohmahapp.api.moshi.FlexibleString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MobileProfileResponse(
    val success: Boolean? = null,
    /** `message` tidak dimodelkan: backend kadang objek/array → crash Moshi bila pakai String. */
    val data: MobileProfileData? = null,
)

/**
 * Gabungan field umum API mobile + struktur mirip [StudentProfileData]/guru agar tidak kosong
 * bila backend mengirim `photo_url`, `current_class`, `user`, atau camelCase.
 */
@JsonClass(generateAdapter = true)
data class MobileProfileData(
    @FlexibleString val name: String? = null,
    @FlexibleString val full_name: String? = null,
    @FlexibleString val fullName: String? = null,
    @FlexibleString val email: String? = null,
    @FlexibleString val role: String? = null,
    @FlexibleString val school_name: String? = null,
    @FlexibleString val school: String? = null,
    @Json(name = "class") val class_obj: MobileProfileNamedRef? = null,
    @FlexibleString val class_name: String? = null,
    @FlexibleString val className: String? = null,
    @FlexibleString val profile_photo_url: String? = null,
    @FlexibleString val photo_url: String? = null,
    val profile: MobileProfilePerson? = null,
    val extension: MobileProfileExtension? = null,
    @FlexibleString val identity_id: String? = null,
    @FlexibleString val nis: String? = null,
    @FlexibleString val nisn: String? = null,
    @FlexibleString val nip: String? = null,
    @FlexibleString val role_subtitle: String? = null,
    val current_class: StudentCurrentClass? = null,
    val user: MobileProfileUserRef? = null,
    val attendance_sites: List<AttendanceSiteGeo>? = null,
)

@JsonClass(generateAdapter = true)
data class MobileProfileUserRef(
    val id: Int? = null,
    @FlexibleString val username: String? = null,
    @FlexibleString val email: String? = null,
)

@JsonClass(generateAdapter = true)
data class MobileProfileNamedRef(
    val id: Int? = null,
    @FlexibleString val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class MobileProfilePerson(
    val id: Int? = null,
    @FlexibleString val full_name: String? = null,
    @FlexibleString val nis: String? = null,
    @FlexibleString val nisn: String? = null,
    @FlexibleString val nip: String? = null,
)

@JsonClass(generateAdapter = true)
data class MobileProfileExtension(
    @FlexibleString val profile_photo_url: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfilePhotoUpdateResponse(
    val success: Boolean? = null,
    val data: ProfilePhotoData? = null,
)

@JsonClass(generateAdapter = true)
data class ProfilePhotoData(
    @FlexibleString val profile_photo_url: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdatePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String,
)

@JsonClass(generateAdapter = true)
data class ApiBasicResponse(
    val success: Boolean? = null,
)
