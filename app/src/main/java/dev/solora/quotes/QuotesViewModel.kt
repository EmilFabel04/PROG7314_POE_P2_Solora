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

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()
    private val nasa = NasaPowerClient()
    private val calculator = QuoteCalculator
    private val geocodingService = GeocodingService(app)

    // Firebase quotes flow
    val quotes = flow {
        emitAll(firebaseRepository.getQuotes())
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
                
                val inputs = QuoteInputs(
                    monthlyUsageKwh = usageKwh,
                    monthlyBillRands = billRands,
                    tariffRPerKwh = tariff,
                    panelWatt = panelWatt,
                    sunHoursPerDay = sunHours,
                    location = if (finalLatitude != null && finalLongitude != null) {
                        dev.solora.quote.LocationInputs(
                            latitude = finalLatitude,
                            longitude = finalLongitude,
                            address = finalAddress
                        )
                    } else null
                )

                android.util.Log.d("QuotesViewModel", "Starting calculation with NASA API integration")
                val result = calculator.calculateAdvanced(inputs, nasa)
                result.fold(
                    onSuccess = { outputs ->
                        android.util.Log.d("QuotesViewModel", "Calculation successful: ${outputs.systemKw}kW system")
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

    // Save quote to Firebase
    fun saveQuote(
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        sunHours: Double,
        systemKwp: Double,
        estimatedGeneration: Double,
        paybackMonths: Int,
        savingsFirstYear: Double
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
                    sunHours = sunHours,
                    systemKwp = systemKwp,
                    estimatedGeneration = estimatedGeneration,
                    paybackMonths = paybackMonths,
                    savingsFirstYear = savingsFirstYear,
                    dateEpoch = System.currentTimeMillis()
                )

                val result = firebaseRepository.saveQuote(quote)
                if (result.isSuccess) {
                    _lastQuote.value = quote.copy(id = result.getOrNull())
                }
            } catch (e: Exception) {
                // Handle error
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
                val quote = FirebaseQuote(
                    reference = reference,
                    clientName = clientName,
                    address = address,
                    usageKwh = calculation.monthlyUsageKwh,
                    billRands = calculation.monthlyBillRands,
                    tariff = calculation.tariffRPerKwh,
                    panelWatt = calculation.panelWatt,
                    sunHours = calculation.sunHoursPerDay,
                    systemKwp = calculation.systemKw,
                    estimatedGeneration = calculation.estimatedMonthlyGeneration,
                    paybackMonths = calculation.paybackMonths,
                    savingsFirstYear = calculation.monthlySavingsRands * 12,
                    dateEpoch = System.currentTimeMillis(),
                    // Add NASA data if available
                    latitude = calculation.detailedAnalysis?.locationData?.latitude,
                    longitude = calculation.detailedAnalysis?.locationData?.longitude,
                    averageAnnualIrradiance = calculation.detailedAnalysis?.locationData?.averageAnnualIrradiance,
                    averageAnnualSunHours = calculation.detailedAnalysis?.locationData?.averageAnnualSunHours,
                    optimalMonth = calculation.detailedAnalysis?.optimalMonth,
                    optimalMonthIrradiance = calculation.detailedAnalysis?.optimalMonthIrradiance,
                    temperature = calculation.detailedAnalysis?.averageTemperature,
                    windSpeed = calculation.detailedAnalysis?.averageWindSpeed,
                    humidity = calculation.detailedAnalysis?.averageHumidity
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