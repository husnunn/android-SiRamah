package com.gridy.rohmahapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gridy.rohmahapp.data.local.entity.ScheduleCacheEntity

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_cache WHERE cache_key = :key LIMIT 1")
    suspend fun getByKey(key: String): ScheduleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ScheduleCacheEntity)
}
