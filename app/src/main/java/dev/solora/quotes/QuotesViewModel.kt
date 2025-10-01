package dev.solora.quotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import dev.solora.data.FirebaseQuote
import dev.solora.data.FirebaseRepository
import dev.solora.quote.NasaPowerClient
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import dev.solora.quote.GeocodingService
import dev.solora.quote.QuoteOutputs
import dev.solora.settings.SettingsRepository
import com.google.firebase.auth.FirebaseAuth

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()
    private val nasa = NasaPowerClient()
    private val calculator = QuoteCalculator
    private val geocodingService = GeocodingService(app)
    private val settingsRepository = SettingsRepository()

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        android.util.Log.d("QuotesViewModel", "QuotesViewModel initialized for user: ${currentUser?.uid ?: "NOT LOGGED IN"}")
        if (currentUser == null) {
            android.util.Log.e("QuotesViewModel", "WARNING: No user logged in! Quotes will be empty.")
        }
    }

    // Firebase quotes flow - filtered by logged-in user's ID
    val quotes = flow {
        android.util.Log.d("QuotesViewModel", "Starting quotes flow for user: ${FirebaseAuth.getInstance().currentUser?.uid}")
        // Try API first, fallback to direct Firestore
        try {
            val apiResult = firebaseRepository.getQuotesViaApi()
            if (apiResult.isSuccess) {
                android.util.Log.d("QuotesViewModel", "Using API for quotes")
                emitAll(flowOf(apiResult.getOrNull() ?: emptyList()))
            } else {
                android.util.Log.w("QuotesViewModel", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                emitAll(firebaseRepository.getQuotes())
            }
        } catch (e: Exception) {
            android.util.Log.w("QuotesViewModel", "API error, using direct Firestore: ${e.message}")
            emitAll(firebaseRepository.getQuotes())
        }
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList<FirebaseQuote>()
    )

    private val _lastQuote = MutableStateFlow<FirebaseQuote?>(null)
    val lastQuote: StateFlow<FirebaseQuote?> = _lastQuote.asStateFlow()

    private val _calculationState = MutableStateFlow<CalculationState>(CalculationState.Idle)
    val calculationState: StateFlow<CalculationState> = _calculationState.asStateFlow()
    
    private val _lastCalculation = MutableStateFlow<QuoteOutputs?>(null)
    val lastCalculation: StateFlow<QuoteOutputs?> = _lastCalculation.asStateFlow()

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
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            _calculationState.value = CalculationState.Loading
            
            try {
                var finalLatitude = latitude
                var finalLongitude = longitude
                var finalAddress = address
                var finalSunHours = sunHours
                
                // If coordinates not provided, try to geocode the address
                if (finalLatitude == null || finalLongitude == null) {
                    android.util.Log.d("QuotesViewModel", "Geocoding address: $address")
                    val geocodeResult = geocodingService.getCoordinatesFromAddress(address)
                    
                    if (geocodeResult.success) {
                        finalLatitude = geocodeResult.latitude
                        finalLongitude = geocodeResult.longitude
                        finalAddress = geocodeResult.address
                        android.util.Log.d("QuotesViewModel", "Geocoding successful: $finalLatitude, $finalLongitude")
                    } else {
                        android.util.Log.w("QuotesViewModel", "Geocoding failed: ${geocodeResult.error}")
                        // Continue with calculation without location data
                    }
                }
                
                // If we have coordinates, try to get NASA sun hours data
                if (finalLatitude != null && finalLongitude != null) {
                    try {
                        val nasaDataResult = nasa.getSolarData(finalLatitude, finalLongitude)
                        if (nasaDataResult.isSuccess) {
                            val nasaData = nasaDataResult.getOrNull()
                            if (nasaData != null) {
                                finalSunHours = nasaData.averageAnnualSunHours
                                android.util.Log.d("QuotesViewModel", "NASA sun hours: $finalSunHours")
                            }
                        } else {
                            android.util.Log.w("QuotesViewModel", "NASA API failed: ${nasaDataResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("QuotesViewModel", "NASA API error: ${e.message}")
                    }
                }
                
                val inputs = QuoteInputs(
                    monthlyUsageKwh = usageKwh,
                    monthlyBillRands = billRands,
                    tariffRPerKwh = tariff,
                    panelWatt = panelWatt,
                    sunHoursPerDay = finalSunHours,
                    location = if (finalLatitude != null && finalLongitude != null) {
                        dev.solora.quote.LocationInputs(
                            latitude = finalLatitude,
                            longitude = finalLongitude,
                            address = finalAddress
                        )
                    } else null
                )

                android.util.Log.d("QuotesViewModel", "Starting calculation with NASA API integration")
                android.util.Log.d("QuotesViewModel", "Input values: usageKwh=$usageKwh, billRands=$billRands, tariff=$tariff, panelWatt=$panelWatt, sunHours=$finalSunHours")
                
                // Get current settings
                val settings = settingsRepository.settings.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    dev.solora.settings.AppSettings()
                ).value.calculationSettings
                
                val result = calculator.calculateAdvanced(inputs, nasa, settings)
                result.fold(
                    onSuccess = { outputs ->
                        android.util.Log.d("QuotesViewModel", "Calculation successful: ${outputs.systemKw}kW system, ${outputs.panels} panels, R${outputs.monthlySavingsRands} savings")
                        _lastCalculation.value = outputs
                        _calculationState.value = CalculationState.Success(outputs)
                    },
                    onFailure = { error ->
                        android.util.Log.e("QuotesViewModel", "Calculation failed: ${error.message}")
                        _calculationState.value = CalculationState.Error(error.message ?: "Calculation failed")
                    }
                )
                
            } catch (e: Exception) {
                android.util.Log.e("QuotesViewModel", "Exception during calculation: ${e.message}", e)
                _calculationState.value = CalculationState.Error(e.message ?: "Calculation failed")
            }
        }
    }

    // Save quote to Firebase (simplified version - prefer saveQuoteFromCalculation)
    fun saveQuote(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        systemKwp: Double,
        estimatedGeneration: Double,
        paybackMonths: Int,
        monthlySavings: Double
    ) {
        viewModelScope.launch {
            try {
                val quote = FirebaseQuote(
                    reference = reference,
                    clientName = clientName,
                    address = address,
                    usageKwh = usageKwh,
                    billRands = billRands,
                    tariff = tariff,
                    panelWatt = panelWatt,
                    systemKwp = systemKwp,
                    estimatedGeneration = estimatedGeneration,
                    paybackMonths = paybackMonths,
                    monthlySavings = monthlySavings,
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )

                val result = firebaseRepository.saveQuote(quote)
                if (result.isSuccess) {
                    _lastQuote.value = quote.copy(id = result.getOrNull())
                }
            } catch (e: Exception) {
                android.util.Log.e("QuotesViewModel", "Error saving quote: ${e.message}", e)
            }
        }
    }

    // Save quote from calculation results
    fun saveQuoteFromCalculation(
        reference: String,
        clientName: String,
        address: String,
        calculation: QuoteOutputs
    ) {
        viewModelScope.launch {
            try {
                // Get company settings snapshot
                val companySettings = settingsRepository.settings.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    dev.solora.settings.AppSettings()
                ).value.companySettings
                
                android.util.Log.d("QuotesViewModel", "Saving quote with panelWatt=${calculation.panelWatt}W (from calculation)")
                
                val quote = FirebaseQuote(
                    reference = reference,
                    clientName = clientName,
                    address = address,
                    // Input data
                    usageKwh = calculation.monthlyUsageKwh,
                    billRands = calculation.monthlyBillRands,
                    tariff = calculation.tariffRPerKwh,
                    panelWatt = calculation.panelWatt,
                    // Location data
                    latitude = calculation.detailedAnalysis?.locationData?.latitude,
                    longitude = calculation.detailedAnalysis?.locationData?.longitude,
                    // NASA API solar data
                    averageAnnualIrradiance = calculation.detailedAnalysis?.locationData?.averageAnnualIrradiance,
                    averageAnnualSunHours = calculation.detailedAnalysis?.locationData?.averageAnnualSunHours,
                    // Calculation results
                    systemKwp = calculation.systemKw,
                    estimatedGeneration = calculation.estimatedMonthlyGeneration,
                    monthlySavings = calculation.monthlySavingsRands,
                    paybackMonths = calculation.paybackMonths,
                    // Company information (snapshot at time of quote)
                    companyName = companySettings.companyName,
                    companyPhone = companySettings.companyPhone,
                    companyEmail = companySettings.companyEmail,
                    consultantName = companySettings.consultantName,
                    consultantPhone = companySettings.consultantPhone,
                    consultantEmail = companySettings.consultantEmail,
                    // Metadata
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )

                val result = firebaseRepository.saveQuote(quote)
                if (result.isSuccess) {
                    val savedQuote = quote.copy(id = result.getOrNull())
                    _lastQuote.value = savedQuote
                    android.util.Log.d("QuotesViewModel", "Quote saved successfully with ID: ${savedQuote.id}")
                } else {
                    android.util.Log.e("QuotesViewModel", "Failed to save quote: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("QuotesViewModel", "Exception saving quote: ${e.message}", e)
            }
        }
    }

    // Get quote by ID
    fun getQuoteById(quoteId: String) {
        viewModelScope.launch {
            val result = firebaseRepository.getQuoteById(quoteId)
            if (result.isSuccess) {
                _lastQuote.value = result.getOrNull()
            }
        }
    }

    // Update quote
    fun updateQuote(quoteId: String, quote: FirebaseQuote) {
        viewModelScope.launch {
            firebaseRepository.updateQuote(quoteId, quote)
        }
    }

    // Delete quote
    fun deleteQuote(quoteId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteQuote(quoteId)
        }
    }

    // Clear calculation state
    fun clearCalculationState() {
        _calculationState.value = CalculationState.Idle
    }
}

sealed class CalculationState {
    object Idle : CalculationState()
    object Loading : CalculationState()
    data class Success(val outputs: QuoteOutputs) : CalculationState()
    data class Error(val message: String) : CalculationState()
}