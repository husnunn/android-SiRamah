package com.gridy.rohmahapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridy.rohmahapp.api.isUnauthorizedSessionHandled
import com.gridy.rohmahapp.data.model.PrayerScheduleUi
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.repository.PrayerRepository
import kotlinx.coroutines.launch

class PrayerViewModel(
    private val repository: PrayerRepository
) : ViewModel() {

    private val _prayerState = MutableLiveData<UiState<PrayerScheduleUi>>(UiState.Idle)
    val prayerState: LiveData<UiState<PrayerScheduleUi>> = _prayerState

    fun loadPrayerSchedule(latitude: Double, longitude: Double) {
        _prayerState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getPrayerSchedule(latitude, longitude)
                _prayerState.value = UiState.Success(result)
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _prayerState.value = UiState.Idle
                    return@launch
                }
                _prayerState.value = UiState.Error(
                    message = e.message ?: "Gagal memuat jadwal sholat"
                )
            }
        }
    }
}