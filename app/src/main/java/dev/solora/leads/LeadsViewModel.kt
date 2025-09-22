package dev.solora.leads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.solora.SoloraApp
import dev.solora.data.Lead
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeadsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as SoloraApp).database
    private val dao = db.leadDao()

    val leads = dao.observeLeads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLead(reference: String, name: String, address: String, contact: String) {
        viewModelScope.launch { dao.insert(Lead(reference = reference, name = name, address = address, contact = contact)) }
    }
}


