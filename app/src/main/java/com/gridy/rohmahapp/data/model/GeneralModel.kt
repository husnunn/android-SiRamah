package com.gridy.rohmahapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NearestScheduleUi(
    val subjectName: String,
    val teacherName: String,
    val startTime: String,
    val room: String
)

@JsonClass(generateAdapter = true)
data class ProfileUi(
    val name: String,
    val roleLabel: String,
    val email: String,
    val userIdLabel: String,
    val photoUrl: String? = null,
    val schoolName: String? = null,
    val className: String? = null,
    val roleTypeLabel: String? = null,
)