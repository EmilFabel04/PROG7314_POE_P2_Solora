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
    
    val settings: Flow<AppSettings> = callbackFlow {
        android.util.Log.d("SettingsRepository", "Initializing Firebase settings")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.w("SettingsRepository", "No authenticated user, using default settings")
            if (!isClosedForSend) {
                trySend(AppSettings())
            }
            awaitClose { } // Empty awaitClose for no-user case
        } else {
            val userId = currentUser.uid
            android.util.Log.d("SettingsRepository", "Loading settings for user: $userId")
            
            val settingsDoc = firestore.collection("user_settings").document(userId)
            
            // Set up real-time listener
            val listener = settingsDoc.addSnapshotListener { snapshot, error ->
                try {
                    if (error != null) {
                        android.util.Log.e("SettingsRepository", "Firebase settings error", error)
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
                            android.util.Log.d("SettingsRepository", "Settings loaded from Firebase: $appSettings")
                            if (!isClosedForSend) {
                                trySend(appSettings)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsRepository", "Error parsing settings data", e)
                            if (!isClosedForSend) {
                                trySend(AppSettings())
                            }
                        }
                    } else {
                        android.util.Log.d("SettingsRepository", "No settings found, using defaults")
                        if (!isClosedForSend) {
                            trySend(AppSettings())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsRepository", "Error in snapshot listener", e)
                }
            }
            
            awaitClose { 
                android.util.Log.d("SettingsRepository", "Removing Firebase listener")
                listener.remove() 
            }
        }
    }
    
    suspend fun updateCalculationSettings(settings: CalculationSettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("SettingsRepository", "No authenticated user for saving calculation settings")
                return
            }
            
            val userId = currentUser.uid
            android.util.Log.d("SettingsRepository", "Saving calculation settings for user: $userId")
            
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
            android.util.Log.d("SettingsRepository", "Calculation settings saved successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving calculation settings", e)
            throw e
        }
    }
    
    suspend fun updateCompanySettings(settings: CompanySettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("SettingsRepository", "No authenticated user for saving company settings")
                return
            }
            
            val userId = currentUser.uid
            android.util.Log.d("SettingsRepository", "Saving company settings for user: $userId")
            
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
            android.util.Log.d("SettingsRepository", "Company settings saved successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving company settings", e)
            throw e
        }
    }
    
    suspend fun updateAppSettings(settings: AppSettings) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("SettingsRepository", "No authenticated user for saving app settings")
                return
            }
            
            val userId = currentUser.uid
            android.util.Log.d("SettingsRepository", "Saving app settings for user: $userId")
            
            val settingsData = mapOf(
                "currency" to settings.currency,
                "language" to settings.language,
                "theme" to settings.theme,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("user_settings").document(userId).set(settingsData, com.google.firebase.firestore.SetOptions.merge()).await()
            android.util.Log.d("SettingsRepository", "App settings saved successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving app settings", e)
            throw e
        }
    }
    
    suspend fun resetToDefaults() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("SettingsRepository", "No authenticated user for resetting settings")
                return
            }
            
            val userId = currentUser.uid
            android.util.Log.d("SettingsRepository", "Resetting settings to defaults for user: $userId")
            
            // Delete the user's settings document to reset to defaults
            firestore.collection("user_settings").document(userId).delete().await()
            android.util.Log.d("SettingsRepository", "Settings reset to defaults successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error resetting settings", e)
            throw e
        }
    }
}
