package com.gridy.rohmahapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gridy.rohmahapp.utils.PrayerReminderScheduler

/**
 * Menjadwalkan ulang pengingat sholat setelah reboot jika jadwal hari ini masih tersimpan.
 */
class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        PrayerReminderScheduler.rescheduleFromPrefsIfToday(context.applicationContext)
    }
}
