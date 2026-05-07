// File: /app/src/main/java/com/gridy/rohmahapp/viewmodel/factory/AttendanceViewModelFactory.kt
package com.gridy.rohmahapp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gridy.rohmahapp.repository.AttendanceRepository
import com.gridy.rohmahapp.viewmodel.AttendanceViewModel

class AttendanceViewModelFactory(
    private val repository: AttendanceRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}