package dev.solora.api

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.flowOf

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

    /**
     * GET /leads - Retrieve saved leads with search/filter support
     */
    suspend fun getLeads(
        search: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Result<List<Map<String, Any>>> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling getLeads function")
            
            val data = hashMapOf<String, Any?>(
                "search" to search,
                "status" to status,
                "limit" to limit
            )
            
            val result = this.functions
                .getHttpsCallable("getLeads")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val leads = response["leads"] as List<Map<String, Any>>
            android.util.Log.d("FirebaseFunctionsApi", "Retrieved ${leads.size} leads via API")
            Result.success(leads)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Get leads error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * GET /quotes - Retrieve previous quotes with search support
     */
    suspend fun getQuotes(
        search: String? = null,
        limit: Int = 50
    ): Result<List<Map<String, Any>>> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling getQuotes function")
            
            val data = hashMapOf<String, Any?>(
                "search" to search,
                "limit" to limit
            )
            
            val result = this.functions
                .getHttpsCallable("getQuotes")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val quotes = response["quotes"] as List<Map<String, Any>>
            android.util.Log.d("FirebaseFunctionsApi", "Retrieved ${quotes.size} quotes via API")
            Result.success(quotes)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Get quotes error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PUT /settings - Update application defaults
     */
    suspend fun updateSettings(settings: Map<String, Any>): Result<String> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling updateSettings function")
            
            val data = hashMapOf<String, Any>(
                "settings" to settings
            )
            
            val result = this.functions
                .getHttpsCallable("updateSettings")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val message = response["message"] as String
            android.util.Log.d("FirebaseFunctionsApi", "Settings updated via API: $message")
            Result.success(message)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Update settings error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * POST /sync - Synchronize offline records with cloud
     */
    suspend fun syncData(offlineData: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling syncData function")
            
            val data = hashMapOf<String, Any>(
                "offlineData" to offlineData
            )
            
            val result = this.functions
                .getHttpsCallable("syncData")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val results = response["results"] as Map<String, Any>
            android.util.Log.d("FirebaseFunctionsApi", "Data synced via API: $results")
            Result.success(results)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Sync data error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Health check endpoint
     */
    suspend fun healthCheck(): Result<Map<String, Any>> {
        return try {
            android.util.Log.d("FirebaseFunctionsApi", "Calling healthCheck function")
            
            val result = this.functions
                .getHttpsCallable("healthCheck")
                .call()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            android.util.Log.d("FirebaseFunctionsApi", "Health check via API: $response")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFunctionsApi", "Health check error: ${e.message}", e)
            Result.failure(e)
        }
    }

