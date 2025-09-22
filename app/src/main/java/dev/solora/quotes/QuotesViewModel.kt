package dev.solora.quotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Quote
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.quoteDao()

    val quotes = dao.observeQuotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}


