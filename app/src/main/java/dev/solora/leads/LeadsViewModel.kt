package dev.solora.leads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Lead
import dev.solora.firebase.FirebaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeadsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.leadDao()
    private val firebaseRepository = FirebaseRepository()

    val leads = dao.observeLeads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLead(reference: String, name: String, address: String, contact: String) {
        viewModelScope.launch { 
            val lead = Lead(reference = reference, name = name, address = address, contact = contact)
            
            // Save to local Room database
            dao.insert(lead)
            
            // Also save to Firebase Firestore
            try {
                val result = firebaseRepository.saveLead(lead)
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
}


