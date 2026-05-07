// File: /app/src/main/java/com/gridy/rohmahapp/data/model/TeacherScheduleResponse.kt
package com.gridy.rohmahapp.data.model

import com.gridy.rohmahapp.api.moshi.FlexibleString
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TeacherScheduleResponse(
    val data: List<TeacherScheduleItem>
)

/** Respons GET /api/v1/teacher/schedule/by-day — hanya hari dengan slot. */
@JsonClass(generateAdapter = true)
data class TeacherScheduleByDayResponse(
    val data: List<TeacherScheduleDayBucket>
)

@JsonClass(generateAdapter = true)
data class TeacherScheduleDayBucket(
    val day_of_week: Int,
    val day_name: String,
    val schedules: List<TeacherScheduleItem>
)

@JsonClass(generateAdapter = true)
data class TeacherScheduleItem(
    val id: Int,
    val day: String,
    val day_of_week: Int,
    val start_time: String,
    val end_time: String,
    val room: String?,
    @FlexibleString val semester: String? = null,
    val notes: String?,
    val subject: TeacherScheduleSubject,
    val `class`: TeacherScheduleClass,
    val school_year: String?
)

@JsonClass(generateAdapter = true)
data class TeacherScheduleSubject(
    val id: Int,
    @FlexibleString val code: String? = null,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TeacherScheduleClass(
    val id: Int,
    val name: String,
    val level: String
)
