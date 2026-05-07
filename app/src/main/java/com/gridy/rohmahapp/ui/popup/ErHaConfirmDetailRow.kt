package com.gridy.rohmahapp.ui.popup

import java.io.Serializable

/** Satu baris detail pada dialog konfirmasi (mis. lokasi, waktu). */
data class ErHaConfirmDetailRow(
    val label: String,
    val primaryText: String,
    val secondaryText: String? = null,
    /** Pass `0` untuk ikon default per baris. */
    val iconResId: Int = 0
) : Serializable
