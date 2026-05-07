package com.gridy.rohmahapp.repository

import com.gridy.rohmahapp.api.ApiService
import com.gridy.rohmahapp.api.ApiException
import com.gridy.rohmahapp.data.local.UserLocalCacheCleaner
import com.gridy.rohmahapp.data.model.LoginRequest
import com.gridy.rohmahapp.data.model.LoginResponse
import com.gridy.rohmahapp.utils.PreferenceClass

class AuthRepository(
    private val apiService: ApiService,
    private val pref: PreferenceClass,
    private val userLocalCacheCleaner: UserLocalCacheCleaner,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    suspend fun loginStudent(username: String, password: String): LoginResponse {
        val response = apiService.loginStudent(
            LoginRequest(
                username = username,
                password = password,
            ),
        )
        userLocalCacheCleaner.wipeRoomOnly()
        saveSession(response, "student")
        deviceTokenRepository.registerCurrentTokenIfLoggedIn()
        return response
    }

    suspend fun loginTeacher(username: String, password: String): LoginResponse {
        val response = apiService.loginTeacher(
            LoginRequest(
                username = username,
                password = password,
            ),
        )
        userLocalCacheCleaner.wipeRoomOnly()
        saveSession(response, "teacher")
        deviceTokenRepository.registerCurrentTokenIfLoggedIn()
        return response
    }

    suspend fun logoutByRole() {
        when (pref.getString(PreferenceClass.KEY_USER_ROLE)) {
            "student" -> apiService.logoutStudent()
            "teacher" -> apiService.logoutTeacher()
        }
        clearSession()
    }

    suspend fun clearSession() {
        userLocalCacheCleaner.wipeUserScopedCachesOnly()
    }

    private fun saveSession(response: LoginResponse, activeRole: String) {
        pref.putString(PreferenceClass.KEY_USER_TOKEN, response.token)
        pref.putString(PreferenceClass.KEY_USER_ROLE, activeRole)
        pref.putInt(PreferenceClass.KEY_USER_ID, response.user.id)
        pref.putString(PreferenceClass.KEY_USER_NAME, response.user.name)
        pref.putString(PreferenceClass.KEY_USERNAME, response.user.username)
    }
}
