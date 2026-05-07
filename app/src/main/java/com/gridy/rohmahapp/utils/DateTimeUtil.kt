// File: /app/src/main/java/com/gridy/rohmahapp/utils/DateTimeUtil.kt
package com.gridy.rohmahapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtil {

    private const val DATE_PATTERN_LOCAL = "yyyy-MM-dd"

    private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"

    /** Tanggal kalender hari ini zona Asia/Jakarta (format yyyy-MM-dd) — konsisten untuk kunci Row cache. */
    fun todayDate(): String {
        val formatter = SimpleDateFormat(DATE_PATTERN_LOCAL, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        return formatter.format(Date())
    }

    fun nowIsoLocal(): String {
        val formatter = SimpleDateFormat(ISO_PATTERN, Locale.US)
        return formatter.format(Date())
    }

    fun toDisplayTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "-"
        return try {
            val parser = SimpleDateFormat(ISO_PATTERN, Locale.US)
            val parsed = parser.parse(isoString) ?: return "-"
            val output = SimpleDateFormat("HH:mm 'WIB'", Locale("id", "ID"))
            output.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            output.format(parsed)
        } catch (e: Exception) {
            isoString
        }
    }
}