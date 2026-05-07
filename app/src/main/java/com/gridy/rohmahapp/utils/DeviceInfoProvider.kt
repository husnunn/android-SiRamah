package com.gridy.rohmahapp.utils

import android.content.Context
import android.os.Build

class DeviceInfoProvider(
    private val context: Context
) {
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()
    }
}