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

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app.applicationContext)
    
    val isLoggedIn = repo.isLoggedIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val userInfo = repo.getCurrentUserInfo().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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

    fun authenticateWithGoogle(idToken: String, isRegistration: Boolean = false) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                Log.d("AuthViewModel", "🚀 Starting Google authentication flow...")
                Log.d("AuthViewModel", "📋 Is registration: $isRegistration")
                
                val result = repo.authenticateWithGoogle(idToken)
                
                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    val welcomeMessage = if (isRegistration) {
                        "Welcome to Solora, ${user.displayName ?: user.email}! 🎉"
                    } else {
                        "Welcome back, ${user.displayName ?: user.email}! 👋"
                    }
                    
                    Log.d("AuthViewModel", "✅ Google authentication completed successfully")
                    _authState.value = AuthState.Success(welcomeMessage)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("AuthViewModel", "❌ Google authentication failed: ${error?.message}")
                    _authState.value = AuthState.Error(
                        error?.message ?: "Google authentication failed. Please try again."
                    )
                }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "💥 Unexpected error in Google authentication: ${e.message}")
                _authState.value = AuthState.Error("An unexpected error occurred. Please try again.")
            }
        }
    }
    
    // Convenience methods for backward compatibility
    fun loginWithGoogle(idToken: String) {
        authenticateWithGoogle(idToken, isRegistration = false)
    }
    
    fun registerWithGoogle(idToken: String) {
        authenticateWithGoogle(idToken, isRegistration = true)
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


