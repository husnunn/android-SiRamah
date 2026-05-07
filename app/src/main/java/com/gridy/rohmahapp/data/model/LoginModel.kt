package com.gridy.rohmahapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val message: String,
    val token: String,
    val must_change_password: Boolean,
    val user: LoginUser
)

@JsonClass(generateAdapter = true)
data class LoginUser(
    val id: Int,
    val name: String,
    val username: String,
    val role: List<String>
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    val message: String
)