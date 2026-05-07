package com.gridy.rohmahapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gridy.rohmahapp.data.local.dao.AttendanceDao
import com.gridy.rohmahapp.data.local.dao.ProfileDao
import com.gridy.rohmahapp.data.local.dao.ScheduleDao
import com.gridy.rohmahapp.data.local.entity.AttendancePolicyEntity
import com.gridy.rohmahapp.data.local.entity.AttendanceSiteEntity
import com.gridy.rohmahapp.data.local.entity.AttendanceTodayEntity
import com.gridy.rohmahapp.data.local.entity.DailyOverrideEntity
import com.gridy.rohmahapp.data.local.entity.ProfileCacheEntity
import com.gridy.rohmahapp.data.local.entity.ScheduleCacheEntity

@Database(
    entities = [
        AttendanceSiteEntity::class,
        AttendancePolicyEntity::class,
        DailyOverrideEntity::class,
        AttendanceTodayEntity::class,
        ProfileCacheEntity::class,
        ScheduleCacheEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class RohMahAppDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    abstract fun profileDao(): ProfileDao

    abstract fun scheduleDao(): ScheduleDao

    companion object {

        fun create(applicationContext: Context): RohMahAppDatabase =
            Room.databaseBuilder(applicationContext, RohMahAppDatabase::class.java, "rohmah_local.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
