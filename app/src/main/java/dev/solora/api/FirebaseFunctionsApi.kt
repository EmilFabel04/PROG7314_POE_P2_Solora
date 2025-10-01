package dev.solora.api

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Functions API Client
 * Provides RESTful API access to server-side business logic
 */
class FirebaseFunctionsApi {
    private val functions: FirebaseFunctions = Firebase.functions
    
    /**
     * Calculate quote via Cloud Function with NASA API integration
     */
    suspend fun calculateQuote(
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        latitude: Double?,
        longitude: Double?
    ): Result<CalculationResponse> {
        return try {
            val data = hashMapOf<String, Any?>(
                "address" to address,
                "usageKwh" to usageKwh,
                "billRands" to billRands,
                "tariff" to tariff,
                "panelWatt" to panelWatt,
                "latitude" to latitude,
                "longitude" to longitude
            )
            
            android.util.Log.d("FirebaseFunctionsApi", "Calling calculateQuote function")
            
            val result = functions
                .getHttpsCallable("calculateQuote")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val calculationData = response["calculation"] as? Map<String, Any>
            
            if (calculationData != null) {
                val calculation = parseCalculationResponse(calculationData)
                android.util.Log.d("FirebaseFunctionsApi", "Calculation successful via API")
                Result.success(calculation)
            } else {
                Result.failure(Exception("Invalid response from API"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Calculate quote error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save quote via Cloud Function
     */
    suspend fun saveQuote(quoteData: Map<String, Any?>): Result<String> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling saveQuote function")
            
            val result = functions
                .getHttpsCallable("saveQuote")
                .call(quoteData)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val quoteId = response["quoteId"] as? String
            
            if (quoteId != null) {
                android.util.Log.d("FirebaseFunctionsApi", "Quote saved via API: $quoteId")
                Result.success(quoteId)
            } else {
                Result.failure(Exception("No quote ID returned"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Save quote error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save lead via Cloud Function
     */
    suspend fun saveLead(leadData: Map<String, Any?>): Result<String> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling saveLead function")
            
            val result = functions
                .getHttpsCallable("saveLead")
                .call(leadData)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val leadId = response["leadId"] as? String
            
            if (leadId != null) {
                android.util.Log.d("FirebaseFunctionsApi", "Lead saved via API: $leadId")
                Result.success(leadId)
            } else {
                Result.failure(Exception("No lead ID returned"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Save lead error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get user settings via Cloud Function
     */
    suspend fun getSettings(): Result<Map<String, Any>?> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling getSettings function")
            
            val result = functions
                .getHttpsCallable("getSettings")
                .call(null)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val settings = response["settings"] as? Map<String, Any>
            
            android.util.Log.d("FirebaseFunctionsApi", "Settings retrieved via API")
            Result.success(settings)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Get settings error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun parseCalculationResponse(data: Map<String, Any>): CalculationResponse {
        @Suppress("UNCHECKED_CAST")
        val nasaDataMap = data["nasaData"] as? Map<String, Any>
        
        return CalculationResponse(
            panels = (data["panels"] as? Number)?.toInt() ?: 0,
            systemKwp = (data["systemKwp"] as? Number)?.toDouble() ?: 0.0,
            inverterKw = (data["inverterKw"] as? Number)?.toDouble() ?: 0.0,
            monthlySavings = (data["monthlySavings"] as? Number)?.toDouble() ?: 0.0,
            estimatedGeneration = (data["estimatedGeneration"] as? Number)?.toDouble() ?: 0.0,
            paybackMonths = (data["paybackMonths"] as? Number)?.toInt() ?: 0,
            usageKwh = (data["usageKwh"] as? Number)?.toDouble(),
            billRands = (data["billRands"] as? Number)?.toDouble(),
            tariff = (data["tariff"] as? Number)?.toDouble() ?: 0.0,
            panelWatt = (data["panelWatt"] as? Number)?.toInt() ?: 0,
            nasaData = nasaDataMap?.let {
                NasaDataResponse(
                    averageAnnualIrradiance = (it["averageAnnualIrradiance"] as? Number)?.toDouble(),
                    averageAnnualSunHours = (it["averageAnnualSunHours"] as? Number)?.toDouble(),
                    latitude = (it["latitude"] as? Number)?.toDouble(),
                    longitude = (it["longitude"] as? Number)?.toDouble()
                )
            }
        )
    }
}

// Response data classes
data class CalculationResponse(
    val panels: Int,
    val systemKwp: Double,
    val inverterKw: Double,
    val monthlySavings: Double,
    val estimatedGeneration: Double,
    val paybackMonths: Int,
    val usageKwh: Double?,
    val billRands: Double?,
    val tariff: Double,
    val panelWatt: Int,
    val nasaData: NasaDataResponse?
)

data class NasaDataResponse(
    val averageAnnualIrradiance: Double?,
    val averageAnnualSunHours: Double?,
    val latitude: Double?,
    val longitude: Double?
)

