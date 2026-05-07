package com.gridy.rohmahapp.data.state


sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val fieldErrors: Map<String, Any> = emptyMap()
    ) : UiState<Nothing>()
}