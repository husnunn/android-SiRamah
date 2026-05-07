package com.gridy.rohmahapp.repository

import android.util.Log
import com.google.gson.Gson
import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.api.AppMoshi
import com.gridy.rohmahapp.data.local.dao.ScheduleDao
import com.gridy.rohmahapp.data.local.entity.ScheduleCacheEntity
import com.gridy.rohmahapp.data.model.NearestScheduleUi
import com.gridy.rohmahapp.data.model.StudentScheduleItem
import com.gridy.rohmahapp.data.model.StudentScheduleResponse
import com.gridy.rohmahapp.data.model.TeacherScheduleByDayResponse
import com.gridy.rohmahapp.data.model.TeacherScheduleItem
import com.gridy.rohmahapp.data.model.TeacherScheduleResponse
import com.gridy.rohmahapp.pages.schedule.ScheduleScreenData
import com.gridy.rohmahapp.pages.schedule.ScheduleUi
import com.gridy.rohmahapp.utils.DateTimeUtil
import com.gridy.rohmahapp.utils.PreferenceClass
import retrofit2.HttpException
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class ScheduleRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val scheduleDao: ScheduleDao,
) {

    private val gson = Gson()
    private val moshi = AppMoshi.build()
    private val teacherByDayAdapter = moshi.adapter(TeacherScheduleByDayResponse::class.java)

    suspend fun getNearestScheduleToday(forceRemoteRefresh: Boolean): NearestScheduleUi? {
        val role = pref.getString(PreferenceClass.KEY_USER_ROLE)
        val todayDow = currentDayOfWeekNumber()
        val todayDate = todayIsoDate()
        Log.d("ScheduleRepository", "role=$role, today=$todayDow")

        if (role != "student" && role != "teacher") return null

        val cacheKey = nearestCacheKey(role, todayDate)

        if (!forceRemoteRefresh) {
            val row = scheduleDao.getByKey(cacheKey)
            if (row != null && row.userRole == role) {
                return nearestFromStored(role, row.payloadJson, todayDow)
            }
            return null
        }

        return try {
            when (role) {
                "student" -> {
                    val response = apiService.getStudentSchedule(date = todayDate)
                    persistScheduleRow(cacheKey, role, gson.toJson(response))
                    mapStudentNearest(
                        response.data.filter { it.day_of_week == todayDow },
                    )
                }
                "teacher" -> {
                    val response = apiService.getTeacherSchedule(date = todayDate)
                    persistScheduleRow(cacheKey, role, gson.toJson(response))
                    mapTeacherNearest(
                        response.data.filter { it.day_of_week == todayDow },
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException) throw e
            val row = scheduleDao.getByKey(cacheKey)
            if (row != null && row.userRole == role) {
                nearestFromStored(role, row.payloadJson, todayDow)
            } else {
                null
            }
        }
    }

    suspend fun getScheduleScreen(
        semester: String? = null,
        day: Int? = null,
        date: String? = null,
        forceRemoteRefresh: Boolean,
    ): ScheduleScreenData {
        val role = pref.getString(PreferenceClass.KEY_USER_ROLE)
        if (role != "student" && role != "teacher") return ScheduleScreenData(emptyList())

        val emptyForRole = ScheduleScreenData(
            items = emptyList(),
            teacherTeachingDays = teacherMetaEmptyIfTeacher(role),
        )

        val filterDay = day ?: currentDayOfWeekNumber()
        val studentScheduleAnchorDate = if (role == "student") date ?: todayIsoDate() else null
        val cacheKey = scheduleListCacheKey(role, semester, day, date, studentScheduleAnchorDate)

        if (!forceRemoteRefresh) {
            val row = scheduleDao.getByKey(cacheKey)
            if (row != null && row.userRole == role) {
                return mapStoredScheduleScreen(role, row.payloadJson, filterDay)
            }
            return emptyForRole
        }

        return try {
            when (role) {
                "student" -> {
                    val anchor = studentScheduleAnchorDate!!
                    val response = apiService.getStudentSchedule(
                        semester = semester,
                        day = null,
                        date = anchor,
                    )
                    persistScheduleRow(cacheKey, role, gson.toJson(response))
                    ScheduleScreenData(mapStudentScheduleUiForDay(response, filterDay))
                }
                "teacher" -> {
                    val semesterInt = semester?.toIntOrNull()
                    val response = apiService.getTeacherScheduleByDay(
                        semester = semesterInt,
                        date = date,
                    )
                    persistScheduleRow(cacheKey, role, teacherByDayAdapter.toJson(response))
                    screenDataTeacherFromByDay(response, filterDay)
                }
                else -> emptyForRole
            }
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException) throw e
            val row = scheduleDao.getByKey(cacheKey)
            if (row != null && row.userRole == role) {
                mapStoredScheduleScreen(role, row.payloadJson, filterDay)
            } else {
                emptyForRole
            }
        }
    }

    private fun teacherMetaEmptyIfTeacher(role: String): List<Int>? =
        if (role == "teacher") emptyList() else null

    private suspend fun persistScheduleRow(cacheKey: String, role: String, json: String) {
        scheduleDao.upsert(
            ScheduleCacheEntity(
                cacheKey = cacheKey,
                userRole = role,
                payloadJson = json,
                cachedAt = DateTimeUtil.nowIsoLocal(),
            ),
        )
    }

    private fun nearestFromStored(role: String, json: String, todayDow: Int): NearestScheduleUi? {
        return runCatching {
            when (role) {
                "student" -> {
                    val r = gson.fromJson(json, StudentScheduleResponse::class.java)
                    mapStudentNearest(r.data.filter { it.day_of_week == todayDow })
                }
                "teacher" -> {
                    val r = gson.fromJson(json, TeacherScheduleResponse::class.java)
                    mapTeacherNearest(r.data.filter { it.day_of_week == todayDow })
                }
                else -> null
            }
        }.getOrNull()
    }

    /**
     * Cache daftar guru: JSON /schedule/by-day (Moshi).
     * Cache daftar siswa: JSON flat siswa (Gson).
     * Fallback gson flat guru jika ada sisa cache lama.
     */
    private fun mapStoredScheduleScreen(
        role: String,
        json: String,
        filterDay: Int,
    ): ScheduleScreenData =
        runCatching {
            when (role) {
                "student" -> {
                    val r = gson.fromJson(json, StudentScheduleResponse::class.java)
                    ScheduleScreenData(mapStudentScheduleUiForDay(r, filterDay))
                }
                "teacher" ->
                    decodeTeacherScheduleCache(json, filterDay)

                else -> ScheduleScreenData(emptyList())
            }
        }.getOrElse {
            when (role) {
                "teacher" -> ScheduleScreenData(emptyList(), teacherTeachingDays = emptyList())
                else -> ScheduleScreenData(emptyList())
            }
        }

    private fun decodeTeacherScheduleCache(json: String, filterDay: Int): ScheduleScreenData {
        teacherByDayAdapter.fromJson(json)?.let {
            return screenDataTeacherFromByDay(it, filterDay)
        }
        val legacy =
            runCatching { gson.fromJson(json, TeacherScheduleResponse::class.java) }.getOrNull()
                ?: return ScheduleScreenData(emptyList(), teacherTeachingDays = emptyList())
        val itemsFiltered = legacy.data
            .filter { it.day_of_week == filterDay }
            .map { teacherSlotToScheduleUi(it) }
            .sortedBy { parseTimeToMinutes(it.start_time) }

        val dayNumbers = legacy.data.map { it.day_of_week }.filter { it in 1..7 }.distinct().sorted()
        val titles = legacy.data.distinctBy { it.day_of_week }.associate {
            val n = it.day_of_week
            n to capitalizeDay(it.day.trim())
        }
        return ScheduleScreenData(
            items = itemsFiltered,
            teacherTeachingDays = dayNumbers,
            teacherDayNameByNumber = titles,
        )
    }

    private fun screenDataTeacherFromByDay(blob: TeacherScheduleByDayResponse, filterDay: Int): ScheduleScreenData {
        val dayNumbers = blob.data.map { it.day_of_week }.sorted()
        val titles = blob.data.associate { bucket ->
            bucket.day_of_week to bucket.day_name.trim()
        }
        val items =
            schedulesForTeachingDay(blob, filterDay)

        return ScheduleScreenData(
            items = items,
            teacherTeachingDays = dayNumbers,
            teacherDayNameByNumber = titles,
        )
    }

    private fun schedulesForTeachingDay(blob: TeacherScheduleByDayResponse, dow: Int): List<ScheduleUi> {
        val bucket = blob.data.firstOrNull { it.day_of_week == dow } ?: return emptyList()
        return bucket.schedules
            .map { teacherSlotToScheduleUi(it) }
            .sortedBy { parseTimeToMinutes(it.start_time) }
    }

    private fun capitalizeDay(day: String): String {
        val t = day.trim()
        if (t.isEmpty()) return ""
        return t.lowercase(Locale.ROOT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    private fun teacherSlotToScheduleUi(slot: TeacherScheduleItem): ScheduleUi {
        val code = slot.subject.code?.trim()?.takeIf { it.isNotEmpty() }
        return ScheduleUi(
            id = slot.id,
            day = slot.day.trim(),
            start_time = cleanTime(slot.start_time),
            end_time = cleanTime(slot.end_time),
            subject_name = slot.subject.name.trim(),
            teacher_or_class = "Kelas ${slot.`class`.name.trim()}",
            room = slot.room?.trim()?.takeIf { it.isNotEmpty() } ?: "-",
            semester = slot.semester?.trim()?.takeIf { it.isNotEmpty() } ?: "-",
            school_year = slot.school_year?.trim()?.takeIf { it.isNotEmpty() } ?: "-",
            subject_code_hint = code,
            notes = slot.notes?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun studentItemToScheduleUi(it: StudentScheduleItem): ScheduleUi =
        ScheduleUi(
            id = it.id,
            day = it.day,
            start_time = cleanTime(it.start_time),
            end_time = cleanTime(it.end_time),
            subject_name = it.subject.name,
            teacher_or_class = it.teacher.full_name,
            room = it.room ?: "-",
            semester = it.semester ?: "-",
            school_year = it.school_year ?: "-",
        )

    /**
     * Layar jadwal siswa: API mengembalikan **semua** slot untuk anchor [date] (tanpa query `day`);
     * filter per tab hari dilakukan di klien (spec student schedule).
     */
    private fun mapStudentScheduleUiForDay(response: StudentScheduleResponse, dow: Int): List<ScheduleUi> =
        response.data
            .filter { it.day_of_week == dow }
            .map { studentItemToScheduleUi(it) }
            .sortedBy { parseTimeToMinutes(it.start_time) }

    private fun todayIsoDate(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun mapStudentNearest(items: List<StudentScheduleItem>): NearestScheduleUi? {
        val selected = pickNearestStudentItem(items) ?: return null
        return NearestScheduleUi(
            subjectName = selected.subject.name,
            teacherName = selected.teacher.full_name,
            startTime = cleanTime(selected.start_time),
            room = selected.room ?: "-",
        )
    }

    private fun mapTeacherNearest(items: List<TeacherScheduleItem>): NearestScheduleUi? {
        val selected = pickNearestTeacherItem(items) ?: return null
        return NearestScheduleUi(
            subjectName = selected.subject.name,
            teacherName = selected.`class`.name,
            startTime = cleanTime(selected.start_time),
            room = selected.room ?: "-",
        )
    }

    /**
     * Jadwal terdekat hanya relevan untuk:
     * 1) mapel yang **sedang berlangsung** (start ≤ now &lt; end), atau
     * 2) mapel berikutnya yang **belum mulai** (now &lt; start).
     * Setelah semua slot hari ini selesai, hasilnya null (mis. sore setelah jam terakhir).
     */
    private inline fun <T> pickNearestRelevantScheduleItem(
        items: List<T>,
        crossinline startOf: (T) -> String,
        crossinline endOf: (T) -> String,
    ): T? {
        if (items.isEmpty()) return null
        val sorted = items.sortedBy { parseTimeToMinutes(startOf(it)) }
        val now = getCurrentMinutes()

        sorted.firstOrNull { item ->
            val start = parseTimeToMinutes(startOf(item))
            val endRaw = parseTimeToMinutes(endOf(item))
            val end = if (endRaw > start) endRaw else start
            now >= start && now < end
        }?.let { return it }

        sorted.firstOrNull { parseTimeToMinutes(startOf(it)) > now }?.let { return it }

        return null
    }

    private fun pickNearestStudentItem(items: List<StudentScheduleItem>): StudentScheduleItem? =
        pickNearestRelevantScheduleItem(
            items = items,
            startOf = { it.start_time },
            endOf = { it.end_time },
        )

    private fun pickNearestTeacherItem(items: List<TeacherScheduleItem>): TeacherScheduleItem? =
        pickNearestRelevantScheduleItem(
            items = items,
            startOf = { it.start_time },
            endOf = { it.end_time },
        )

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val clean = cleanTime(time)
            val parts = clean.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (hour * 60) + minute
        } catch (_: Exception) {
            0
        }
    }

    private fun cleanTime(value: String): String {
        return value.trim().take(5)
    }

    private fun getCurrentMinutes(): Int {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return (hour * 60) + minute
    }

    private fun currentDayOfWeekNumber(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    private fun nearestCacheKey(role: String, todayIso: String): String = "nearest|$role|$todayIso"

    /**
     * Siswa: satu entri cache per (semester, tanggal anchor), **bukan** per hari tab —
     * agar response penuh dari API bisa dipakai ulang saat ganti Senin/Selasa, dll.
     */
    private fun scheduleListCacheKey(
        role: String,
        semester: String?,
        day: Int?,
        date: String?,
        studentAnchorDate: String?,
    ): String {
        val semBucket =
            semester?.toIntOrNull()?.toString()
                ?: (semester?.trim()?.takeIf { it.isNotEmpty() } ?: "_")
        return when (role) {
            "teacher" ->
                "list|teacher|byday|$semBucket|${date ?: "_"}"
            "student" -> {
                val anchor = studentAnchorDate ?: todayIsoDate()
                "list|student|$semBucket|full|$anchor"
            }
            else ->
                "list|$role|$semBucket|${day?.toString() ?: "_"}|${date ?: "_"}"
        }
    }
}
