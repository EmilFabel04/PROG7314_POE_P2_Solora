package dev.solora.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.data.FirebaseRepository
import dev.solora.data.FirebaseQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.Result

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                android.util.Log.d("DashboardViewModel", "Loading dashboard data...")
                
                // Try API first, fallback to direct Firestore
                val apiResult = firebaseRepository.getQuotesViaApi()
                val quotes = if (apiResult.isSuccess) {
                    android.util.Log.d("DashboardViewModel", "Using API for dashboard data")
                    apiResult.getOrNull() ?: emptyList()
                } else {
                    android.util.Log.w("DashboardViewModel", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                    // Fallback to direct Firestore - collect the flow
                    val directFlow = firebaseRepository.getQuotes()
                    directFlow.first() // Get the first emission from the flow
                }

                android.util.Log.d("DashboardViewModel", "Loaded ${quotes.size} quotes for dashboard")
                
                // Convert API response to FirebaseQuote objects if needed
                val firebaseQuotes = if (quotes.isNotEmpty() && quotes.first() is Map<*, *>) {
                    // Convert API response to FirebaseQuote objects
                    quotes.mapNotNull { quoteMap ->
                        try {
                            val map = quoteMap as Map<String, Any>
                            FirebaseQuote(
                                id = map["id"] as? String,
                                reference = map["reference"] as? String ?: "",
                                clientName = map["clientName"] as? String ?: "",
                                address = map["address"] as? String ?: "",
                                usageKwh = (map["usageKwh"] as? Number)?.toDouble(),
                                billRands = (map["billRands"] as? Number)?.toDouble(),
                                tariff = (map["tariff"] as? Number)?.toDouble() ?: 0.0,
                                panelWatt = (map["panelWatt"] as? Number)?.toInt() ?: 0,
                                latitude = (map["latitude"] as? Number)?.toDouble(),
                                longitude = (map["longitude"] as? Number)?.toDouble(),
                                averageAnnualIrradiance = (map["averageAnnualIrradiance"] as? Number)?.toDouble(),
                                averageAnnualSunHours = (map["averageAnnualSunHours"] as? Number)?.toDouble(),
                                systemKwp = (map["systemKwp"] as? Number)?.toDouble() ?: 0.0,
                                estimatedGeneration = (map["estimatedGeneration"] as? Number)?.toDouble() ?: 0.0,
                                monthlySavings = (map["monthlySavings"] as? Number)?.toDouble() ?: 0.0,
                                paybackMonths = (map["paybackMonths"] as? Number)?.toInt() ?: 0,
                                companyName = map["companyName"] as? String ?: "",
                                companyPhone = map["companyPhone"] as? String ?: "",
                                companyEmail = map["companyEmail"] as? String ?: "",
                                consultantName = map["consultantName"] as? String ?: "",
                                consultantPhone = map["consultantPhone"] as? String ?: "",
                                consultantEmail = map["consultantEmail"] as? String ?: "",
                                userId = map["userId"] as? String ?: "",
                                createdAt = map["createdAt"] as? com.google.firebase.Timestamp,
                                updatedAt = map["updatedAt"] as? com.google.firebase.Timestamp
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("DashboardViewModel", "Error converting quote: ${e.message}")
                            null
                        }
                    }
                } else {
                    quotes as List<FirebaseQuote>
                }

                // Calculate dashboard data
                val dashboardData = calculateDashboardData(firebaseQuotes)
                _dashboardData.value = dashboardData
                
                android.util.Log.d("DashboardViewModel", "Dashboard data calculated: ${dashboardData.totalQuotes} quotes, avg system: ${dashboardData.averageSystemSize}kW")

            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error loading dashboard data: ${e.message}", e)
                _error.value = "Failed to load dashboard data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDashboard() {
        loadDashboardData()
    }
}
