package dev.solora.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.solora.data.Quote
import dev.solora.data.Lead
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "FirebaseRepository"
        private const val QUOTES_COLLECTION = "quotes"
        private const val LEADS_COLLECTION = "leads"
        private const val USERS_COLLECTION = "users"
    }
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    // ==================== QUOTES ====================
    
    suspend fun saveQuote(quote: Quote): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val quoteData = hashMapOf(
                "id" to quote.id,
                "reference" to quote.reference,
                "clientName" to quote.clientName,
                "address" to quote.address,
                "monthlyUsageKwh" to quote.monthlyUsageKwh,
                "monthlyBillRands" to quote.monthlyBillRands,
                "tariff" to quote.tariff,
                "panelWatt" to quote.panelWatt,
                "sunHours" to quote.sunHours,
                "panels" to quote.panels,
                "systemKw" to quote.systemKw,
                "inverterKw" to quote.inverterKw,
                "savingsRands" to quote.savingsRands,
                "dateEpoch" to quote.dateEpoch,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            val documentId = "${userId}_${quote.reference}_${quote.dateEpoch}"
            
            firestore.collection(QUOTES_COLLECTION)
                .document(documentId)
                .set(quoteData)
                .await()
            
            Log.d(TAG, "Quote saved successfully: $documentId")
            Result.success(documentId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save quote to Firebase", e)
            Result.failure(e)
        }
    }
    
    suspend fun getQuotes(): Result<List<Map<String, Any>>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection(QUOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("dateEpoch", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val quotes = snapshot.documents.mapNotNull { it.data }
            Log.d(TAG, "Retrieved ${quotes.size} quotes from Firebase")
            Result.success(quotes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quotes from Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== LEADS ====================
    
    suspend fun saveLead(lead: Lead): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val leadData = hashMapOf(
                "id" to lead.id,
                "reference" to lead.reference,
                "name" to lead.name,
                "address" to lead.address,
                "contact" to lead.contact,
                "userId" to userId,
                "status" to "new",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            val documentId = "${userId}_${lead.reference}_${System.currentTimeMillis()}"
            
            firestore.collection(LEADS_COLLECTION)
                .document(documentId)
                .set(leadData)
                .await()
            
            Log.d(TAG, "Lead saved successfully: $documentId")
            Result.success(documentId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save lead to Firebase", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLeads(): Result<List<Map<String, Any>>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection(LEADS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val leads = snapshot.documents.mapNotNull { it.data }
            Log.d(TAG, "Retrieved ${leads.size} leads from Firebase")
            Result.success(leads)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get leads from Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== USER PROFILE ====================
    
    suspend fun saveUserProfile(
        name: String,
        email: String,
        companyName: String? = null,
        phone: String? = null
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val userProfile = hashMapOf(
                "name" to name,
                "email" to email,
                "companyName" to companyName,
                "phone" to phone,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userProfile)
                .await()
            
            Log.d(TAG, "User profile saved successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile to Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== ANALYTICS & STATS ====================
    
    suspend fun getUserStats(): Result<Map<String, Any>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            // Get quote count
            val quotesSnapshot = firestore.collection(QUOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val totalQuotes = quotesSnapshot.size()
            var totalSystemKw = 0.0
            var totalSavings = 0.0
            
            quotesSnapshot.documents.forEach { doc ->
                val data = doc.data
                if (data != null) {
                    (data["systemKw"] as? Number)?.let { totalSystemKw += it.toDouble() }
                    (data["savingsRands"] as? Number)?.let { totalSavings += it.toDouble() }
                }
            }
            
            // Get leads count
            val leadsSnapshot = firestore.collection(LEADS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val totalLeads = leadsSnapshot.size()
            
            val stats = mapOf(
                "totalQuotes" to totalQuotes,
                "totalLeads" to totalLeads,
                "totalSystemKw" to totalSystemKw,
                "totalMonthlySavings" to totalSavings,
                "totalAnnualSavings" to (totalSavings * 12),
                "generatedAt" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "User stats: $stats")
            Result.success(stats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user stats from Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== TESTING ====================
    
    suspend fun testConnection(): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val testData = hashMapOf(
                "test" to "Firebase connection successful",
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId,
                "appVersion" to "1.0.0"
            )
            
            firestore.collection("debug")
                .document("test_${userId}")
                .set(testData)
                .await()
            
            Log.d(TAG, "Firebase connection test successful")
            Result.success("Firebase connection successful!")
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test failed", e)
            Result.failure(e)
        }
    }
}
