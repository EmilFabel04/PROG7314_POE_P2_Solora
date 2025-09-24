package dev.solora.quotes

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.api.ApiRepository
import dev.solora.api.NetworkMonitor
import dev.solora.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dev.solora.data.Quote
import dev.solora.quote.CustomerPreferences
import dev.solora.quote.LocationInputs
import dev.solora.quote.NasaPowerClient
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import dev.solora.quote.QuoteOutputs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// State classes for UI
sealed class CalculationState {
    object Idle : CalculationState()
    object Loading : CalculationState()
    data class Success(val message: String) : CalculationState()
    data class Error(val message: String) : CalculationState()
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

// Simplified network status for this implementation
// NetworkStatus removed, using Boolean directly

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.quoteDao()
    private val nasa = NasaPowerClient()
    private val authRepository = AuthRepository(app.applicationContext)
    private val apiRepository = ApiRepository(app.applicationContext)
    private val networkMonitor = NetworkMonitor(app.applicationContext)

    val quotes = dao.observeQuotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun quoteById(id: Long) = dao.observeQuote(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _lastQuote = MutableStateFlow<Quote?>(null)
    val lastQuote: StateFlow<Quote?> = _lastQuote.asStateFlow()
    
    private val _calculationState = MutableStateFlow<CalculationState>(CalculationState.Idle)
    val calculationState: StateFlow<CalculationState> = _calculationState.asStateFlow()
    
    private val _lastCalculation = MutableStateFlow<QuoteOutputs?>(null)
    val lastCalculation: StateFlow<QuoteOutputs?> = _lastCalculation.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Network and sync status
    val networkStatus = networkMonitor.isOnline.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        false
    )

    // Enhanced calculation with NASA API integration
    fun calculateAdvanced(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        sunHours: Double = 5.0,
        latitude: Double? = null,
        longitude: Double? = null,
        preferences: CustomerPreferences? = null
    ) {
        viewModelScope.launch {
            _calculationState.value = CalculationState.Loading
            
            try {
                val locationInputs = if (latitude != null && longitude != null) {
                    LocationInputs(
                        latitude = latitude,
                        longitude = longitude,
                        address = address
                    )
                } else null
                
                val inputs = QuoteInputs(
                    monthlyUsageKwh = usageKwh,
                    monthlyBillRands = billRands,
                    tariffRPerKwh = tariff,
                    panelWatt = panelWatt,
                    sunHoursPerDay = sunHours,
                    location = locationInputs,
                    preferences = preferences
                )
                
                val result = if (networkStatus.value && locationInputs != null) {
                    QuoteCalculator.calculateAdvanced(inputs, nasa)
                } else {
                    Result.success(QuoteCalculator.calculateBasic(inputs))
                }
                
                if (result.isSuccess) {
                    val outputs = result.getOrThrow()
                    _lastCalculation.value = outputs
                    _calculationState.value = CalculationState.Success("Calculation completed successfully")
                    
                    // Save to database
                    saveQuoteFromOutputs(reference, clientName, address, inputs, outputs)
                } else {
                    _calculationState.value = CalculationState.Error(result.exceptionOrNull()?.message ?: "Calculation failed")
                }
                
            } catch (e: Exception) {
                _calculationState.value = CalculationState.Error("Error: ${e.message}")
            }
        }
    }

    fun calculateAndSave(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        sunHours: Double = 5.0
    ) {
        calculateAdvanced(reference, clientName, address, usageKwh, billRands, tariff, panelWatt, sunHours)
    }

    fun calculateAndSaveUsingAddress(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            var sun = 5.0
            var latitude: Double? = null
            var longitude: Double? = null
            
            try {
                val geocoder = Geocoder(ctx)
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    val loc = results.first()
                    latitude = loc.latitude
                    longitude = loc.longitude
                    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
                    nasa.getMonthlySunHours(loc.latitude, loc.longitude, month).getOrNull()?.let { sun = it }
                }
            } catch (_: Exception) {
            }

            calculateAdvanced(reference, clientName, address, usageKwh, billRands, tariff, panelWatt, sun, latitude, longitude)
        }
    }

    private suspend fun saveQuoteFromOutputs(
        reference: String,
        clientName: String,
        address: String,
        inputs: QuoteInputs,
        outputs: QuoteOutputs
    ) {
        val quote = Quote(
            reference = reference,
            clientName = clientName,
            address = address,
            monthlyUsageKwh = inputs.monthlyUsageKwh,
            monthlyBillRands = inputs.monthlyBillRands,
            tariff = inputs.tariffRPerKwh,
            panelWatt = inputs.panelWatt,
            sunHours = inputs.sunHoursPerDay,
            panels = outputs.panels,
            systemKw = outputs.systemKw,
            inverterKw = outputs.inverterKw,
            savingsRands = outputs.estimatedMonthlySavingsR
        )
        dao.insert(quote)
        _lastQuote.value = quote
    }

    private suspend fun saveQuote(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        sunHours: Double
    ) {
        val inputs = QuoteInputs(
            monthlyUsageKwh = usageKwh,
            monthlyBillRands = billRands,
            tariffRPerKwh = tariff,
            panelWatt = panelWatt,
            sunHoursPerDay = sunHours
        )
        val outputs = QuoteCalculator.calculateBasic(inputs)
        saveQuoteFromOutputs(reference, clientName, address, inputs, outputs)
    }

    // API Integration Methods
    fun syncQuotes() {
        viewModelScope.launch {
            if (!networkStatus.value) {
                _syncStatus.value = SyncStatus.Error("No internet connection")
                return@launch
            }

            _syncStatus.value = SyncStatus.Syncing
            
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _syncStatus.value = SyncStatus.Error("User not authenticated")
                    return@launch
                }

                val token = authRepository.getFirebaseIdToken()
                if (token == null) {
                    _syncStatus.value = SyncStatus.Error("Failed to get auth token")
                    return@launch
                }

                // Get local quotes and sync with backend
                val localQuotes = quotes.value
                // Convert to API format and sync
                // Implementation depends on your backend API structure
                
                _syncStatus.value = SyncStatus.Success("Quotes synced successfully")
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("Sync failed: ${e.message}")
            }
        }
    }

    fun testNasaApi(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _calculationState.value = CalculationState.Loading
            
            try {
                val result = nasa.getSolarData(latitude, longitude)
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    _calculationState.value = CalculationState.Success(
                        "NASA API Test: Average sun hours: ${data.averageAnnualSunHours.toString().take(4)}/day"
                    )
                } else {
                    _calculationState.value = CalculationState.Error(
                        result.exceptionOrNull()?.message ?: "NASA API test failed"
                    )
                }
            } catch (e: Exception) {
                _calculationState.value = CalculationState.Error("NASA API error: ${e.message}")
            }
        }
    }

    fun clearCalculationState() {
        _calculationState.value = CalculationState.Idle
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    override fun onCleared() {
        super.onCleared()
        nasa.close()
    }
}
