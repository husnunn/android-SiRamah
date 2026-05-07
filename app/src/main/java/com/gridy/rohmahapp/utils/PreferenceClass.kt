// File: /app/src/main/java/com/gridy/rohmahapp/utils/PreferenceClass.kt
package com.gridy.rohmahapp.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceClass @Inject constructor(
    context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "rohmah_app_pref"

        const val KEY_USER_TOKEN = "user_token"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_FCM_TOKEN = "fcm_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USERNAME = "username"

        /** Cache JSON array titik absensi dari `/me` — fallback jika `/today` kosong (SpecApi.md). */
        const val KEY_ATTENDANCE_SITES_JSON_CACHE = "attendance_sites_json_cache"
        /** Preferensi `attendance_site_id` saat banyak titik. */
        const val KEY_SELECTED_ATTENDANCE_SITE_ID = "selected_attendance_site_id"

        /** JSON [com.gridy.rohmahapp.data.model.PrayerScheduleUi] untuk jadwal harian. */
        const val KEY_PRAYER_SCHEDULE_JSON = "prayer_schedule_json"
        /** Tanggal jadwal tersimpan `yyyy-MM-dd` (timezone perangkat). */
        const val KEY_PRAYER_SCHEDULE_DATE = "prayer_schedule_date_iso"
        /** Notifikasi 5 menit sebelum waktu sholat (default aktif). */
        const val KEY_PRAYER_REMINDER_5MIN_ENABLED = "prayer_reminder_5min_enabled"
    }
}