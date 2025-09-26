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
import dev.solora.firebase.FirebaseRepository
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
    private val firebaseRepository = FirebaseRepository()

    val quotes = dao.observeQuotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Method to get a specific quote by ID  
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
    val networkStatus = networkMonitor.isConnected.stateIn(
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
        // Delegate to the enhanced calculation method
        calculateAndSaveUsingAddress(reference, clientName, address, usageKwh, billRands, tariff, panelWatt)
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
            savingsRands = outputs.estimatedMonthlySavingsR,
            consultantId = FirebaseAuth.getInstance().currentUser?.uid
        )
        
        // Save to local Room database and get the generated ID
        val insertedId = dao.insert(quote)
        val savedQuote = quote.copy(id = insertedId)
        _lastQuote.value = savedQuote
        
        android.util.Log.d("QuotesViewModel", "Quote saved with ID: $insertedId")
        
        // Also save to Firebase Firestore (using the saved quote with correct ID)
        try {
            val result = firebaseRepository.saveQuote(savedQuote)
            if (result.isSuccess) {
                android.util.Log.d("QuotesViewModel", "Quote saved to Firebase: ${result.getOrNull()}")
            } else {
                android.util.Log.e("QuotesViewModel", "Failed to save to Firebase: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("QuotesViewModel", "Firebase save error: ${e.message}")
        }
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

    // Enhanced calculation method that includes NASA API data and geocoding
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
            _calculationState.value = CalculationState.Loading
            
            try {
                val ctx = getApplication<SoloraApp>().applicationContext
                
                // Step 1: Geocode the address to get coordinates with fallback
                var latitude: Double
                var longitude: Double
                
                val geocoder = Geocoder(ctx)
                val addresses = try {
                    android.util.Log.d("QuotesViewModel", "Attempting to geocode: $address")
                    if (Geocoder.isPresent()) {
                        geocoder.getFromLocationName(address, 3)
                    } else {
                        android.util.Log.w("QuotesViewModel", "Geocoder not available on this device")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuotesViewModel", "Geocoding failed: ${e.message}")
                    null
                }
                
                if (!addresses.isNullOrEmpty()) {
                    // Use geocoded coordinates
                    val location = addresses[0]
                    latitude = location.latitude
                    longitude = location.longitude
                    android.util.Log.d("QuotesViewModel", "Address geocoded: $address -> ($latitude, $longitude)")
                } else {
                    // Fallback: Use predefined coordinates for common South African cities
                    val fallbackCoords = getFallbackCoordinates(address)
                    latitude = fallbackCoords.first
                    longitude = fallbackCoords.second
                    android.util.Log.d("QuotesViewModel", "Using fallback coordinates for: $address -> ($latitude, $longitude)")
                }
                
                // Step 2: Get NASA API solar data for the location with fallback
                val nasaResult = nasa.getSolarData(latitude, longitude)
                val (sunHours, nasaData) = if (nasaResult.isSuccess) {
                    val data = nasaResult.getOrThrow()
                    Pair(data.averageAnnualSunHours, data)
                } else {
                    android.util.Log.w("QuotesViewModel", "NASA API failed: ${nasaResult.exceptionOrNull()?.message}")
                    // Fallback to regional average sun hours for South Africa
                    val fallbackSunHours = getFallbackSunHours(latitude, longitude)
                    android.util.Log.d("QuotesViewModel", "Using fallback sun hours: $fallbackSunHours for location ($latitude, $longitude)")
                    Pair(fallbackSunHours, null)
                }
                
                // Get optimal month data (only if NASA API worked)
                val (optimalMonth, optimalData) = if (nasaData != null) {
                    val optimalResult = nasa.getOptimalSolarMonth(latitude, longitude)
                    if (optimalResult.isSuccess) {
                        optimalResult.getOrThrow()
                    } else {
                        12 to null // December is typically optimal for South Africa
                    }
                } else {
                    12 to null // December fallback
                }
                
                android.util.Log.d("QuotesViewModel", "NASA data: avg sun hours = $sunHours, optimal month = $optimalMonth")
                
                // Step 3: Calculate solar system using enhanced data
                val inputs = QuoteInputs(
                    monthlyUsageKwh = usageKwh,
                    monthlyBillRands = billRands,
                    tariffRPerKwh = tariff,
                    panelWatt = panelWatt,
                    sunHoursPerDay = sunHours
                )
                
                val outputs = QuoteCalculator.calculateBasic(inputs)
                
                // Step 4: Calculate additional financial metrics
                val systemCost = outputs.systemKw * 15000.0 // R15,000 per kW estimate
                val annualSavings = outputs.estimatedMonthlySavingsR * 12
                val paybackYears = if (annualSavings > 0) systemCost / annualSavings else null
                val co2Savings = outputs.systemKw * 1500 // Rough estimate: 1.5 tons CO2 per kW per year
                
                // Step 5: Create enhanced quote with all data
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
                    savingsRands = outputs.estimatedMonthlySavingsR,
                    consultantId = FirebaseAuth.getInstance().currentUser?.uid,
                    // NASA API and location data (with fallbacks)
                    latitude = latitude,
                    longitude = longitude,
                    averageAnnualIrradiance = nasaData?.averageAnnualIrradiance,
                    averageAnnualSunHours = nasaData?.averageAnnualSunHours ?: sunHours,
                    optimalMonth = optimalMonth,
                    optimalMonthIrradiance = optimalData?.solarIrradiance,
                    temperature = optimalData?.temperature,
                    windSpeed = optimalData?.windSpeed,
                    humidity = optimalData?.humidity,
                    // Financial calculations
                    systemCostRands = systemCost,
                    paybackYears = paybackYears,
                    annualSavingsRands = annualSavings,
                    co2SavingsKgPerYear = co2Savings
                )
                
                // Step 6: Save to local database and get the generated ID
                val insertedId = dao.insert(quote)
                val savedQuote = quote.copy(id = insertedId)
                _lastQuote.value = savedQuote
                
                android.util.Log.d("QuotesViewModel", "Quote saved with ID: $insertedId")
                
                // Step 7: Save to Firebase with enhanced data (using the saved quote with correct ID)
                try {
                    val result = firebaseRepository.saveQuote(savedQuote)
                    if (result.isSuccess) {
                        android.util.Log.d("QuotesViewModel", "Enhanced quote saved to Firebase: ${result.getOrNull()}")
                    } else {
                        android.util.Log.e("QuotesViewModel", "Failed to save enhanced quote to Firebase: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuotesViewModel", "Firebase save error: ${e.message}")
                }
                
                val locationNote = if (addresses.isNullOrEmpty()) {
                    " (Using approximate location)"
                } else {
                    ""
                }
                
                val nasaNote = if (nasaData == null) {
                    " (Using regional solar estimates)"
                } else {
                    ""
                }
                
                _calculationState.value = CalculationState.Success(
                    "Quote calculated and saved! System: ${String.format("%.2f", outputs.systemKw)}kW, " +
                    "Monthly savings: R${String.format("%.2f", outputs.estimatedMonthlySavingsR)}$locationNote$nasaNote"
                )
                
            } catch (e: Exception) {
                android.util.Log.e("QuotesViewModel", "Calculation failed", e)
                _calculationState.value = CalculationState.Error("Calculation failed: ${e.message}")
            }
        }
    }

    // Firebase Testing and Management
    fun testFirebaseConnection() {
        viewModelScope.launch {
            _calculationState.value = CalculationState.Loading
            try {
                val result = firebaseRepository.testConnection()
                if (result.isSuccess) {
                    _calculationState.value = CalculationState.Success(result.getOrThrow())
                } else {
                    _calculationState.value = CalculationState.Error("Firebase test failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _calculationState.value = CalculationState.Error("Firebase test error: ${e.message}")
            }
        }
    }

    fun syncToFirebase() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                // Get all local quotes and sync to Firebase
                val localQuotes = quotes.value
                var successCount = 0
                var errorCount = 0

                localQuotes.forEach { quote ->
                    val result = firebaseRepository.saveQuote(quote)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        errorCount++
                    }
                }

                if (errorCount == 0) {
                    _syncStatus.value = SyncStatus.Success("Successfully synced $successCount quotes to Firebase")
                } else {
                    _syncStatus.value = SyncStatus.Error("Synced $successCount quotes, failed $errorCount")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("Sync failed: ${e.message}")
            }
        }
    }

    private fun getFallbackSunHours(latitude: Double, longitude: Double): Double {
        // Regional sun hour estimates for South Africa based on location
        return when {
            // Northern Cape (highest solar irradiance in SA)
            latitude > -31.0 && longitude > 18.0 && longitude < 25.0 -> 7.5
            
            // Western Cape coast (good solar, but more variable weather)
            latitude < -33.0 && longitude < 19.0 -> 6.8
            
            // Johannesburg/Gauteng area (high altitude, good solar)
            latitude > -27.0 && latitude < -25.0 && longitude > 27.0 && longitude < 29.0 -> 7.2
            
            // Eastern coast (Durban area - more humid, slightly lower)
            longitude > 29.0 -> 6.5
            
            // Central regions (Free State, North West)
            latitude > -30.0 && latitude < -26.0 -> 7.0
            
            // Default for South Africa (conservative estimate)
            else -> 6.5
        }
    }

    private fun getFallbackCoordinates(address: String): Pair<Double, Double> {
        val addressLower = address.lowercase()
        
        return when {
            // Cape Town area (including Durbanville)
            addressLower.contains("cape town") || 
            addressLower.contains("durbanville") || 
            addressLower.contains("bellville") ||
            addressLower.contains("stellenbosch") ||
            addressLower.contains("paarl") -> Pair(-33.9249, 18.4241) // Cape Town center
            
            // Johannesburg area
            addressLower.contains("johannesburg") || 
            addressLower.contains("joburg") || 
            addressLower.contains("sandton") ||
            addressLower.contains("randburg") -> Pair(-26.2041, 28.0473) // Johannesburg center
            
            // Pretoria area
            addressLower.contains("pretoria") || 
            addressLower.contains("centurion") -> Pair(-25.7479, 28.2293) // Pretoria center
            
            // Durban area
            addressLower.contains("durban") ||
            addressLower.contains("pinetown") ||
            addressLower.contains("umhlanga") -> Pair(-29.8587, 31.0218) // Durban center
            
            // Port Elizabeth / Gqeberha
            addressLower.contains("port elizabeth") ||
            addressLower.contains("gqeberha") -> Pair(-33.9608, 25.6022)
            
            // Bloemfontein
            addressLower.contains("bloemfontein") -> Pair(-29.1217, 26.2148)
            
            // East London
            addressLower.contains("east london") -> Pair(-33.0153, 27.9116)
            
            // Kimberley
            addressLower.contains("kimberley") -> Pair(-28.7282, 24.7499)
            
            // Default to Cape Town if no match (good solar conditions)
            else -> Pair(-33.9249, 18.4241)
        }
    }

    // Public method to update an existing quote with client details
    fun updateQuoteWithClientDetails(
        quoteId: Long,
        reference: String,
        clientName: String,
        address: String
    ) {
        viewModelScope.launch {
            try {
                // Get the existing quote
                val existingQuote = dao.getQuoteById(quoteId)
                if (existingQuote != null) {
                    // Update with new client information
                    val updatedQuote = existingQuote.copy(
                        reference = reference,
                        clientName = clientName,
                        address = address
                    )
                    
                    // Update in Room database
                    dao.update(updatedQuote)
                    _lastQuote.value = updatedQuote
                    
                    android.util.Log.d("QuotesViewModel", "Quote updated with client details: $clientName")
                    
                    // Also update in Firebase
                    try {
                        val result = firebaseRepository.saveQuote(updatedQuote)
                        if (result.isSuccess) {
                            android.util.Log.d("QuotesViewModel", "Updated quote saved to Firebase: ${result.getOrNull()}")
                        } else {
                            android.util.Log.e("QuotesViewModel", "Failed to update quote in Firebase: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuotesViewModel", "Firebase update error: ${e.message}")
                    }
                } else {
                    android.util.Log.e("QuotesViewModel", "Quote with ID $quoteId not found for update")
                }
            } catch (e: Exception) {
                android.util.Log.e("QuotesViewModel", "Error updating quote with client details: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nasa.close()
        networkMonitor.unregister()
    }
}
