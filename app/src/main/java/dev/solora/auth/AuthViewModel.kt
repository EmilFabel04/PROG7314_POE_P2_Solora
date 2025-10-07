package dev.solora.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.solora.data.FirebaseUser as UserInfo

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app.applicationContext)
    
    val hasSeenOnboarding = repo.hasSeenOnboarding.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val userInfo = repo.getCurrentUserInfo().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null as UserInfo?)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    fun markOnboardingComplete() {
        viewModelScope.launch {
            repo.markOnboardingComplete()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repo.login(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Login successful")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repo.loginWithGoogle(idToken)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Google login successful")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Google login failed")
            }
        }
    }

    fun register(name: String, surname: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repo.register(name, surname, email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Registration successful")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repo.registerWithGoogle(idToken)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Google sign-in successful")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Google sign-in failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repo.logout()
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Logged out successfully")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Logout failed")
            }
        }
    }

    fun clearAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}