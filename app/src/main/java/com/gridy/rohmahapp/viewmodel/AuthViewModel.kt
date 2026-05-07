package com.gridy.rohmahapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridy.rohmahapp.api.ApiException
import com.gridy.rohmahapp.data.model.LoginResponse
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<UiState<LoginResponse>>(UiState.Idle)
    val loginState: LiveData<UiState<LoginResponse>> = _loginState

    private val _logoutState = MutableLiveData<UiState<String>>(UiState.Idle)
    val logoutState: LiveData<UiState<String>> = _logoutState

    fun loginStudent(username: String, password: String) {
        _loginState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val response = authRepository.loginStudent(username, password)
                _loginState.value = UiState.Success(response)
            } catch (e: ApiException) {
                _loginState.value = UiState.Error(
                    message = e.message ?: "Terjadi kesalahan",
                    fieldErrors = e.data
                )
            } catch (e: Exception) {
                _loginState.value = UiState.Error(
                    message = e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    fun loginTeacher(username: String, password: String) {
        _loginState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val response = authRepository.loginTeacher(username, password)
                _loginState.value = UiState.Success(response)
            } catch (e: ApiException) {
                _loginState.value = UiState.Error(
                    message = e.message ?: "Terjadi kesalahan",
                    fieldErrors = e.data
                )
            } catch (e: Exception) {
                _loginState.value = UiState.Error(
                    message = e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    fun logout() {
        _logoutState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.logoutByRole()
                _logoutState.value = UiState.Success("Logout berhasil")
            } catch (e: Exception) {
                // paksa hapus session lokal walau request logout gagal
                authRepository.clearSession()
                _logoutState.value = UiState.Success("Logout berhasil")
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.value = UiState.Idle
    }

    fun resetState() {
        _loginState.value = UiState.Idle
    }
}