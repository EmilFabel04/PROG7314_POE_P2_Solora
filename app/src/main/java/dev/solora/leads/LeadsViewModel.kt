package dev.solora.leads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.data.FirebaseLead
import dev.solora.data.FirebaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class LeadsViewModel(app: Application) : AndroidViewModel(app) {
    private val firebaseRepository = FirebaseRepository()
    private var leadsForSelection: List<FirebaseLead> = emptyList()

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        android.util.Log.d("LeadsViewModel", "LeadsViewModel initialized for user: ${currentUser?.uid ?: "NOT LOGGED IN"}")
        if (currentUser == null) {
            android.util.Log.e("LeadsViewModel", "WARNING: No user logged in! Leads will be empty.")
        }
    }

    // Firebase leads flow - filtered by logged-in user's ID
    val leads = flow {
        android.util.Log.d("LeadsViewModel", "Starting leads flow for user: ${FirebaseAuth.getInstance().currentUser?.uid}")
        // Try API first, fallback to direct Firestore
        try {
            val apiResult = firebaseRepository.getLeadsViaApi()
            if (apiResult.isSuccess) {
                android.util.Log.d("LeadsViewModel", "Using API for leads")
                emitAll(flowOf(apiResult.getOrNull() ?: emptyList()))
            } else {
                android.util.Log.w("LeadsViewModel", "API failed, using direct Firestore: ${apiResult.exceptionOrNull()?.message}")
                emitAll(firebaseRepository.getLeads())
            }
        } catch (e: Exception) {
            android.util.Log.w("LeadsViewModel", "API error, using direct Firestore: ${e.message}")
            emitAll(firebaseRepository.getLeads())
        }
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList<FirebaseLead>()
    )

    fun addLead(name: String, email: String, phone: String, notes: String = "") {
        viewModelScope.launch { 
            val lead = FirebaseLead(
                name = name,
                email = email,
                phone = phone,
                status = "new",
                notes = notes
            )
            
            val result = firebaseRepository.saveLead(lead)
            if (result.isSuccess) {
                android.util.Log.d("LeadsViewModel", "Lead saved to Firebase: ${result.getOrNull()}")
            } else {
                android.util.Log.e("LeadsViewModel", "Failed to save lead to Firebase: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun updateLeadStatus(leadId: String, status: String, notes: String = "") {
        viewModelScope.launch {
            try {
                // Get current lead
                val result = firebaseRepository.getLeadById(leadId)
                if (result.isSuccess) {
                    val currentLead = result.getOrNull()
                    if (currentLead != null) {
                        val updatedLead = currentLead.copy(
                            status = status,
                            notes = notes.ifEmpty { currentLead.notes }
                        )
                        
                        val updateResult = firebaseRepository.updateLead(leadId, updatedLead)
                        if (updateResult.isSuccess) {
                            android.util.Log.d("LeadsViewModel", "Lead status updated in Firebase")
                        } else {
                            android.util.Log.e("LeadsViewModel", "Failed to update lead status in Firebase: ${updateResult.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to get lead: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error updating lead status: ${e.message}")
            }
        }
    }
    
    fun deleteLead(leadId: String) {
        viewModelScope.launch {
            try {
                val result = firebaseRepository.deleteLead(leadId)
                if (result.isSuccess) {
                    android.util.Log.d("LeadsViewModel", "Lead deleted from Firebase")
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to delete lead from Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error deleting lead: ${e.message}")
            }
        }
    }
    
    // Create a lead from a quote
    fun createLeadFromQuote(
        quoteId: String,
        clientName: String,
        address: String,
        email: String = "",
        phone: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val lead = FirebaseLead(
                    name = clientName,
                    email = email,
                    phone = phone,
                    status = "qualified", // Leads from quotes are typically qualified
                    notes = notes.ifEmpty { "Lead created from quote. Address: $address" },
                    quoteId = quoteId
                )
                
                val result = firebaseRepository.saveLead(lead)
                if (result.isSuccess) {
                    android.util.Log.d("LeadsViewModel", "Lead from quote saved to Firebase: ${result.getOrNull()}")
                } else {
                    android.util.Log.e("LeadsViewModel", "Failed to save lead from quote to Firebase: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error creating lead from quote: ${e.message}")
            }
        }
    }
    
    fun setLeadsForSelection(leads: List<FirebaseLead>) {
        leadsForSelection = leads
    }
    
    fun getLeadsForSelection(): List<FirebaseLead> {
        return leadsForSelection
    }
    
    // Link an existing quote to an existing lead
    fun linkQuoteToLead(leadId: String, quoteId: String) {
        viewModelScope.launch {
            try {
                // Get the current lead
                val result = firebaseRepository.getLeadById(leadId)
                if (result.isSuccess) {
                    val currentLead = result.getOrNull()
                    if (currentLead != null) {
                        // Update the lead with the quote ID
                        val updatedLead = currentLead.copy(quoteId = quoteId)
                        val updateResult = firebaseRepository.updateLead(leadId, updatedLead)
                        
                        if (updateResult.isSuccess) {
                            android.util.Log.d("LeadsViewModel", "Quote $quoteId linked to lead $leadId")
                        } else {
                            android.util.Log.e("LeadsViewModel", "Failed to link quote to lead: ${updateResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        android.util.Log.e("LeadsViewModel", "Lead not found: $leadId")
                    }
                } else {
                    android.util.Log.e("LeadsViewModel", "Error getting lead: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LeadsViewModel", "Error linking quote to lead: ${e.message}", e)
            }
        }
    }
    
    // Synchronous version for immediate feedback
    suspend fun linkQuoteToLeadSync(leadId: String, quoteId: String): Boolean {
        return try {
            // Get the current lead
            val result = firebaseRepository.getLeadById(leadId)
            if (result.isSuccess) {
                val currentLead = result.getOrNull()
                if (currentLead != null) {
                    // Update the lead with the quote ID
                    val updatedLead = currentLead.copy(quoteId = quoteId)
                    val updateResult = firebaseRepository.updateLead(leadId, updatedLead)
                    
                    if (updateResult.isSuccess) {
                        android.util.Log.d("LeadsViewModel", "Quote $quoteId linked to lead $leadId")
                        true
                    } else {
                        android.util.Log.e("LeadsViewModel", "Failed to link quote to lead: ${updateResult.exceptionOrNull()?.message}")
                        false
                    }
                } else {
                    android.util.Log.e("LeadsViewModel", "Lead not found: $leadId")
                    false
                }
            } else {
                android.util.Log.e("LeadsViewModel", "Error getting lead: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("LeadsViewModel", "Error linking quote to lead: ${e.message}", e)
            false
        }
    }
    
    // Synchronous version for immediate feedback
    suspend fun createLeadFromQuoteSync(
        quoteId: String,
        clientName: String,
        address: String,
        email: String = "",
        phone: String = "",
        notes: String = ""
    ): Boolean {
        return try {
            val lead = FirebaseLead(
                name = clientName,
                email = email,
                phone = phone,
                status = "qualified", // Leads from quotes are typically qualified
                notes = notes.ifEmpty { "Lead created from quote. Address: $address" },
                quoteId = quoteId
            )
            
            val result = firebaseRepository.saveLead(lead)
            if (result.isSuccess) {
                android.util.Log.d("LeadsViewModel", "Lead from quote saved to Firebase: ${result.getOrNull()}")
                true
            } else {
                android.util.Log.e("LeadsViewModel", "Failed to save lead from quote to Firebase: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("LeadsViewModel", "Error creating lead from quote: ${e.message}")
            false
        }
    }
}