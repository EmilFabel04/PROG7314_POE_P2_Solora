package dev.solora.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.data.FirebaseUser
import dev.solora.data.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()

    private val _userProfile = MutableStateFlow<FirebaseUser?>(null)
    val userProfile: StateFlow<FirebaseUser?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = firebaseRepository.getUserProfile()
                if (result.isSuccess) {
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile(
        name: String,
        surname: String,
        email: String,
        phone: String? = null,
        company: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val updatedUser = FirebaseUser(
                    name = name,
                    surname = surname,
                    email = email,
                    phone = phone,
                    company = company,
                    role = _userProfile.value?.role ?: "sales_consultant"
                )
                
                val result = firebaseRepository.saveUserProfile(updatedUser)
                if (result.isSuccess) {
                    // Reload profile after successful update
                    loadUserProfile()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshProfile() {
        loadUserProfile()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearUserData() {
        _userProfile.value = null
        _isLoading.value = false
        _errorMessage.value = null
    }
}