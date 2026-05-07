// File: /app/src/main/java/com/gridy/rohmahapp/data/model/StudentScheduleResponse.kt
package com.gridy.rohmahapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentScheduleResponse(
    val data: List<StudentScheduleItem>
)

@JsonClass(generateAdapter = true)
data class StudentScheduleItem(
    val id: Int,
    val day: String,
    val day_of_week: Int,
    val start_time: String,
    val end_time: String,
    val room: String?,
    val semester: String?,
    val notes: String?,
    val subject: StudentScheduleSubject,
    val teacher: StudentScheduleTeacher,
    val school_year: String?
)

@JsonClass(generateAdapter = true)
data class StudentScheduleSubject(
    val id: Int,
    val code: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class StudentScheduleTeacher(
    val id: Int,
    val full_name: String,
    val nip: String?
)