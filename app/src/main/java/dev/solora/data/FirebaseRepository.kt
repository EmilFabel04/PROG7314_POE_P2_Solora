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
            
            // Try API first, fallback to direct Firestore
            val apiResult = saveQuoteViaApi(quoteWithUser)
            if (apiResult.isSuccess) {
                android.util.Log.d("FirebaseRepository", "Quote saved via API: ${apiResult.getOrNull()}")
                apiResult
            } else {
                android.util.Log.w("FirebaseRepository", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                val docRef = firestore.collection("quotes").add(quoteWithUser).await()
                android.util.Log.d("FirebaseRepository", "Quote saved via Firestore with ID: ${docRef.id}")
                Result.success(docRef.id)
            }
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
            
            // Try API first, fallback to direct Firestore
            val apiResult = apiService.getQuoteById(quoteId)
            if (apiResult.isSuccess) {
                val quoteData = apiResult.getOrNull()
                if (quoteData != null) {
                    android.util.Log.d("FirebaseRepository", "Quote retrieved via API: $quoteId")
                    // Convert Map to FirebaseQuote
                    val quote = FirebaseQuote(
                        id = quoteData["id"] as? String,
                        reference = quoteData["reference"] as? String ?: "",
                        clientName = quoteData["clientName"] as? String ?: "",
                        address = quoteData["address"] as? String ?: "",
                        usageKwh = (quoteData["usageKwh"] as? Number)?.toDouble(),
                        billRands = (quoteData["billRands"] as? Number)?.toDouble(),
                        tariff = (quoteData["tariff"] as? Number)?.toDouble() ?: 0.0,
                        panelWatt = (quoteData["panelWatt"] as? Number)?.toInt() ?: 0,
                        latitude = (quoteData["latitude"] as? Number)?.toDouble(),
                        longitude = (quoteData["longitude"] as? Number)?.toDouble(),
                        averageAnnualIrradiance = (quoteData["averageAnnualIrradiance"] as? Number)?.toDouble(),
                        averageAnnualSunHours = (quoteData["averageAnnualSunHours"] as? Number)?.toDouble(),
                        systemKwp = (quoteData["systemKwp"] as? Number)?.toDouble() ?: 0.0,
                        estimatedGeneration = (quoteData["estimatedGeneration"] as? Number)?.toDouble() ?: 0.0,
                        monthlySavings = (quoteData["monthlySavings"] as? Number)?.toDouble() ?: 0.0,
                        paybackMonths = (quoteData["paybackMonths"] as? Number)?.toInt() ?: 0,
                        companyName = quoteData["companyName"] as? String ?: "",
                        companyPhone = quoteData["companyPhone"] as? String ?: "",
                        companyEmail = quoteData["companyEmail"] as? String ?: "",
                        consultantName = quoteData["consultantName"] as? String ?: "",
                        consultantPhone = quoteData["consultantPhone"] as? String ?: "",
                        consultantEmail = quoteData["consultantEmail"] as? String ?: "",
                        userId = quoteData["userId"] as? String ?: "",
                        createdAt = quoteData["createdAt"] as? com.google.firebase.Timestamp,
                        updatedAt = quoteData["updatedAt"] as? com.google.firebase.Timestamp
                    )
                    Result.success(quote)
                } else {
                    Result.success(null)
                }
            } else {
                android.util.Log.w("FirebaseRepository", "API failed for getQuoteById, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                
                // Fallback to direct Firestore
                val doc = firestore.collection("quotes")
                    .document(quoteId)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    val quote = doc.toObject(FirebaseQuote::class.java)?.copy(id = doc.id)
                    if (quote?.userId == userId) {
                        android.util.Log.d("FirebaseRepository", "Quote retrieved via Firestore: $quoteId")
                        Result.success(quote)
                    } else {
                        Result.failure(Exception("Quote not found or access denied"))
                    }
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting quote by ID: ${e.message}", e)
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
            
            // Try API first, fallback to direct Firestore
            val apiResult = saveLeadViaApi(leadWithUser)
            if (apiResult.isSuccess) {
                android.util.Log.d("FirebaseRepository", "Lead saved via API: ${apiResult.getOrNull()}")
                apiResult
            } else {
                android.util.Log.w("FirebaseRepository", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                val docRef = firestore.collection("leads").add(leadWithUser).await()
                android.util.Log.d("FirebaseRepository", "Lead saved via Firestore with ID: ${docRef.id}")
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Failed to save lead: ${e.message}", e)
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
            
            android.util.Log.d("FirebaseRepository", "Updating lead $leadId with quoteId: ${lead.quoteId}")
            
            // Use update() instead of set() to only update specific fields
            val updateData = mapOf(
                "quoteId" to lead.quoteId,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("leads")
                .document(leadId)
                .update(updateData)
                .await()
            
            android.util.Log.d("FirebaseRepository", "Successfully updated lead $leadId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Failed to update lead $leadId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getLeadById(leadId: String): Result<FirebaseLead?> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // Note: Using Firestore directly - no API endpoint exists for getLeadById
            android.util.Log.d("FirebaseRepository", "Getting lead by ID via Firestore: $leadId")
            val doc = firestore.collection("leads")
                .document(leadId)
                .get()
                .await()
            
            if (doc.exists()) {
                val lead = doc.toObject(FirebaseLead::class.java)?.copy(id = doc.id)
                if (lead?.userId == userId) {
                    android.util.Log.d("FirebaseRepository", "Lead retrieved via Firestore: $leadId")
                    Result.success(lead)
                } else {
                    Result.failure(Exception("Lead not found or access denied"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting lead by ID: ${e.message}", e)
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
            
            // Try API first, fallback to direct Firestore
            val apiResult = apiService.updateUserProfile(user.copy(id = userId))
            if (apiResult.isSuccess) {
                android.util.Log.d("FirebaseRepository", "User profile saved via API")
                Result.success(userId)
            } else {
                android.util.Log.w("FirebaseRepository", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                
                // Fallback to direct Firestore
                firestore.collection("users")
                    .document(userId)
                    .set(user.copy(id = userId))
                    .await()
                
                Result.success(userId)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Save user profile error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<FirebaseUser?> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // Try API first, fallback to direct Firestore
            val apiResult = apiService.getUserProfile()
            if (apiResult.isSuccess) {
                val userData = apiResult.getOrNull()
                if (userData != null) {
                    val user = convertMapToFirebaseUser(userData)
                    android.util.Log.d("FirebaseRepository", "User profile retrieved via API")
                    Result.success(user)
                } else {
                    Result.success(null)
                }
            } else {
                android.util.Log.w("FirebaseRepository", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                
                // Fallback to direct Firestore
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
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Get user profile error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun convertMapToFirebaseUser(data: Map<String, Any>): FirebaseUser? {
        return try {
            FirebaseUser(
                id = data["id"] as? String,
                name = data["name"] as? String ?: "",
                surname = data["surname"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phone = data["phone"] as? String,
                company = data["company"] as? String,
                role = data["role"] as? String ?: "sales_consultant",
                createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error converting map to FirebaseUser: ${e.message}", e)
            null
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
    
    /**
     * Calculate quote via API with NASA integration
     */
    suspend fun calculateQuoteViaApi(
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int,
        latitude: Double?,
        longitude: Double?
    ): Result<dev.solora.quote.QuoteOutputs> {
        return try {
            val result = apiService.calculateQuote(address, usageKwh, billRands, tariff, panelWatt, latitude, longitude)
            if (result.isSuccess) {
                val calculationData = result.getOrNull()
                if (calculationData != null) {
                    // Convert API response to QuoteOutputs
                    val quoteOutputs = dev.solora.quote.QuoteOutputs(
                        panels = calculationData.panels,
                        systemKw = calculationData.systemKwp,
                        inverterKw = calculationData.inverterKw,
                        estimatedMonthlySavingsR = calculationData.monthlySavings,
                        monthlyUsageKwh = usageKwh ?: 0.0,
                        monthlyBillRands = billRands ?: 0.0,
                        tariffRPerKwh = tariff,
                        panelWatt = panelWatt,
                        estimatedMonthlyGeneration = calculationData.estimatedGeneration,
                        monthlySavingsRands = calculationData.monthlySavings,
                        paybackMonths = calculationData.paybackMonths,
                        detailedAnalysis = null // API doesn't return detailed analysis
                    )
                    android.util.Log.d("FirebaseRepository", "Quote calculated via API: ${quoteOutputs.systemKw}kW system")
                    Result.success(quoteOutputs)
                } else {
                    Result.failure(Exception("No calculation data returned from API"))
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Calculate quote via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save quote via API
     */
    suspend fun saveQuoteViaApi(quote: FirebaseQuote): Result<String> {
        return try {
            val quoteData = mapOf(
                "reference" to quote.reference,
                "clientName" to quote.clientName,
                "address" to quote.address,
                "usageKwh" to quote.usageKwh,
                "billRands" to quote.billRands,
                "tariff" to quote.tariff,
                "panelWatt" to quote.panelWatt,
                "latitude" to quote.latitude,
                "longitude" to quote.longitude,
                "averageAnnualIrradiance" to quote.averageAnnualIrradiance,
                "averageAnnualSunHours" to quote.averageAnnualSunHours,
                "systemKwp" to quote.systemKwp,
                "estimatedGeneration" to quote.estimatedGeneration,
                "monthlySavings" to quote.monthlySavings,
                "paybackMonths" to quote.paybackMonths,
                "companyName" to quote.companyName,
                "companyPhone" to quote.companyPhone,
                "companyEmail" to quote.companyEmail,
                "consultantName" to quote.consultantName,
                "consultantPhone" to quote.consultantPhone,
                "consultantEmail" to quote.consultantEmail,
                "userId" to quote.userId
            )
            
            val result = apiService.saveQuote(quoteData)
            if (result.isSuccess) {
                val quoteId = result.getOrNull()
                android.util.Log.d("FirebaseRepository", "Quote saved via API: $quoteId")
                Result.success(quoteId ?: "")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Save quote via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get quote by ID via API
     */
    suspend fun getQuoteByIdViaApi(quoteId: String): Result<FirebaseQuote?> {
        return try {
            val result = apiService.getQuoteById(quoteId)
            if (result.isSuccess) {
                val quoteData = result.getOrNull()
                if (quoteData != null) {
                    val quote = FirebaseQuote(
                        id = quoteData.get("id") as? String,
                        reference = quoteData.get("reference") as? String ?: "",
                        clientName = quoteData.get("clientName") as? String ?: "",
                        address = quoteData.get("address") as? String ?: "",
                        usageKwh = (quoteData.get("usageKwh") as? Number)?.toDouble(),
                        billRands = (quoteData.get("billRands") as? Number)?.toDouble(),
                        tariff = (quoteData.get("tariff") as? Number)?.toDouble() ?: 0.0,
                        panelWatt = (quoteData.get("panelWatt") as? Number)?.toInt() ?: 0,
                        latitude = (quoteData.get("latitude") as? Number)?.toDouble(),
                        longitude = (quoteData.get("longitude") as? Number)?.toDouble(),
                        averageAnnualIrradiance = (quoteData.get("averageAnnualIrradiance") as? Number)?.toDouble(),
                        averageAnnualSunHours = (quoteData.get("averageAnnualSunHours") as? Number)?.toDouble(),
                        systemKwp = (quoteData.get("systemKwp") as? Number)?.toDouble() ?: 0.0,
                        estimatedGeneration = (quoteData.get("estimatedGeneration") as? Number)?.toDouble() ?: 0.0,
                        monthlySavings = (quoteData.get("monthlySavings") as? Number)?.toDouble() ?: 0.0,
                        paybackMonths = (quoteData.get("paybackMonths") as? Number)?.toInt() ?: 0,
                        companyName = quoteData.get("companyName") as? String ?: "",
                        companyPhone = quoteData.get("companyPhone") as? String ?: "",
                        companyEmail = quoteData.get("companyEmail") as? String ?: "",
                        consultantName = quoteData.get("consultantName") as? String ?: "",
                        consultantPhone = quoteData.get("consultantPhone") as? String ?: "",
                        consultantEmail = quoteData.get("consultantEmail") as? String ?: "",
                        userId = quoteData.get("userId") as? String ?: "",
                        createdAt = quoteData.get("createdAt") as? Timestamp,
                        updatedAt = quoteData.get("updatedAt") as? Timestamp
                    )
                    android.util.Log.d("FirebaseRepository", "Quote retrieved via API: ${quote.reference}")
                    Result.success(quote)
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Get quote by ID via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save lead via API
     */
    suspend fun saveLeadViaApi(lead: FirebaseLead): Result<String> {
        return try {
            val leadData = mapOf(
                "name" to lead.name,
                "email" to lead.email,
                "phone" to lead.phone,
                "status" to lead.status,
                "notes" to lead.notes,
                "quoteId" to lead.quoteId,
                "userId" to lead.userId
            )
            
            val result = apiService.saveLead(leadData)
            if (result.isSuccess) {
                val leadId = result.getOrNull()
                android.util.Log.d("FirebaseRepository", "Lead saved via API: $leadId")
                Result.success(leadId ?: "")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Save lead via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get settings via API
     */
    suspend fun getSettingsViaApi(): Result<Map<String, Any>?> {
        return try {
            val result = apiService.getSettings()
            if (result.isSuccess) {
                val settings = result.getOrNull()
                android.util.Log.d("FirebaseRepository", "Settings retrieved via API")
                Result.success(settings)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Get settings via API error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
