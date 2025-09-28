package dev.solora.leads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Lead
import dev.solora.firebase.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeadsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.leadDao()
    private val firebaseRepository = FirebaseRepository()

    val leads = dao.observeLeads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLead(reference: String, name: String, address: String, contact: String, source: String = "manual") {
        viewModelScope.launch { 
            val lead = Lead(
                reference = reference, 
                name = name, 
                address = address, 
                contact = contact,
                status = "new",
                source = source,
                notes = "",
                consultantId = FirebaseAuth.getInstance().currentUser?.uid,
                quoteId = null, // This is a manual lead, not from a quote
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save to local Room database and get the generated ID
            val insertedId = dao.insert(lead)
            val savedLead = lead.copy(id = insertedId)
            
            android.util.Log.d("LeadsViewModel", "Lead saved locally with ID: $insertedId")
            
            // Also save to Firebase Firestore (using the saved lead with correct ID)
            try {
                val result = firebaseRepository.saveLead(savedLead)
                if (result.isSuccess) {
                    android.util.Log.d("LeadsViewModel", "Lead saved to Firebase: ${result.getOrNull()}")
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to save lead to Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Firebase save error: ${e.message}")
            }
        }
    }
    
    fun updateLeadStatus(leadId: Long, status: String, notes: String = "") {
        viewModelScope.launch {
            try {
                // Update in local database
                // Note: You would need to add an update method to LeadDao
                
                // Update in Firebase
                val result = firebaseRepository.updateLeadStatus(leadId.toString(), status, notes)
                if (result.isSuccess) {
                    android.util.Log.d("LeadsViewModel", "Lead status updated in Firebase")
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to update lead status in Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error updating lead status: ${e.message}")
            }
        }
    }
    
    fun syncLeadsFromFirebase() {
        viewModelScope.launch {
            try {
                val result = firebaseRepository.getLeads()
                if (result.isSuccess) {
                    val firebaseLeads = result.getOrNull() ?: emptyList()
                    android.util.Log.d("LeadsViewModel", "Retrieved ${firebaseLeads.size} leads from Firebase")
                    // TODO: Sync with local database if needed
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to sync leads from Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error syncing leads: ${e.message}")
            }
        }
    }
    
    // Create a lead from a quote
    fun createLeadFromQuote(quote: dev.solora.data.Quote, contactInfo: String = "", notes: String = "") {
        viewModelScope.launch {
            try {
                val leadReference = "L${System.currentTimeMillis()}"
                
                val lead = Lead(
                    reference = leadReference,
                    name = quote.clientName,
                    address = quote.address,
                    contact = contactInfo,
                    status = "qualified", // Leads from quotes are typically qualified
                    source = "quote",
                    notes = notes.ifEmpty { "Lead created from quote ${quote.reference}. System: ${quote.systemKw}kW, Savings: R${quote.savingsRands}/month" },
                    consultantId = quote.consultantId ?: FirebaseAuth.getInstance().currentUser?.uid,
                    quoteId = quote.id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Save to local Room database and get the generated ID
                val insertedId = dao.insert(lead)
                val savedLead = lead.copy(id = insertedId)
                
                android.util.Log.d("LeadsViewModel", "Lead created from quote with ID: $insertedId")
                
                // Also save to Firebase Firestore
                try {
                    val result = firebaseRepository.saveLead(savedLead)
                    if (result.isSuccess) {
                        android.util.Log.d("LeadsViewModel", "Lead from quote saved to Firebase: ${result.getOrNull()}")
                    } else {
                        android.util.Log.e("LeadsViewModel", "Failed to save lead from quote to Firebase: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LeadsViewModel", "Firebase save error for lead from quote: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error creating lead from quote: ${e.message}")
            }
        }
    }
}


