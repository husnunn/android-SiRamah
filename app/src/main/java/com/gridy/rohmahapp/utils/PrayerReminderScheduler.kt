package com.gridy.rohmahapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.gridy.rohmahapp.data.model.PrayerScheduleUi
import com.gridy.rohmahapp.pages.MainActivity
import com.gridy.rohmahapp.receiver.PrayerReminderReceiver
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Menjadwalkan notifikasi [REMINDER_MINUTES_BEFORE] menit sebelum setiap waktu sholat (hari ini).
 */
object PrayerReminderScheduler {

    const val REMINDER_MINUTES_BEFORE = 5

    private val gson = Gson()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    internal const val RC_FAJR = 9101
    internal const val RC_DHUHA = 9102
    internal const val RC_DHUHR = 9103
    internal const val RC_ASR = 9104
    internal const val RC_MAGHRIB = 9105
    internal const val RC_ISHA = 9106

    private val requestCodes = intArrayOf(
        RC_FAJR, RC_DHUHA, RC_DHUHR, RC_ASR, RC_MAGHRIB, RC_ISHA,
    )

    fun scheduleAfterFetch(context: Context, schedule: PrayerScheduleUi) {
        val pref = PreferenceClass(context)
        if (!pref.getBoolean(PreferenceClass.KEY_PRAYER_REMINDER_5MIN_ENABLED, true)) {
            cancelAll(context)
            return
        }

        NotificationHelper.ensurePrayerChannel(context)

        val today = dateFmt.format(Calendar.getInstance().time)
        cancelAll(context)
        pref.putString(PreferenceClass.KEY_PRAYER_SCHEDULE_JSON, gson.toJson(schedule))
        pref.putString(PreferenceClass.KEY_PRAYER_SCHEDULE_DATE, today)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pairs = listOf(
            schedule.fajr to RC_FAJR,
            schedule.dhuha to RC_DHUHA,
            schedule.dhuhr to RC_DHUHR,
            schedule.asr to RC_ASR,
            schedule.maghrib to RC_MAGHRIB,
            schedule.isha to RC_ISHA,
        )

        var scheduled = 0
        for ((timeStr, reqCode) in pairs) {
            val triggerAt = triggerWallClockMillis(timeStr, REMINDER_MINUTES_BEFORE) ?: continue
            if (triggerAt <= System.currentTimeMillis()) continue
            val op = prayerPendingIntent(context, reqCode)
            try {
                setExactAlarm(am, context, triggerAt, op)
                scheduled++
            } catch (e: Exception) {
                Timber.e(e, "PrayerReminderScheduler: gagal jadwal req=%s", reqCode)
            }
        }
        Timber.d("PrayerReminderScheduler: dijadwalkan %d alarm", scheduled)
    }

    fun rescheduleFromPrefsIfToday(context: Context) {
        val pref = PreferenceClass(context)
        if (!pref.getBoolean(PreferenceClass.KEY_PRAYER_REMINDER_5MIN_ENABLED, true)) return

        val json = pref.getString(PreferenceClass.KEY_PRAYER_SCHEDULE_JSON)
        val savedDate = pref.getString(PreferenceClass.KEY_PRAYER_SCHEDULE_DATE)
        val today = dateFmt.format(Calendar.getInstance().time)
        if (json.isBlank() || savedDate != today) return

        runCatching {
            gson.fromJson(json, PrayerScheduleUi::class.java)
        }.getOrNull()?.let { scheduleAfterFetch(context, it) }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in requestCodes) {
            val pi = prayerPendingIntent(context, code)
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun prayerPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            action = PrayerReminderReceiver.ACTION_ALARM
            putExtra(PrayerReminderReceiver.EXTRA_SLOT, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun triggerWallClockMillis(timeHHmm: String, minutesBefore: Int): Long? {
        val cleaned = timeHHmm.trim().substringBefore(" ")
        val parts = cleaned.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, -minutesBefore)
        return cal.timeInMillis
    }

    private fun setExactAlarm(
        am: AlarmManager,
        context: Context,
        triggerAtMillis: Long,
        operation: PendingIntent,
    ) {
        val show = PendingIntent.getActivity(
            context,
            9199,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, show)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            am.setAlarmClock(info, operation)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }
}
