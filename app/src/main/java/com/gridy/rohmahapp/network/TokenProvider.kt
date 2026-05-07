package com.gridy.rohmahapp.network

interface TokenProvider {
    fun getAccessToken(): String?
    fun saveAccessToken(token: String)
    fun clearSession()
}