// File: /app/src/main/java/com/gridy/rohmahapp/utils/DailyAttendanceTimeUtil.kt
package com.gridy.rohmahapp.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DailyAttendanceTimeUtil {

    private val schoolZone = ZoneId.of("Asia/Jakarta")
    private val outputFormatter = DateTimeFormatter.ofPattern("HH:mm 'WIB'", Locale("id", "ID"))
    private val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun toDisplayTime(value: String?): String {
        if (value.isNullOrBlank()) return "-"

        return try {
            when {
                // kalau backend nanti sudah benar kirim +07:00
                Regex(""".*[+-]\d{2}:\d{2}$""").matches(value) -> {
                    OffsetDateTime.parse(value)
                        .atZoneSameInstant(schoolZone)
                        .format(outputFormatter)
                }

                // workaround sementara:
                // backend kirim jam lokal sekolah tapi suffix-nya Z
                value.endsWith("Z") -> {
                    val normalized = value
                        .removeSuffix("Z")
                        .substringBefore(".")
                    LocalDateTime.parse(normalized, localFormatter)
                        .format(outputFormatter)
                }

                else -> {
                    val normalized = value.substringBefore(".")
                    LocalDateTime.parse(normalized, localFormatter)
                        .format(outputFormatter)
                }
            }
        } catch (e: Exception) {
            value
        }
    }
}