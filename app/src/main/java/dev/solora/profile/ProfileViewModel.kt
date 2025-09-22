package dev.solora.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileInfo(
    val fullName: String = "Milla Muller",
    val jobTitle: String = "Sales Consultant",
    val email: String = "milla@solora.co.za",
    val phone: String = "+27 82 123 4567",
    val location: String = "Cape Town, South Africa",
    val bio: String = "Dedicated to guiding clients through sustainable solar solutions."
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val _profile = MutableStateFlow(ProfileInfo())
    val profile: StateFlow<ProfileInfo> = _profile.asStateFlow()

    fun updateProfile(updated: ProfileInfo) {
        viewModelScope.launch { _profile.value = updated }
    }

    fun changePassword(current: String, new: String): Boolean {
        // Stubbed success path – in a real app this would call an API.
        return current.isNotBlank() && new.length >= 6
    }
}
