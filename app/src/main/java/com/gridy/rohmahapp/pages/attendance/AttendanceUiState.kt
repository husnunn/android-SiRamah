package com.gridy.rohmahapp.pages.attendance


import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttendanceUiState(
    val isLoading: Boolean = false,
    val locationStatusText: String = "Memeriksa kondisi perangkat...",
    val checkInHistoryText: String = "Belum Presensi Masuk",
    val checkOutHistoryText: String = "Belum Presensi Pulang",
    val canCheckIn: Boolean = false,
    val canCheckOut: Boolean = false,
    val toastMessage: String? = null
)