package dev.solora.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SettingsRepository(app.applicationContext)
    
    val settings by lazy {
        try {
            android.util.Log.d("SettingsViewModel", "Initializing settings flow")
            repository.settings.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AppSettings()
            )
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error initializing settings", e)
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
            android.util.Log.d("SettingsViewModel", "Calculation settings updated")
        }
    }
    
    fun updateCompanySettings(settings: CompanySettings) {
        viewModelScope.launch {
            repository.updateCompanySettings(settings)
            android.util.Log.d("SettingsViewModel", "Company settings updated")
        }
    }
    
    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateAppSettings(settings)
            android.util.Log.d("SettingsViewModel", "App settings updated")
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            android.util.Log.d("SettingsViewModel", "Settings reset to defaults")
        }
    }
}
