package com.gridy.rohmahapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.pages.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "general_notifications"
    private const val CHANNEL_NAME = "General Notifications"

    const val CHANNEL_PRAYER_ID = "prayer_reminders"
    const val CHANNEL_SCHEDULE_ID = "schedule_reminder_channel"
    private const val CHANNEL_SCHEDULE_NAME = "Pengingat Jadwal"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"

    private fun drawableToBitmap(
        context: Context,
        @DrawableRes drawableRes: Int
    ): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return null

        return drawable.toBitmap(
            width = 128,
            height = 128,
            config = Bitmap.Config.ARGB_8888
        )
    }
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val general = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi umum aplikasi"
            }
            manager.createNotificationChannel(general)

            val prayer = NotificationChannel(
                CHANNEL_PRAYER_ID,
                context.getString(R.string.prayer_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.prayer_notifications_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(prayer)

            val schedule = NotificationChannel(
                CHANNEL_SCHEDULE_ID,
                CHANNEL_SCHEDULE_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi pengingat jadwal pelajaran"
                enableVibration(true)
            }
            manager.createNotificationChannel(schedule)
        }
    }

    /** Memastikan channel sholat ada (mis. setelah OTA / sebelum alarm pertama). */
    fun ensurePrayerChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_PRAYER_ID) != null) return
            val prayer = NotificationChannel(
                CHANNEL_PRAYER_ID,
                context.getString(R.string.prayer_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.prayer_notifications_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(prayer)
        }
    }

    fun showPrayerReminder(
        context: Context,
        prayerName: String,
        prayerTime: String,
        minutesBefore: Int,
        notificationId: Int,
    ) {
        ensurePrayerChannel(context)
        val open = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val largeIcon = drawableToBitmap(context, R.drawable.logo_new)
        val text = context.getString(
            R.string.prayer_reminder_text,
            prayerName,
            prayerTime,
            minutesBefore,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(context.getString(R.string.prayer_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showScheduleNotification(
        context: Context,
        type: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        if (!isNotificationAllowed(context)) return
        ensureScheduleChannel(context)
        val content = resolveScheduleContent(context, type, title, body, data)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val id = scheduleNotificationId(type, data)
        val open = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val largeIcon = drawableToBitmap(context, R.drawable.logo_new)
        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULE_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSubText(context.getString(R.string.school_name))
            .setContentIntent(open)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureScheduleChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_SCHEDULE_ID) != null) return
            val schedule = NotificationChannel(
                CHANNEL_SCHEDULE_ID,
                CHANNEL_SCHEDULE_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi pengingat jadwal pelajaran"
                enableVibration(true)
            }
            manager.createNotificationChannel(schedule)
        }
    }

    private fun scheduleNotificationId(type: String, data: Map<String, String>): Int {
        val key = when (type) {
            "teacher_schedule_start_reminder" -> "teacher_start_${data["schedule_id"].orEmpty()}"
            "teacher_schedule_end_reminder" -> "teacher_end_${data["schedule_id"].orEmpty()}"
            "teacher_schedule_ended" -> "teacher_ended_${data["schedule_id"].orEmpty()}"
            "student_first_schedule_reminder" -> {
                "student_first_${data["class_id"].orEmpty()}_" +
                    "${data["first_schedule_id"].orEmpty()}_${data["start_time"].orEmpty()}"
            }
            else -> "general_${System.currentTimeMillis()}"
        }
        return key.hashCode()
    }

    private data class NotificationContent(
        val title: String,
        val body: String,
    )

    private fun resolveScheduleContent(
        context: Context,
        type: String,
        fallbackTitle: String,
        fallbackBody: String,
        data: Map<String, String>,
    ): NotificationContent {
        val subject = data["subject_name"]?.trim().orEmpty()
        val startTime = data["start_time"]?.trim().orEmpty()
        val endTime = data["end_time"]?.trim().orEmpty()

        return when (type) {
            "teacher_schedule_start_reminder" -> NotificationContent(
                title = context.getString(R.string.notification_teacher_start_title),
                body = if (subject.isNotEmpty() && startTime.isNotEmpty()) {
                    context.getString(R.string.notification_teacher_start_body, subject, startTime)
                } else {
                    fallbackBody
                },
            )
            "teacher_schedule_end_reminder" -> NotificationContent(
                title = context.getString(R.string.notification_teacher_end_title),
                body = if (subject.isNotEmpty() && endTime.isNotEmpty()) {
                    context.getString(R.string.notification_teacher_end_body, subject, endTime)
                } else {
                    fallbackBody
                },
            )
            "teacher_schedule_ended" -> NotificationContent(
                title = context.getString(R.string.notification_teacher_ended_title),
                body = if (subject.isNotEmpty()) {
                    context.getString(R.string.notification_teacher_ended_body, subject)
                } else {
                    fallbackBody
                },
            )
            "student_first_schedule_reminder" -> NotificationContent(
                title = context.getString(R.string.notification_student_first_title),
                body = if (startTime.isNotEmpty()) {
                    context.getString(R.string.notification_student_first_body, startTime)
                } else {
                    fallbackBody
                },
            )
            else -> NotificationContent(
                title = fallbackTitle.ifBlank { context.getString(R.string.app_name) },
                body = fallbackBody,
            )
        }
    }

    private fun isNotificationAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}