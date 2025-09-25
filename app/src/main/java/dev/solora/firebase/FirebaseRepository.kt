package dev.solora.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.solora.data.Quote
import dev.solora.data.Lead
import dev.solora.data.UserProfile
import dev.solora.data.UserStats
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
                // Basic quote information
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
                
                // Location and NASA API data
                "latitude" to quote.latitude,
                "longitude" to quote.longitude,
                "averageAnnualIrradiance" to quote.averageAnnualIrradiance,
                "averageAnnualSunHours" to quote.averageAnnualSunHours,
                "optimalMonth" to quote.optimalMonth,
                "optimalMonthIrradiance" to quote.optimalMonthIrradiance,
                "temperature" to quote.temperature,
                "windSpeed" to quote.windSpeed,
                "humidity" to quote.humidity,
                
                // Financial calculations
                "systemCostRands" to quote.systemCostRands,
                "paybackYears" to quote.paybackYears,
                "annualSavingsRands" to quote.annualSavingsRands,
                "co2SavingsKgPerYear" to quote.co2SavingsKgPerYear,
                
                // Metadata
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
                "status" to lead.status,
                "source" to lead.source,
                "notes" to lead.notes,
                "userId" to userId,
                "createdAt" to lead.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            val documentId = "${userId}_${lead.reference}_${lead.createdAt}"
            
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
    
    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val profileData = hashMapOf(
                "userId" to userId,
                "name" to userProfile.name,
                "email" to userProfile.email,
                "companyName" to userProfile.companyName,
                "phone" to userProfile.phone,
                "jobTitle" to userProfile.jobTitle,
                "address" to userProfile.address,
                "profileImageUrl" to userProfile.profileImageUrl,
                "preferences" to userProfile.preferences,
                "createdAt" to userProfile.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(profileData)
                .await()
            
            Log.d(TAG, "User profile saved successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile to Firebase", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val data = document.data
            if (data != null) {
                val profile = UserProfile(
                    userId = data["userId"] as? String ?: userId,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    companyName = data["companyName"] as? String,
                    phone = data["phone"] as? String,
                    jobTitle = data["jobTitle"] as? String,
                    address = data["address"] as? String,
                    profileImageUrl = data["profileImageUrl"] as? String,
                    preferences = data["preferences"] as? Map<String, Any> ?: emptyMap(),
                    createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                    updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
                )
                Log.d(TAG, "User profile retrieved successfully")
                Result.success(profile)
            } else {
                Log.d(TAG, "No user profile found")
                Result.success(null)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile from Firebase", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val updateData = updates.toMutableMap()
            updateData["updatedAt"] = System.currentTimeMillis()
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "User profile updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile in Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== ANALYTICS & STATS ====================
    
    suspend fun getUserStats(): Result<UserStats> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            // Get quote count and data
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
            
            // Calculate derived metrics
            val averageSystemSize = if (totalQuotes > 0) totalSystemKw / totalQuotes else 0.0
            val conversionRate = if (totalLeads > 0) (totalQuotes.toDouble() / totalLeads) * 100 else 0.0
            
            val stats = UserStats(
                totalQuotes = totalQuotes,
                totalLeads = totalLeads,
                totalSystemKw = totalSystemKw,
                totalMonthlySavings = totalSavings,
                totalAnnualSavings = totalSavings * 12,
                averageSystemSize = averageSystemSize,
                conversionRate = conversionRate,
                generatedAt = System.currentTimeMillis()
            )
            
            Log.d(TAG, "User stats calculated: quotes=$totalQuotes, leads=$totalLeads, conversion=$conversionRate%")
            Result.success(stats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user stats from Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== LEAD MANAGEMENT ====================
    
    suspend fun updateLeadStatus(leadId: String, status: String, notes: String = ""): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val updates = mapOf(
                "status" to status,
                "notes" to notes,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection(LEADS_COLLECTION)
                .document(leadId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Lead status updated: $leadId -> $status")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update lead status", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLeadsByStatus(status: String): Result<List<Map<String, Any>>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection(LEADS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val leads = snapshot.documents.mapNotNull { it.data }
            Log.d(TAG, "Retrieved ${leads.size} leads with status: $status")
            Result.success(leads)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get leads by status from Firebase", e)
            Result.failure(e)
        }
    }
    
    // ==================== TESTING ====================
    
    suspend fun testConnection(): Result<String> {
        return try {
            val currentUser = auth.currentUser
            Log.d(TAG, "Current user: ${currentUser?.uid}, email: ${currentUser?.email}")
            
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated - no current user"))
            
            val testData = hashMapOf(
                "test" to "Firebase connection successful",
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId,
                "userEmail" to currentUser?.email,
                "appVersion" to "1.0.0"
            )
            
            Log.d(TAG, "Attempting to write to Firestore with userId: $userId")
            
            firestore.collection("debug")
                .document("test_${userId}")
                .set(testData)
                .await()
            
            Log.d(TAG, "Firebase connection test successful")
            Result.success("Firebase connection successful! User: ${currentUser?.email}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test failed: ${e.message}", e)
            Result.failure(Exception("Firebase test failed: ${e.message}"))
        }
    }
}
