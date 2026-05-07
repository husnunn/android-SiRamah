package com.gridy.rohmahapp.api

import com.gridy.rohmahapp.data.model.AttendanceResponse
import com.gridy.rohmahapp.data.model.AttendanceSubmitRequest
import com.gridy.rohmahapp.data.model.AttendanceTodayResponse
import com.gridy.rohmahapp.data.model.DailyAttendanceCheckInResponse
import com.gridy.rohmahapp.data.model.DailyAttendanceCheckOutResponse
import com.gridy.rohmahapp.data.model.DailyAttendanceSubmitRequest
import com.gridy.rohmahapp.data.model.DailyAttendanceTodayResponse
import com.gridy.rohmahapp.data.model.ApiBasicResponse
import com.gridy.rohmahapp.data.model.LoginRequest
import com.gridy.rohmahapp.data.model.LoginResponse
import com.gridy.rohmahapp.data.model.MobileProfileResponse
import com.gridy.rohmahapp.data.model.ProfilePhotoUpdateResponse
import com.gridy.rohmahapp.data.model.RegisterDeviceTokenRequest
import com.gridy.rohmahapp.data.model.StudentProfileResponse
import com.gridy.rohmahapp.data.model.UpdatePasswordRequest
import com.gridy.rohmahapp.data.model.StudentScheduleResponse
import com.gridy.rohmahapp.data.model.TeacherProfileResponse
import com.gridy.rohmahapp.data.model.TeacherScheduleByDayResponse
import com.gridy.rohmahapp.data.model.TeacherScheduleResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    @POST("api/v1/student/login")
    suspend fun loginStudent(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/teacher/login")
    suspend fun loginTeacher(@Body request: LoginRequest): LoginResponse

    @GET("api/v1/student/me")
    suspend fun getStudentProfile(): StudentProfileResponse

    @GET("api/v1/teacher/me")
    suspend fun getTeacherProfile(): TeacherProfileResponse

    @GET("api/mobile/profile")
    suspend fun getMobileProfile(): MobileProfileResponse

    @Multipart
    @POST("api/mobile/profile/photo")
    suspend fun updateProfilePhoto(
        @Part photo: MultipartBody.Part,
    ): ProfilePhotoUpdateResponse

    @POST("api/mobile/profile/password")
    suspend fun updateProfilePassword(
        @Body body: UpdatePasswordRequest,
    ): ApiBasicResponse

    @POST("api/mobile/device-token")
    suspend fun registerDeviceToken(
        @Body request: RegisterDeviceTokenRequest,
    ): ApiBasicResponse

    @POST("api/v1/student/logout")
    suspend fun logoutStudent(): Map<String, String>

    @POST("api/v1/teacher/logout")
    suspend fun logoutTeacher(): Map<String, String>

    @GET("api/v1/student/schedule")
    suspend fun getStudentSchedule(
        @Query("semester") semester: String? = null,
        @Query("day") day: Int? = null,
        /** Tanggal kalender (YYYY-MM-DD) di timezone sekolah — SpecApi.md */
        @Query("date") date: String? = null
    ): StudentScheduleResponse

    @GET("api/v1/teacher/schedule")
    suspend fun getTeacherSchedule(
        @Query("semester") semester: String? = null,
        @Query("day") day: Int? = null,
        @Query("date") date: String? = null
    ): TeacherScheduleResponse

    /** Jadwal guru dikelompokkan per hari (hari tanpa slot tidak dikembalikan). */
    @GET("api/v1/teacher/schedule/by-day")
    suspend fun getTeacherScheduleByDay(
        @Query("semester") semester: Int? = null,
        @Query("date") date: String? = null,
    ): TeacherScheduleByDayResponse

    @Headers("Accept: */*")
    @GET
    suspend fun download(@Url url: String): ResponseBody


//    ABSENSI — siswa: dua jalur (SpecApi.md). UI masuk/pulang memakai daily-attendance.

    /** Legacy — berbasis jadwal mapel (`attendance_records`, window ± menit dari start_time). */
    @POST("api/v1/student/attendance/check-in")
    suspend fun studentLegacyAttendanceCheckIn(
        @Body request: AttendanceSubmitRequest
    ): AttendanceResponse

    @POST("api/v1/student/attendance/check-out")
    suspend fun studentLegacyAttendanceCheckOut(
        @Body request: AttendanceSubmitRequest
    ): AttendanceResponse

    @GET("api/v1/student/attendance/today")
    suspend fun studentLegacyAttendanceToday(): AttendanceTodayResponse

    /** Absensi harian sekolah — tanpa `attendance_type` di body; jam dari config sekolah. */
    @POST("api/v1/student/daily-attendance/check-in")
    suspend fun studentDailyCheckIn(
        @Body request: DailyAttendanceSubmitRequest
    ): DailyAttendanceCheckInResponse

    @POST("api/v1/student/daily-attendance/check-out")
    suspend fun studentDailyCheckOut(
        @Body request: DailyAttendanceSubmitRequest
    ): DailyAttendanceCheckOutResponse

    @GET("api/v1/student/daily-attendance/today")
    suspend fun studentDailyAttendanceToday(): DailyAttendanceTodayResponse

//    ABSENSI — guru (endpoint berbasis jadwal / backward compatible)


    @POST("api/v1/teacher/attendance/check-in")
    suspend fun teacherCheckIn(
        @Body request: AttendanceSubmitRequest
    ): AttendanceResponse

    @POST("api/v1/teacher/attendance/check-out")
    suspend fun teacherCheckOut(
        @Body request: AttendanceSubmitRequest
    ): AttendanceResponse

    @GET("api/v1/teacher/attendance/today")
    suspend fun teacherAttendanceToday(): AttendanceTodayResponse

}