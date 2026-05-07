package com.gridy.rohmahapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gridy.rohmahapp.data.local.entity.AttendancePolicyEntity
import com.gridy.rohmahapp.data.local.entity.AttendanceSiteEntity
import com.gridy.rohmahapp.data.local.entity.AttendanceTodayEntity
import com.gridy.rohmahapp.data.local.entity.DailyOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_sites LIMIT 1")
    fun observeSite(): Flow<AttendanceSiteEntity?>

    @Query("SELECT * FROM attendance_policies LIMIT 1")
    fun observePolicy(): Flow<AttendancePolicyEntity?>

    @Query("SELECT * FROM daily_overrides WHERE date = :date LIMIT 1")
    fun observeOverride(date: String): Flow<DailyOverrideEntity?>

    @Query("SELECT * FROM attendance_today WHERE date = :date LIMIT 1")
    fun observeAttendanceToday(date: String): Flow<AttendanceTodayEntity?>

    @Query("SELECT * FROM attendance_today WHERE date = :date LIMIT 1")
    suspend fun getAttendanceToday(date: String): AttendanceTodayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSite(site: AttendanceSiteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicy(policy: AttendancePolicyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: DailyOverrideEntity)

    @Query("DELETE FROM daily_overrides WHERE date = :date")
    suspend fun deleteOverrideByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceToday(attendance: AttendanceTodayEntity)

    @Query("DELETE FROM attendance_today WHERE date != :today")
    suspend fun deleteOldAttendanceToday(today: String)
}