package dev.solora.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.firebase.FirebaseRepository
import dev.solora.data.UserProfile
import dev.solora.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileInfo(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val bio: String = "",
    val companyName: String = ""
)

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class StatsState {
    object Loading : StatsState()
    data class Success(val stats: UserStats) : StatsState()
    data class Error(val message: String) : StatsState()
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
    
    private val _statsState = MutableStateFlow<StatsState>(StatsState.Loading)
    val statsState: StateFlow<StatsState> = _statsState.asStateFlow()
    
    private val _profile = MutableStateFlow(ProfileInfo())
    val profile: StateFlow<ProfileInfo> = _profile.asStateFlow()

    init {
        loadUserProfile()
        loadUserStats()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val result = firebaseRepository.getUserProfile()
                if (result.isSuccess) {
                    val userProfile = result.getOrNull()
                    if (userProfile != null) {
                        _profileState.value = ProfileState.Success(userProfile)
                        _profile.value = ProfileInfo(
                            fullName = userProfile.name,
                            jobTitle = userProfile.jobTitle ?: "",
                            email = userProfile.email,
                            phone = userProfile.phone ?: "",
                            location = userProfile.address ?: "",
                            companyName = userProfile.companyName ?: ""
                        )
                    } else {
                        // Create default profile for new users
                        createDefaultProfile()
                    }
                } else {
                    _profileState.value = ProfileState.Error(result.exceptionOrNull()?.message ?: "Failed to load profile")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun loadUserStats() {
        viewModelScope.launch {
            _statsState.value = StatsState.Loading
            try {
                val result = firebaseRepository.getUserStats()
                if (result.isSuccess) {
                    _statsState.value = StatsState.Success(result.getOrThrow())
                } else {
                    _statsState.value = StatsState.Error(result.exceptionOrNull()?.message ?: "Failed to load stats")
                }
            } catch (e: Exception) {
                _statsState.value = StatsState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private suspend fun createDefaultProfile() {
        try {
            val defaultProfile = UserProfile(
                userId = "", // Will be set by FirebaseRepository
                name = "Solar Consultant",
                email = "user@solora.co.za",
                companyName = "Solora Solar Solutions",
                jobTitle = "Sales Consultant",
                address = "Cape Town, South Africa"
            )
            
            val result = firebaseRepository.saveUserProfile(defaultProfile)
            if (result.isSuccess) {
                _profileState.value = ProfileState.Success(defaultProfile)
                _profile.value = ProfileInfo(
                    fullName = defaultProfile.name,
                    jobTitle = defaultProfile.jobTitle ?: "",
                    email = defaultProfile.email,
                    companyName = defaultProfile.companyName ?: "",
                    location = defaultProfile.address ?: ""
                )
            }
        } catch (e: Exception) {
            _profileState.value = ProfileState.Error("Failed to create default profile: ${e.message}")
        }
    }

    fun updateProfile(updated: ProfileInfo) {
        viewModelScope.launch { 
            _profile.value = updated 
            
            // Update Firebase
            try {
                val currentProfile = (_profileState.value as? ProfileState.Success)?.profile
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        name = updated.fullName,
                        email = updated.email,
                        phone = updated.phone,
                        jobTitle = updated.jobTitle,
                        address = updated.location,
                        companyName = updated.companyName
                    )
                    
                    val result = firebaseRepository.saveUserProfile(updatedProfile)
                    if (result.isSuccess) {
                        _profileState.value = ProfileState.Success(updatedProfile)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to update profile in Firebase: ${e.message}")
            }
        }
    }
    
    fun refreshProfile() {
        loadUserProfile()
    }
    
    fun refreshStats() {
        loadUserStats()
    }

    fun changePassword(current: String, new: String): Boolean {
        // TODO: Implement Firebase password change
        return current.isNotBlank() && new.length >= 6
    }
}
