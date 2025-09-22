package dev.solora.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app.applicationContext)
    val isLoggedIn = repo.token.map { !it.isNullOrEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun login(email: String, password: String) { viewModelScope.launch { repo.login(email, password) } }
    fun register(name: String, email: String, password: String) { viewModelScope.launch { repo.register(name, email, password) } }
    fun logout() { viewModelScope.launch { repo.logout() } }
}


