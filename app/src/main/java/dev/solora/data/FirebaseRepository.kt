package dev.solora.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Quote Operations
    suspend fun saveQuote(quote: FirebaseQuote): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            val quoteWithUser = quote.copy(userId = userId)
            
            val docRef = firestore.collection("quotes").add(quoteWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuotes(): Flow<List<FirebaseQuote>> = flow {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val snapshot = firestore.collection("quotes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val quotes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseQuote::class.java)?.copy(id = doc.id)
            }
            
            emit(quotes)
        } catch (e: Exception) {
            emit(emptyList())
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

    suspend fun getLeads(): Flow<List<FirebaseLead>> = flow {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val snapshot = firestore.collection("leads")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val leads = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseLead::class.java)?.copy(id = doc.id)
            }
            
            emit(leads)
        } catch (e: Exception) {
            emit(emptyList())
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
}
