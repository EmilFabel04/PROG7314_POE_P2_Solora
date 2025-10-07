package dev.solora.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class SettingsRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val apiService = dev.solora.api.FirebaseFunctionsApi()
    
    val settings: Flow<AppSettings> = callbackFlow {
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            if (!isClosedForSend) {
                trySend(AppSettings())
            }
            awaitClose { } // Empty awaitClose for no-user case
        } else {
            val userId = currentUser.uid
            
            val settingsDoc = firestore.collection("user_settings").document(userId)
            
            // Set up real-time listener
            val listener = settingsDoc.addSnapshotListener { snapshot, error ->
                try {
                    if (error != null) {
                        if (!isClosedForSend) {
                            trySend(AppSettings()) // Send default settings on error
                        }
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data
                            val appSettings = AppSettings(
                                calculationSettings = CalculationSettings(
                                    defaultTariff = (data?.get("defaultTariff") as? Double) ?: 2.50,
                                    defaultPanelWatt = (data?.get("defaultPanelWatt") as? Long)?.toInt() ?: 420,
                                    panelCostPerWatt = (data?.get("panelCostPerWatt") as? Double) ?: 15.0,
                                    inverterCostPerWatt = (data?.get("inverterCostPerWatt") as? Double) ?: 12.0,
                                    installationCostPerKw = (data?.get("installationCostPerKw") as? Double) ?: 15000.0,
                                    panelEfficiency = (data?.get("panelEfficiency") as? Double) ?: 0.20,
                                    performanceRatio = (data?.get("performanceRatio") as? Double) ?: 0.80,
                                    inverterSizingRatio = (data?.get("inverterSizingRatio") as? Double) ?: 0.80,
                                    defaultSunHours = (data?.get("defaultSunHours") as? Double) ?: 5.0,
                                    systemLifetime = (data?.get("systemLifetime") as? Long)?.toInt() ?: 25,
                                    panelDegradationRate = (data?.get("panelDegradationRate") as? Double) ?: 0.005,
                                    co2PerKwh = (data?.get("co2PerKwh") as? Double) ?: 0.5
                                ),
                                companySettings = CompanySettings(
                                    companyName = (data?.get("companyName") as? String) ?: "",
                                    companyAddress = (data?.get("companyAddress") as? String) ?: "",
                                    companyPhone = (data?.get("companyPhone") as? String) ?: "",
                                    companyEmail = (data?.get("companyEmail") as? String) ?: "",
                                    companyWebsite = (data?.get("companyWebsite") as? String) ?: "",
                                    consultantName = (data?.get("consultantName") as? String) ?: "",
                                    consultantPhone = (data?.get("consultantPhone") as? String) ?: "",
                                    consultantEmail = (data?.get("consultantEmail") as? String) ?: "",
                                    consultantLicense = (data?.get("consultantLicense") as? String) ?: "",
                                    companyLogo = (data?.get("companyLogo") as? String) ?: "",
                                    quoteFooter = (data?.get("quoteFooter") as? String) ?: "",
                                    termsAndConditions = (data?.get("termsAndConditions") as? String) ?: ""
                                ),
                                currency = (data?.get("currency") as? String) ?: "ZAR",
                                language = (data?.get("language") as? String) ?: "en",
                                theme = (data?.get("theme") as? String) ?: "light"
                            )
                            if (!isClosedForSend) {
                                trySend(appSettings)
                            }
                        } catch (e: Exception) {
                            if (!isClosedForSend) {
                                trySend(AppSettings())
                            }
                        }
                    } else {
                        if (!isClosedForSend) {
                            trySend(AppSettings())
                        }
                    }
                } catch (e: Exception) {
                }
            }
            
            awaitClose { 
                listener.remove() 
            }
        }
    }
    
    suspend fun updateCalculationSettings(settings: CalculationSettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return
            }
            
            val userId = currentUser.uid
            
            val settingsData = mapOf(
                "defaultTariff" to settings.defaultTariff,
                "defaultPanelWatt" to settings.defaultPanelWatt,
                "panelCostPerWatt" to settings.panelCostPerWatt,
                "inverterCostPerWatt" to settings.inverterCostPerWatt,
                "installationCostPerKw" to settings.installationCostPerKw,
                "panelEfficiency" to settings.panelEfficiency,
                "performanceRatio" to settings.performanceRatio,
                "inverterSizingRatio" to settings.inverterSizingRatio,
                "defaultSunHours" to settings.defaultSunHours,
                "systemLifetime" to settings.systemLifetime,
                "panelDegradationRate" to settings.panelDegradationRate,
                "co2PerKwh" to settings.co2PerKwh,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("user_settings").document(userId).set(settingsData, com.google.firebase.firestore.SetOptions.merge()).await()
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun updateCompanySettings(settings: CompanySettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return
            }
            
            val userId = currentUser.uid
            
            val settingsData = mapOf(
                "companyName" to settings.companyName,
                "companyAddress" to settings.companyAddress,
                "companyPhone" to settings.companyPhone,
                "companyEmail" to settings.companyEmail,
                "companyWebsite" to settings.companyWebsite,
                "consultantName" to settings.consultantName,
                "consultantPhone" to settings.consultantPhone,
                "consultantEmail" to settings.consultantEmail,
                "consultantLicense" to settings.consultantLicense,
                "companyLogo" to settings.companyLogo,
                "quoteFooter" to settings.quoteFooter,
                "termsAndConditions" to settings.termsAndConditions,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("user_settings").document(userId).set(settingsData, com.google.firebase.firestore.SetOptions.merge()).await()
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun updateAppSettings(settings: AppSettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return
            }
            
            val userId = currentUser.uid
            
            val settingsData = mapOf(
                "currency" to settings.currency,
                "language" to settings.language,
                "theme" to settings.theme,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("user_settings").document(userId).set(settingsData, com.google.firebase.firestore.SetOptions.merge()).await()
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun resetToDefaults() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return
            }
            
            val userId = currentUser.uid
            
            // Delete the user's settings document to reset to defaults
            firestore.collection("user_settings").document(userId).delete().await()
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    // ============================================
    // API-BASED METHODS (Using REST API endpoints)
    // ============================================
    
    /**
     * Update settings via API
     */
    suspend fun updateSettingsViaApi(settings: AppSettings): Result<String> {
        return try {
            
            val settingsMap = mapOf(
                "calculationSettings" to mapOf(
                    "defaultTariff" to settings.calculationSettings.defaultTariff,
                    "defaultPanelWatt" to settings.calculationSettings.defaultPanelWatt,
                    "panelCostPerWatt" to settings.calculationSettings.panelCostPerWatt,
                    "inverterCostPerWatt" to settings.calculationSettings.inverterCostPerWatt,
                    "installationCostPerKw" to settings.calculationSettings.installationCostPerKw,
                    "panelEfficiency" to settings.calculationSettings.panelEfficiency,
                    "performanceRatio" to settings.calculationSettings.performanceRatio,
                    "inverterSizingRatio" to settings.calculationSettings.inverterSizingRatio,
                    "defaultSunHours" to settings.calculationSettings.defaultSunHours,
                    "systemLifetime" to settings.calculationSettings.systemLifetime,
                    "panelDegradationRate" to settings.calculationSettings.panelDegradationRate,
                    "co2PerKwh" to settings.calculationSettings.co2PerKwh
                ),
                "companySettings" to mapOf(
                    "companyName" to settings.companySettings.companyName,
                    "companyAddress" to settings.companySettings.companyAddress,
                    "companyPhone" to settings.companySettings.companyPhone,
                    "companyEmail" to settings.companySettings.companyEmail,
                    "companyWebsite" to settings.companySettings.companyWebsite,
                    "consultantName" to settings.companySettings.consultantName,
                    "consultantPhone" to settings.companySettings.consultantPhone,
                    "consultantEmail" to settings.companySettings.consultantEmail,
                    "consultantLicense" to settings.companySettings.consultantLicense,
                    "companyLogo" to settings.companySettings.companyLogo,
                    "quoteFooter" to settings.companySettings.quoteFooter,
                    "termsAndConditions" to settings.companySettings.termsAndConditions
                ),
                "currency" to settings.currency,
                "language" to settings.language,
                "theme" to settings.theme
            )
            
            val result = apiService.updateSettings(settingsMap)
            if (result.isSuccess) {
                Result.success(result.getOrNull() ?: "Settings updated")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get settings via API
     */
    suspend fun getSettingsViaApi(): Result<AppSettings?> {
        return try {
            
            val result = apiService.getSettings()
            if (result.isSuccess) {
                val settingsData = result.getOrNull()
                if (settingsData != null) {
                    val appSettings = parseSettingsFromMap(settingsData)
                    Result.success(appSettings)
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse settings from API response map
     */
    private fun parseSettingsFromMap(data: Map<String, Any>): AppSettings {
        val calculationData = data["calculationSettings"] as? Map<String, Any> ?: emptyMap()
        val companyData = data["companySettings"] as? Map<String, Any> ?: emptyMap()
        
        val calculationSettings = CalculationSettings(
            defaultTariff = (calculationData["defaultTariff"] as? Number)?.toDouble() ?: 2.5,
            defaultPanelWatt = (calculationData["defaultPanelWatt"] as? Number)?.toInt() ?: 420,
            panelCostPerWatt = (calculationData["panelCostPerWatt"] as? Number)?.toDouble() ?: 15.0,
            inverterCostPerWatt = (calculationData["inverterCostPerWatt"] as? Number)?.toDouble() ?: 12.0,
            installationCostPerKw = (calculationData["installationCostPerKw"] as? Number)?.toDouble() ?: 15000.0,
            panelEfficiency = (calculationData["panelEfficiency"] as? Number)?.toDouble() ?: 0.2,
            performanceRatio = (calculationData["performanceRatio"] as? Number)?.toDouble() ?: 0.8,
            inverterSizingRatio = (calculationData["inverterSizingRatio"] as? Number)?.toDouble() ?: 0.8,
            defaultSunHours = (calculationData["defaultSunHours"] as? Number)?.toDouble() ?: 5.0,
            systemLifetime = (calculationData["systemLifetime"] as? Number)?.toInt() ?: 25,
            panelDegradationRate = (calculationData["panelDegradationRate"] as? Number)?.toDouble() ?: 0.005,
            co2PerKwh = (calculationData["co2PerKwh"] as? Number)?.toDouble() ?: 0.5
        )
        
        val companySettings = CompanySettings(
            companyName = companyData["companyName"] as? String ?: "",
            companyAddress = companyData["companyAddress"] as? String ?: "",
            companyPhone = companyData["companyPhone"] as? String ?: "",
            companyEmail = companyData["companyEmail"] as? String ?: "",
            companyWebsite = companyData["companyWebsite"] as? String ?: "",
            consultantName = companyData["consultantName"] as? String ?: "",
            consultantPhone = companyData["consultantPhone"] as? String ?: "",
            consultantEmail = companyData["consultantEmail"] as? String ?: "",
            consultantLicense = companyData["consultantLicense"] as? String ?: "",
            companyLogo = companyData["companyLogo"] as? String ?: "",
            quoteFooter = companyData["quoteFooter"] as? String ?: "",
            termsAndConditions = companyData["termsAndConditions"] as? String ?: ""
        )
        
        return AppSettings(
            calculationSettings = calculationSettings,
            companySettings = companySettings,
            currency = data["currency"] as? String ?: "ZAR",
            language = data["language"] as? String ?: "en",
            theme = data["theme"] as? String ?: "light"
        )
    }
}
