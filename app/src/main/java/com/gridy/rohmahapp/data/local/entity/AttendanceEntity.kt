package com.gridy.rohmahapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_sites")
data class AttendanceSiteEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val updatedAt: String?
)

@Entity(tableName = "attendance_policies")
data class AttendancePolicyEntity(
    @PrimaryKey val id: Int,
    val siteId: Int,
    val checkInStart: String,
    val checkInEnd: String,
    val checkOutStart: String,
    val checkOutEnd: String,
    val updatedAt: String?
)

@Entity(tableName = "daily_overrides")
data class DailyOverrideEntity(
    @PrimaryKey val date: String,
    val siteId: Int,
    val checkInStart: String?,
    val checkInEnd: String?,
    val checkOutStart: String?,
    val checkOutEnd: String?,
    val reason: String?
)

@Entity(tableName = "attendance_today")
data class AttendanceTodayEntity(
    @PrimaryKey val date: String,

    val checkInTime: String?,
    val checkOutTime: String?,
    val status: String,

    // Simpan response lengkap agar bisa dipakai ulang saat offline
    val rawJson: String?,

    // Waktu data ini disimpan ke lokal
    val cachedAt: String?,

    val synced: Boolean = true
)