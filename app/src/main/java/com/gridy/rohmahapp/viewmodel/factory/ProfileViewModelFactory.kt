// File: /app/src/main/java/com/gridy/rohmahapp/viewmodel/factory/ProfileViewModelFactory.kt
package com.gridy.rohmahapp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gridy.rohmahapp.repository.ProfileRepository
import com.gridy.rohmahapp.viewmodel.ProfileViewModel

class ProfileViewModelFactory(
    private val repository: ProfileRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}