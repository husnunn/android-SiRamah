package com.gridy.rohmahapp.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AttendanceTimeFormatter {

    private val schoolZone = ZoneId.of("Asia/Jakarta")
    private val outputFormatter = DateTimeFormatter.ofPattern("HH:mm 'WIB'", Locale("id", "ID"))
    private val localInputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * Temporary compatibility:
     * - If backend sends proper offset (+07:00 / Z that is really UTC), parse as instant.
     * - If backend sends wall time but still ends with Z, treat it as local school wall time.
     */
    fun toDisplayTime(value: String?): String {
        if (value.isNullOrBlank()) return "-"

        return try {
            when {
                // proper offset, e.g. 2026-04-29T08:48:05+07:00
                Regex(""".*[+-]\d{2}:\d{2}$""").matches(value) -> {
                    OffsetDateTime.parse(value)
                        .atZoneSameInstant(schoolZone)
                        .format(outputFormatter)
                }

                // temporary workaround for backend that sends wall time + Z
                value.endsWith("Z") -> {
                    val normalized = value
                        .removeSuffix("Z")
                        .substringBefore(".")
                    LocalDateTime.parse(normalized, localInputFormatter)
                        .format(outputFormatter)
                }

                else -> {
                    val normalized = value.substringBefore(".")
                    LocalDateTime.parse(normalized, localInputFormatter)
                        .format(outputFormatter)
                }
            }
        } catch (e: Exception) {
            value
        }
    }
}