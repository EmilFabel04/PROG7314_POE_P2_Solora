package dev.solora.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SettingsRepository()
    
    val settings by lazy {
        try {
            repository.settings.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AppSettings()
            )
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(AppSettings()).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AppSettings()
            )
        }
    }
    
    fun updateCalculationSettings(settings: CalculationSettings) {
        viewModelScope.launch {
            repository.updateCalculationSettings(settings)
        }
    }
    
    fun updateCompanySettings(settings: CompanySettings) {
        viewModelScope.launch {
            repository.updateCompanySettings(settings)
        }
    }
    
    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            // Try API first, fallback to direct Firestore
            try {
                val apiResult = repository.updateSettingsViaApi(settings)
                if (apiResult.isSuccess) {
                } else {
                    repository.updateAppSettings(settings)
                }
            } catch (e: Exception) {
                repository.updateAppSettings(settings)
            }
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
    
    fun clearSettings() {
        // Clear any cached settings data
    }
}
