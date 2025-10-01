package dev.solora.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val apiService = dev.solora.api.FirebaseFunctionsApi()
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Quote Operations
    suspend fun saveQuote(quote: FirebaseQuote): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            android.util.Log.d("FirebaseRepository", "Saving quote for user: $userId")
            android.util.Log.d("FirebaseRepository", "Quote data: ref=${quote.reference}, client=${quote.clientName}, panelWatt=${quote.panelWatt}")
            
            val quoteWithUser = quote.copy(userId = userId)
            
            val docRef = firestore.collection("quotes").add(quoteWithUser).await()
            android.util.Log.d("FirebaseRepository", "Quote saved successfully with ID: ${docRef.id}")
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Failed to save quote: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getQuotes(): Flow<List<FirebaseQuote>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            android.util.Log.w("FirebaseRepository", "No authenticated user for quotes")
            if (!isClosedForSend) {
                trySend(emptyList())
            }
            awaitClose { }
        } else {
            android.util.Log.d("FirebaseRepository", "Setting up real-time listener for quotes (user: $userId)")
            
            val listener = firestore.collection("quotes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirebaseRepository", "Error listening to quotes: ${error.message}", error)
                        
                        // Check for index requirement error
                        if (error.message?.contains("index", ignoreCase = true) == true) {
                            android.util.Log.e("FirebaseRepository", "⚠️ FIREBASE INDEX REQUIRED!")
                            android.util.Log.e("FirebaseRepository", "Create index at: https://console.firebase.google.com/project/solora-e00a4/firestore/indexes")
                            android.util.Log.e("FirebaseRepository", "Index needed: quotes collection with userId (Ascending) + createdAt (Descending)")
                        }
                        
                        if (!isClosedForSend) {
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val quotes = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseQuote::class.java)?.copy(id = doc.id)
                        }
                        android.util.Log.d("FirebaseRepository", "Real-time update: ${quotes.size} quotes for user")
                        if (!isClosedForSend) {
                            trySend(quotes)
                        }
                    } else {
                        if (!isClosedForSend) {
                            trySend(emptyList())
                        }
                    }
                }
            
            awaitClose {
                android.util.Log.d("FirebaseRepository", "Removing quotes listener")
                listener.remove()
            }
        }
    }

    suspend fun getQuoteById(quoteId: String): Result<FirebaseQuote?> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val doc = firestore.collection("quotes")
                .document(quoteId)
                .get()
                .await()
            
            if (doc.exists()) {
                val quote = doc.toObject(FirebaseQuote::class.java)?.copy(id = doc.id)
                if (quote?.userId == userId) {
                    Result.success(quote)
                } else {
                    Result.failure(Exception("Quote not found or access denied"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuote(quoteId: String, quote: FirebaseQuote): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            firestore.collection("quotes")
                .document(quoteId)
                .set(quote.copy(id = quoteId, userId = userId))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuote(quoteId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // Verify ownership before deletion
            val doc = firestore.collection("quotes").document(quoteId).get().await()
            val quote = doc.toObject(FirebaseQuote::class.java)
            
            if (quote?.userId == userId) {
                firestore.collection("quotes").document(quoteId).delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Quote not found or access denied"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lead Operations
    suspend fun saveLead(lead: FirebaseLead): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            val leadWithUser = lead.copy(userId = userId)
            
            val docRef = firestore.collection("leads").add(leadWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeads(): Flow<List<FirebaseLead>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            android.util.Log.w("FirebaseRepository", "No authenticated user for leads")
            if (!isClosedForSend) {
                trySend(emptyList())
            }
            awaitClose { }
        } else {
            android.util.Log.d("FirebaseRepository", "Setting up real-time listener for leads (user: $userId)")
            
            val listener = firestore.collection("leads")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirebaseRepository", "Error listening to leads: ${error.message}", error)
                        
                        // Check for index requirement error
                        if (error.message?.contains("index", ignoreCase = true) == true) {
                            android.util.Log.e("FirebaseRepository", "⚠️ FIREBASE INDEX REQUIRED!")
                            android.util.Log.e("FirebaseRepository", "Create index at: https://console.firebase.google.com/project/solora-e00a4/firestore/indexes")
                            android.util.Log.e("FirebaseRepository", "Index needed: leads collection with userId (Ascending) + createdAt (Descending)")
                        }
                        
                        if (!isClosedForSend) {
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val leads = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseLead::class.java)?.copy(id = doc.id)
                        }
                        android.util.Log.d("FirebaseRepository", "Real-time update: ${leads.size} leads for user")
                        if (!isClosedForSend) {
                            trySend(leads)
                        }
                    } else {
                        if (!isClosedForSend) {
                            trySend(emptyList())
                        }
                    }
                }
            
            awaitClose {
                android.util.Log.d("FirebaseRepository", "Removing leads listener")
                listener.remove()
            }
        }
    }

    suspend fun updateLead(leadId: String, lead: FirebaseLead): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            firestore.collection("leads")
                .document(leadId)
                .set(lead.copy(id = leadId, userId = userId))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeadById(leadId: String): Result<FirebaseLead?> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val doc = firestore.collection("leads")
                .document(leadId)
                .get()
                .await()
            
            if (doc.exists()) {
                val lead = doc.toObject(FirebaseLead::class.java)?.copy(id = doc.id)
                if (lead?.userId == userId) {
                    Result.success(lead)
                } else {
                    Result.failure(Exception("Lead not found or access denied"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLead(leadId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // Verify ownership before deletion
            val doc = firestore.collection("leads").document(leadId).get().await()
            val lead = doc.toObject(FirebaseLead::class.java)
            
            if (lead?.userId == userId) {
                firestore.collection("leads").document(leadId).delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Lead not found or access denied"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User Profile Operations
    suspend fun saveUserProfile(user: FirebaseUser): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            firestore.collection("users")
                .document(userId)
                .set(user.copy(id = userId))
                .await()
            
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<FirebaseUser?> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                val user = doc.toObject(FirebaseUser::class.java)?.copy(id = doc.id)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Configuration Operations
    suspend fun getConfiguration(): Result<FirebaseConfiguration?> {
        return try {
            val doc = firestore.collection("configurations")
                .document("app_config")
                .get()
                .await()
            
            if (doc.exists()) {
                val config = doc.toObject(FirebaseConfiguration::class.java)?.copy(id = doc.id)
                Result.success(config)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveConfiguration(config: FirebaseConfiguration): Result<Unit> {
        return try {
            firestore.collection("configurations")
                .document("app_config")
                .set(config)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // API-BASED METHODS (Using REST API endpoints)
    // ============================================
    
    /**
     * Get leads via API with search and filter support
     */
    suspend fun getLeadsViaApi(
        search: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Result<List<FirebaseLead>> {
        return try {
            val result = apiService.getLeads(search, status, limit)
            if (result.isSuccess) {
                val leadsData = result.getOrNull() ?: emptyList()
                val leads = leadsData.mapNotNull { data: Map<String, Any> ->
                    try {
                        FirebaseLead(
                            id = data["id"] as? String,
                            name = data["name"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            phone = data["phone"] as? String ?: "",
                            status = data["status"] as? String ?: "NEW",
                            notes = data["notes"] as? String,
                            quoteId = data["quoteId"] as? String,
                            userId = data["userId"] as? String ?: "",
                            createdAt = data["createdAt"] as? Timestamp,
                            updatedAt = data["updatedAt"] as? Timestamp
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("FirebaseRepository", "Failed to parse lead: ${e.message}")
                        null
                    }
                }
                android.util.Log.d("FirebaseRepository", "Retrieved ${leads.size} leads via API")
                Result.success(leads)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Get leads via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get quotes via API with search support
     */
    suspend fun getQuotesViaApi(
        search: String? = null,
        limit: Int = 50
    ): Result<List<FirebaseQuote>> {
        return try {
            val result = apiService.getQuotes(search, limit)
            if (result.isSuccess) {
                val quotesData = result.getOrNull() ?: emptyList()
                val quotes = quotesData.mapNotNull { data: Map<String, Any> ->
                    try {
                        FirebaseQuote(
                            id = data["id"] as? String,
                            reference = data["reference"] as? String ?: "",
                            clientName = data["clientName"] as? String ?: "",
                            address = data["address"] as? String ?: "",
                            usageKwh = (data["usageKwh"] as? Number)?.toDouble(),
                            billRands = (data["billRands"] as? Number)?.toDouble(),
                            tariff = (data["tariff"] as? Number)?.toDouble() ?: 0.0,
                            panelWatt = (data["panelWatt"] as? Number)?.toInt() ?: 0,
                            latitude = (data["latitude"] as? Number)?.toDouble(),
                            longitude = (data["longitude"] as? Number)?.toDouble(),
                            averageAnnualIrradiance = (data["averageAnnualIrradiance"] as? Number)?.toDouble(),
                            averageAnnualSunHours = (data["averageAnnualSunHours"] as? Number)?.toDouble(),
                            systemKwp = (data["systemKwp"] as? Number)?.toDouble() ?: 0.0,
                            estimatedGeneration = (data["estimatedGeneration"] as? Number)?.toDouble() ?: 0.0,
                            monthlySavings = (data["monthlySavings"] as? Number)?.toDouble() ?: 0.0,
                            paybackMonths = (data["paybackMonths"] as? Number)?.toInt() ?: 0,
                            companyName = data["companyName"] as? String ?: "",
                            companyPhone = data["companyPhone"] as? String ?: "",
                            companyEmail = data["companyEmail"] as? String ?: "",
                            consultantName = data["consultantName"] as? String ?: "",
                            consultantPhone = data["consultantPhone"] as? String ?: "",
                            consultantEmail = data["consultantEmail"] as? String ?: "",
                            userId = data["userId"] as? String ?: "",
                            createdAt = data["createdAt"] as? Timestamp,
                            updatedAt = data["updatedAt"] as? Timestamp
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("FirebaseRepository", "Failed to parse quote: ${e.message}")
                        null
                    }
                }
                android.util.Log.d("FirebaseRepository", "Retrieved ${quotes.size} quotes via API")
                Result.success(quotes)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Get quotes via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update settings via API
     */
    suspend fun updateSettingsViaApi(settings: Map<String, Any>): Result<String> {
        return try {
            val result = apiService.updateSettings(settings)
            if (result.isSuccess) {
                android.util.Log.d("FirebaseRepository", "Settings updated via API: ${result.getOrNull()}")
                Result.success(result.getOrNull() ?: "Settings updated")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Update settings via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync data via API
     */
    suspend fun syncDataViaApi(offlineData: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val result = apiService.syncData(offlineData)
            if (result.isSuccess) {
                android.util.Log.d("FirebaseRepository", "Data synced via API: ${result.getOrNull()}")
                Result.success(result.getOrNull() ?: emptyMap())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Sync data via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Health check via API
     */
    suspend fun healthCheckViaApi(): Result<Map<String, Any>> {
        return try {
            val result = apiService.healthCheck()
            if (result.isSuccess) {
                android.util.Log.d("FirebaseRepository", "Health check via API: ${result.getOrNull()}")
                Result.success(result.getOrNull() ?: emptyMap())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Health check via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
