package com.gridy.rohmahapp.network

import android.content.Context
import android.content.SharedPreferences

class AppTokenProvider(context: Context) : TokenProvider {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rohmah_session", Context.MODE_PRIVATE)

    override fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override fun clearSession() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "key_access_token"
    }
}