package dev.solora.quotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.solora.data.FirebaseQuote
import dev.solora.data.FirebaseRepository
import dev.solora.quote.NasaPowerClient
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import dev.solora.quote.QuoteOutputs

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()
    private val nasa = NasaPowerClient()
    private val calculator = QuoteCalculator()

    // Firebase quotes flow
    val quotes = firebaseRepository.getQuotes().stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
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
                val inputs = QuoteInputs(
                    reference = reference,
                    clientName = clientName,
                    address = address,
                    usageKwh = usageKwh,
                    billRands = billRands,
                    tariff = tariff,
                    panelWatt = panelWatt,
                    sunHours = sunHours,
                    latitude = latitude,
                    longitude = longitude
                )

                val outputs = calculator.calculateQuote(inputs)
                _lastCalculation.value = outputs
                _calculationState.value = CalculationState.Success(outputs)
                
            } catch (e: Exception) {
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