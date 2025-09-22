package dev.solora.quotes

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Quote
import dev.solora.quote.NasaPowerClient
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.quoteDao()
    private val nasa = NasaPowerClient()

    val quotes = dao.observeQuotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun quoteById(id: Long) = dao.observeQuote(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _lastQuote = MutableStateFlow<Quote?>(null)
    val lastQuote: StateFlow<Quote?> = _lastQuote.asStateFlow()

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
        viewModelScope.launch {
            saveQuote(reference, clientName, address, usageKwh, billRands, tariff, panelWatt, sunHours)
        }
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
            try {
                val geocoder = Geocoder(ctx)
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    val loc = results.first()
                    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
                    nasa.getMonthlySunHours(loc.latitude, loc.longitude, month)?.let { sun = it }
                }
            } catch (_: Exception) {
            }

            saveQuote(reference, clientName, address, usageKwh, billRands, tariff, panelWatt, sun)
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
        val outputs = QuoteCalculator.calculate(
            QuoteInputs(
                monthlyUsageKwh = usageKwh,
                monthlyBillRands = billRands,
                tariffRPerKwh = tariff,
                panelWatt = panelWatt,
                sunHoursPerDay = sunHours
            )
        )
        val quote = Quote(
            reference = reference,
            clientName = clientName,
            address = address,
            monthlyUsageKwh = usageKwh,
            monthlyBillRands = billRands,
            tariff = tariff,
            panelWatt = panelWatt,
            sunHours = sunHours,
            panels = outputs.panels,
            systemKw = outputs.systemKw,
            inverterKw = outputs.inverterKw,
            savingsRands = outputs.estimatedMonthlySavingsR
        )
        dao.insert(quote)
        _lastQuote.value = quote
    }
}
