package com.gridy.rohmahapp.pages.schedule

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleUi(
    val id: Int,
    val day: String,
    val start_time: String,
    val end_time: String,
    val subject_name: String,
    val teacher_or_class: String,
    val room: String,
    val semester: String,
    val school_year: String,
    /** Opsional — kode mapel (mis. MTK) untuk tampilan guru. */
    val subject_code_hint: String? = null,
    /** Catatan slot dari backend (guru); null untuk siswa. */
    val notes: String? = null,
)

/**
 * Sekali muat untuk layar jadwal: daftar kartu + (jika guru) meta chip hari dari /by-day.
 */
data class ScheduleScreenData(
    val items: List<ScheduleUi>,
    /** Hari (1–6) yang punya jadwal; null artinya mode siswa (tampilkan pemilih penuh). */
    val teacherTeachingDays: List<Int>? = null,
    val teacherDayNameByNumber: Map<Int, String>? = null,
)