package com.gridy.rohmahapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "android",
    val device_name: String?,
    val app_version: String?,
    val os_version: String?,
)
