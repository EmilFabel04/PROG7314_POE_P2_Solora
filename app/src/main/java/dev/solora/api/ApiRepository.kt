package dev.solora.api

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.IOException

private val Context.apiDataStore by preferencesDataStore(name = "api_config")

class ApiRepository(private val context: Context) {
    private val apiService = SoloraApiService()
    private val firestore = FirebaseFirestore.getInstance()
    
    // DataStore keys
    private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
    private val KEY_NASA_API_ENABLED = booleanPreferencesKey("nasa_api_enabled")
    private val KEY_NASA_API_URL = stringPreferencesKey("nasa_api_url")
    private val KEY_DEFAULT_TARIFF = doublePreferencesKey("default_tariff")
    private val KEY_DEFAULT_PANEL_WATT = intPreferencesKey("default_panel_watt")
    private val KEY_LAST_SYNC = stringPreferencesKey("last_sync")

    // Configuration Flow
    val apiConfiguration: Flow<ApiConfigurationLocal> = context.apiDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            ApiConfigurationLocal(
                backendUrl = prefs[KEY_BACKEND_URL] ?: "https://api.solora.dev/v1/",
                nasaApiEnabled = prefs[KEY_NASA_API_ENABLED] ?: true,
                nasaApiUrl = prefs[KEY_NASA_API_URL] ?: "https://power.larc.nasa.gov/api/",
                defaultTariff = prefs[KEY_DEFAULT_TARIFF] ?: 2.50,
                defaultPanelWatt = prefs[KEY_DEFAULT_PANEL_WATT] ?: 450,
                lastSync = prefs[KEY_LAST_SYNC]
            )
        }

    // Save API configuration locally
    suspend fun saveApiConfiguration(config: ApiConfiguration) {
        context.apiDataStore.edit { prefs ->
            prefs[KEY_NASA_API_ENABLED] = config.nasaApiEnabled
            config.nasaApiUrl?.let { prefs[KEY_NASA_API_URL] = it }
            prefs[KEY_DEFAULT_TARIFF] = config.defaultTariff
            prefs[KEY_DEFAULT_PANEL_WATT] = config.defaultPanelWatt
        }
    }

    // Update backend URL
    suspend fun updateBackendUrl(url: String) {
        context.apiDataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = url
        }
    }

    // Sync configuration from backend
    suspend fun syncConfiguration(token: String): Result<ApiConfiguration> {
        return try {
            val result = apiService.getApiConfiguration(token)
            if (result.isSuccess) {
                result.getOrNull()?.let { config ->
                    saveApiConfiguration(config)
                    // Also save to Firestore for backup
                    saveConfigurationToFirestore(config)
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Save configuration to Firestore
    private suspend fun saveConfigurationToFirestore(config: ApiConfiguration) {
        try {
            val configDoc = hashMapOf(
                "nasa_api_enabled" to config.nasaApiEnabled,
                "nasa_api_url" to config.nasaApiUrl,
                "default_tariff" to config.defaultTariff,
                "default_panel_watt" to config.defaultPanelWatt,
                "company_info" to mapOf(
                    "name" to config.companyInfo.name,
                    "contact_email" to config.companyInfo.contactEmail,
                    "contact_phone" to config.companyInfo.contactPhone,
                    "address" to config.companyInfo.address
                ),
                "updated_at" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("configurations").document("app_config").set(configDoc).await()
        } catch (e: Exception) {
            // Log error but don't fail the operation
            println("Failed to save config to Firestore: ${e.message}")
        }
    }

    // Load configuration from Firestore (fallback)
    suspend fun loadConfigurationFromFirestore(): Result<ApiConfiguration> {
        return try {
            val doc = firestore.collection("configurations").document("app_config").get().await()
            if (doc.exists()) {
                val data = doc.data ?: throw Exception("No configuration data found")
                val companyInfo = data["company_info"] as? Map<String, Any> ?: throw Exception("Invalid company info")
                
                val config = ApiConfiguration(
                    nasaApiEnabled = data["nasa_api_enabled"] as? Boolean ?: true,
                    nasaApiUrl = data["nasa_api_url"] as? String,
                    defaultTariff = (data["default_tariff"] as? Number)?.toDouble() ?: 2.50,
                    defaultPanelWatt = (data["default_panel_watt"] as? Number)?.toInt() ?: 450,
                    companyInfo = CompanyInfo(
                        name = companyInfo["name"] as? String ?: "Solora Solar",
                        contactEmail = companyInfo["contact_email"] as? String ?: "info@solora.dev",
                        contactPhone = companyInfo["contact_phone"] as? String ?: "+27 11 123 4567",
                        address = companyInfo["address"] as? String ?: "Johannesburg, South Africa"
                    )
                )
                saveApiConfiguration(config)
                Result.success(config)
            } else {
                Result.failure(Exception("No configuration found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark last sync time
    suspend fun markLastSync() {
        context.apiDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = System.currentTimeMillis().toString()
        }
    }

    // Data sync methods
    suspend fun syncQuotes(token: String, quotes: List<QuoteData>): Result<SyncResponse> {
        return apiService.syncQuotes(token, quotes)
    }

    suspend fun syncLeads(token: String, leads: List<LeadData>): Result<SyncResponse> {
        return apiService.syncLeads(token, leads)
    }

    suspend fun getQuotes(token: String): Result<List<QuoteData>> {
        return apiService.getQuotes(token)
    }

    suspend fun getLeads(token: String): Result<List<LeadData>> {
        return apiService.getLeads(token)
    }

    suspend fun getUserProfile(token: String): Result<UserProfile> {
        return apiService.getUserProfile(token)
    }

    suspend fun updateUserProfile(token: String, profile: UserProfileUpdate): Result<UserProfile> {
        return apiService.updateUserProfile(token, profile)
    }
}

data class ApiConfigurationLocal(
    val backendUrl: String,
    val nasaApiEnabled: Boolean,
    val nasaApiUrl: String,
    val defaultTariff: Double,
    val defaultPanelWatt: Int,
    val lastSync: String?
)
