package com.gridy.rohmahapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gridy.rohmahapp.data.local.entity.ProfileCacheEntity

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile_cache WHERE id = 1 LIMIT 1")
    suspend fun getProfileRow(): ProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(row: ProfileCacheEntity)
}
