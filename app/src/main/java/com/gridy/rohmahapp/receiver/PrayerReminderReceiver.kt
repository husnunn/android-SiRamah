package com.gridy.rohmahapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.data.model.PrayerScheduleUi
import com.gridy.rohmahapp.utils.NotificationHelper
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.PrayerReminderScheduler

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ALARM) return
        val slot = intent.getIntExtra(EXTRA_SLOT, -1)
        if (slot < 0) return

        val app = context.applicationContext
        val pref = PreferenceClass(app)
        val json = pref.getString(PreferenceClass.KEY_PRAYER_SCHEDULE_JSON)
        val schedule = runCatching {
            Gson().fromJson(json, PrayerScheduleUi::class.java)
        }.getOrNull() ?: return

        val (nameRes, time) = when (slot) {
            PrayerReminderScheduler.RC_FAJR -> R.string.prayer_name_fajr to schedule.fajr
            PrayerReminderScheduler.RC_DHUHA -> R.string.prayer_name_dhuha to schedule.dhuha
            PrayerReminderScheduler.RC_DHUHR -> R.string.prayer_name_dhuhr to schedule.dhuhr
            PrayerReminderScheduler.RC_ASR -> R.string.prayer_name_asr to schedule.asr
            PrayerReminderScheduler.RC_MAGHRIB -> R.string.prayer_name_maghrib to schedule.maghrib
            PrayerReminderScheduler.RC_ISHA -> R.string.prayer_name_isha to schedule.isha
            else -> return
        }
        val title = app.getString(nameRes)
        NotificationHelper.showPrayerReminder(
            app,
            prayerName = title,
            prayerTime = time,
            minutesBefore = PrayerReminderScheduler.REMINDER_MINUTES_BEFORE,
            notificationId = slot,
        )
    }

    companion object {
        const val ACTION_ALARM = "com.gridy.rohmahapp.PRAYER_REMINDER"
        const val EXTRA_SLOT = "extra_slot"
    }
}
