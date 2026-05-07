package com.gridy.rohmahapp.utils

import android.content.Context
import android.provider.Settings

object DeviceUtil {
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}