package com.gridy.rohmahapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_cache")
data class ProfileCacheEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "user_role") val userRole: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: String,
)

@Entity(tableName = "schedule_cache")
data class ScheduleCacheEntity(
    @PrimaryKey @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "user_role") val userRole: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: String,
)
