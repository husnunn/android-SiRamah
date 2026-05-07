// File: /app/src/main/java/com/gridy/rohmahapp/viewmodel/AttendanceViewModel.kt
package com.gridy.rohmahapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridy.rohmahapp.api.isUnauthorizedSessionHandled
import com.gridy.rohmahapp.data.local.sync.AppRemoteRefreshGate
import com.gridy.rohmahapp.data.local.sync.RemoteRefreshSlot
import com.gridy.rohmahapp.data.model.AttendanceSubmitUi
import com.gridy.rohmahapp.data.model.AttendanceTodayUi
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttendanceViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _todayAttendanceState =
        MutableLiveData<UiState<AttendanceTodayUi>>(UiState.Idle)
    val todayAttendanceState: LiveData<UiState<AttendanceTodayUi>> = _todayAttendanceState

    private val _submitAttendanceState =
        MutableLiveData<UiState<AttendanceSubmitUi>>(UiState.Idle)
    val submitAttendanceState: LiveData<UiState<AttendanceSubmitUi>> = _submitAttendanceState

    fun loadTodayAttendance(
        hasLocationPermission: Boolean,
        manualRefresh: Boolean = false,
    ) {
        _todayAttendanceState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val forceRemote =
                    manualRefresh ||
                        AppRemoteRefreshGate.consumeRemoteRefresh(RemoteRefreshSlot.ATTENDANCE)
                val result = withContext(Dispatchers.IO) {
                    repository.getTodayAttendanceUi(
                        hasLocationPermission = hasLocationPermission,
                        forceRemoteRefresh = forceRemote,
                    )
                }
                _todayAttendanceState.value = UiState.Success(result)
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _todayAttendanceState.value = UiState.Idle
                    return@launch
                }
                _todayAttendanceState.value = UiState.Error(
                    message = e.message ?: "Gagal memuat absensi hari ini",
                )
            }
        }
    }

    fun submitAttendance(
        attendanceType: String,
        attendanceSiteId: Int,
        hasLocationPermission: Boolean
    ) {
        _submitAttendanceState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.submitAttendanceUi(
                        attendanceType = attendanceType,
                        attendanceSiteId = attendanceSiteId,
                        hasLocationPermission = hasLocationPermission
                    )
                }
                _submitAttendanceState.value = UiState.Success(result)

                loadTodayAttendance(
                    hasLocationPermission = hasLocationPermission,
                    manualRefresh = true,
                )
            } catch (e: Exception) {
                if (e.isUnauthorizedSessionHandled()) {
                    _submitAttendanceState.value = UiState.Idle
                    return@launch
                }
                _submitAttendanceState.value = UiState.Error(
                    message = e.message ?: "Gagal memproses absensi"
                )
            }
        }
    }

    fun persistAttendanceSiteSelection(siteId: Int) {
        repository.persistSelectedAttendanceSite(siteId)
    }
}
