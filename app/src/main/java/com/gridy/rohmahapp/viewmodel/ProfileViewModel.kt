// File: /app/src/main/java/com/gridy/rohmahapp/viewmodel/ProfileViewModel.kt
package com.gridy.rohmahapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridy.rohmahapp.api.ApiException
import com.gridy.rohmahapp.api.isUnauthorizedSessionHandled
import com.gridy.rohmahapp.data.local.sync.AppRemoteRefreshGate
import com.gridy.rohmahapp.data.local.sync.RemoteRefreshSlot
import com.gridy.rohmahapp.data.model.ProfileUi
import com.gridy.rohmahapp.data.model.UpdatePasswordRequest
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _profileState = MutableLiveData<UiState<ProfileUi>>(UiState.Idle)
    val profileState: LiveData<UiState<ProfileUi>> = _profileState

    private val _uploadPhotoState = MutableLiveData<UiState<String>>(UiState.Idle)
    val uploadPhotoState: LiveData<UiState<String>> = _uploadPhotoState

    private val _passwordState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val passwordState: LiveData<UiState<Unit>> = _passwordState

    fun resetUploadPhotoState() {
        _uploadPhotoState.value = UiState.Idle
    }

    fun resetPasswordState() {
        _passwordState.value = UiState.Idle
    }

    fun loadProfile(manualRefresh: Boolean = false) {
        _profileState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val forceRemote =
                    manualRefresh ||
                        AppRemoteRefreshGate.consumeRemoteRefresh(RemoteRefreshSlot.PROFILE)
                val result = withContext(Dispatchers.IO) {
                    repository.getMyProfile(forceRemoteRefresh = forceRemote)
                }
                _profileState.value = UiState.Success(result)
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _profileState.value = UiState.Idle
                    return@launch
                }
                _profileState.value = UiState.Error(
                    message = e.message ?: "Gagal memuat profil",
                )
            }
        }
    }

    fun uploadProfilePhoto(part: MultipartBody.Part) {
        viewModelScope.launch {
            _uploadPhotoState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfilePhoto(part)
                }
                val refreshed = withContext(Dispatchers.IO) {
                    repository.getMyProfile(forceRemoteRefresh = true)
                }
                _profileState.value = UiState.Success(refreshed)
                _uploadPhotoState.value = UiState.Success("Foto profil berhasil diperbarui.")
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _uploadPhotoState.value = UiState.Idle
                    return@launch
                }
                _uploadPhotoState.value = UiState.Error(
                    message = e.message ?: "Gagal mengunggah foto",
                )
            }
        }
    }

    fun updatePassword(request: UpdatePasswordRequest) {
        viewModelScope.launch {
            _passwordState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    repository.updatePassword(request)
                }
                _passwordState.value = UiState.Success(Unit)
            } catch (e: ApiException) {
                _passwordState.value = UiState.Error(
                    message = e.message ?: "Gagal mengubah password",
                    fieldErrors = e.validationErrors.mapValues { it.value as Any },
                )
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _passwordState.value = UiState.Idle
                    return@launch
                }
                _passwordState.value = UiState.Error(
                    message = e.message ?: "Gagal mengubah password",
                )
            }
        }
    }
}
