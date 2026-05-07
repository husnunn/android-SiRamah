package com.gridy.rohmahapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridy.rohmahapp.api.isUnauthorizedSessionHandled
import com.gridy.rohmahapp.data.local.sync.AppRemoteRefreshGate
import com.gridy.rohmahapp.data.local.sync.RemoteRefreshSlot
import com.gridy.rohmahapp.data.model.NearestScheduleUi
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.pages.schedule.ScheduleScreenData
import com.gridy.rohmahapp.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _nearestScheduleState = MutableLiveData<UiState<NearestScheduleUi?>>(UiState.Idle)
    val nearestScheduleState: LiveData<UiState<NearestScheduleUi?>> = _nearestScheduleState

    private val _scheduleScreenState = MutableLiveData<UiState<ScheduleScreenData>>(UiState.Idle)
    val scheduleScreenState: LiveData<UiState<ScheduleScreenData>> = _scheduleScreenState

    fun loadNearestSchedule(manualRefresh: Boolean = false) {
        _nearestScheduleState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val forceRemote =
                    manualRefresh ||
                        AppRemoteRefreshGate.consumeRemoteRefresh(RemoteRefreshSlot.SCHEDULE_NEAREST)
                val result = withContext(Dispatchers.IO) {
                    repository.getNearestScheduleToday(forceRemoteRefresh = forceRemote)
                }
                _nearestScheduleState.value = UiState.Success(result)
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _nearestScheduleState.value = UiState.Idle
                    return@launch
                }
                _nearestScheduleState.value = UiState.Error(
                    message = e.message ?: "Gagal memuat jadwal",
                )
            }
        }
    }

    fun loadSchedules(
        semester: String? = null,
        day: Int? = null,
        date: String? = null,
        manualRefresh: Boolean = false,
    ) {
        _scheduleScreenState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val forceRemote =
                    manualRefresh ||
                        AppRemoteRefreshGate.consumeRemoteRefresh(RemoteRefreshSlot.SCHEDULE_LIST)
                val result = withContext(Dispatchers.IO) {
                    repository.getScheduleScreen(
                        semester = semester,
                        day = day,
                        date = date,
                        forceRemoteRefresh = forceRemote,
                    )
                }
                _scheduleScreenState.value = UiState.Success(result)
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _scheduleScreenState.value = UiState.Idle
                    return@launch
                }
                _scheduleScreenState.value = UiState.Error(
                    message = e.message ?: "Gagal memuat daftar jadwal",
                )
            }
        }
    }
}
