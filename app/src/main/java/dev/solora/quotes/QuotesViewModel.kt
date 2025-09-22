package dev.solora.quotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Quote
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import dev.solora.quote.NasaPowerClient
import java.util.Calendar
import android.location.Geocoder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.quoteDao()
    private val nasa = NasaPowerClient()

    val quotes = dao.observeQuotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun quoteById(id: Long) = dao.observeQuote(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun calculateAndSave(reference: String, address: String, usageKwh: Double?, tariff: Double, panelWatt: Int, sunHours: Double = 5.0) {
        val outputs = QuoteCalculator.calculate(
            QuoteInputs(monthlyUsageKwh = usageKwh, monthlyBillRands = null, tariffRPerKwh = tariff, panelWatt = panelWatt, sunHoursPerDay = sunHours)
        )
        viewModelScope.launch {
            dao.insert(
                Quote(reference = reference, leadId = null, panels = outputs.panels, systemKw = outputs.systemKw, inverterKw = outputs.inverterKw, savingsRands = outputs.estimatedMonthlySavingsR)
            )
        }
    }

    fun calculateAndSaveUsingAddress(reference: String, address: String, usageKwh: Double?, tariff: Double, panelWatt: Int) {
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
            } catch (_: Exception) { }

            calculateAndSave(reference, address, usageKwh, tariff, panelWatt, sun)
        }
    }
}


