package com.justself.klique

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// ProfileRepository.kt
object ProfileRepository {
    // Mutable LiveData to hold profile data
    private val _profileUpdateData = MutableLiveData<ProfileUpdateData?>()

    // Public LiveData for observing profile updates
    val profileUpdateData: LiveData<ProfileUpdateData?> = _profileUpdateData

    // Method to update profile data
    fun updateProfileData(profileData: ProfileUpdateData?) {
        _profileUpdateData.postValue(profileData)
    }
    fun clearProfileData() {
        _profileUpdateData.postValue(null)
    }
}