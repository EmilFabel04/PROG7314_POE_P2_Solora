package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.solora.R
import dev.solora.leads.LeadsViewModel
import dev.solora.data.FirebaseLead
import kotlinx.coroutines.launch

class LeadsFragment : Fragment() {
    
    private val leadsViewModel: LeadsViewModel by viewModels()
    private lateinit var leadsAdapter: LeadsAdapter
    
    // UI Elements
    private lateinit var rvLeads: RecyclerView
    private lateinit var layoutEmptyLeads: View
    private lateinit var fabAddLead: FloatingActionButton
    private lateinit var btnAddLeadFallback: Button
    private lateinit var btnAddLeadHeader: Button
    private lateinit var btnAddLeadEmpty: Button
    private lateinit var overlayAddLead: View
    
    // Form elements
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etEmail: EditText
    private lateinit var etContact: EditText
    private lateinit var spinnerSource: AutoCompleteTextView
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        android.util.Log.d("LeadsFragment", "===== UPDATED LEADS FRAGMENT ONCREATEVIEW CALLED =====")
        android.util.Log.d("LeadsFragment", "Inflating layout: R.layout.fragment_leads")
        val view = inflater.inflate(R.layout.fragment_leads, container, false)
        android.util.Log.d("LeadsFragment", "Layout inflated successfully. View type: ${view.javaClass.simpleName}")
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("LeadsFragment", "===== UPDATED LEADS FRAGMENT ONVIEWCREATED CALLED =====")
        android.util.Log.d("LeadsFragment", "View tree: ${view.javaClass.simpleName}")
        
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeLeads()
        
        android.util.Log.d("LeadsFragment", "===== LEADS FRAGMENT SETUP COMPLETED =====")
    }
    
    private fun initializeViews(view: View) {
        rvLeads = view.findViewById(R.id.rv_leads)
        layoutEmptyLeads = view.findViewById(R.id.layout_empty_leads)
        fabAddLead = view.findViewById(R.id.fab_add_lead)
        btnAddLeadFallback = view.findViewById(R.id.btn_add_lead_fallback)
        btnAddLeadHeader = view.findViewById(R.id.btn_add_lead_header)
        btnAddLeadEmpty = view.findViewById(R.id.btn_add_lead_empty)
        overlayAddLead = view.findViewById(R.id.overlay_add_lead)
        
        android.util.Log.d("LeadsFragment", "Views initialized. FAB found: ${fabAddLead != null}")
        if (fabAddLead != null) {
            android.util.Log.d("LeadsFragment", "FAB visibility: ${fabAddLead.visibility}, alpha: ${fabAddLead.alpha}")
            // Explicitly ensure FAB is visible
            fabAddLead.visibility = View.VISIBLE
            fabAddLead.alpha = 1.0f
            fabAddLead.isClickable = true
            fabAddLead.isFocusable = true
            android.util.Log.d("LeadsFragment", "FAB visibility set to VISIBLE")
        } else {
            android.util.Log.e("LeadsFragment", "FAB not found in layout!")
            // Show fallback button if FAB doesn't work
            btnAddLeadFallback.visibility = View.VISIBLE
        }
        
        // Form elements
        etFirstName = view.findViewById(R.id.et_first_name)
        etLastName = view.findViewById(R.id.et_last_name)
        etAddress = view.findViewById(R.id.et_address)
        etEmail = view.findViewById(R.id.et_email)
        etContact = view.findViewById(R.id.et_contact)
        spinnerSource = view.findViewById(R.id.spinner_source)
        btnAdd = view.findViewById(R.id.btn_add)
        btnCancel = view.findViewById(R.id.btn_cancel)
        
        // Setup spinner with lead sources
        val leadSources = arrayOf("Website", "Referral", "Cold Call", "Social Media", "Advertisement", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, leadSources)
        spinnerSource.setAdapter(adapter)
    }
    
    private fun setupRecyclerView() {
        leadsAdapter = LeadsAdapter { lead ->
            // Show lead details in professional dialog
            showLeadDetails(lead)
        }
        
        rvLeads.layoutManager = LinearLayoutManager(requireContext())
        rvLeads.adapter = leadsAdapter
    }
    
    private fun showLeadDetails(lead: FirebaseLead) {
        // Format date
        val dateText = lead.createdAt?.toDate()?.let {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
        } ?: "Unknown date"
        
        // Build detailed message
        val details = buildString {
            appendLine("═══════════════════════════")
            appendLine("LEAD DETAILS")
            appendLine("═══════════════════════════")
            appendLine()
            appendLine("ID: ${lead.id ?: "N/A"}")
            appendLine("Date: $dateText")
            appendLine()
            appendLine("CONTACT INFORMATION")
            appendLine("───────────────────────────")
            appendLine("Name: ${lead.name}")
            if (lead.email.isNotEmpty()) {
                appendLine("Email: ${lead.email}")
            }
            if (lead.phone.isNotEmpty()) {
                appendLine("Phone: ${lead.phone}")
            }
            appendLine()
            appendLine("STATUS")
            appendLine("───────────────────────────")
            appendLine(lead.status.uppercase())
            appendLine()
            if (!lead.notes.isNullOrEmpty()) {
                appendLine("NOTES")
                appendLine("───────────────────────────")
                appendLine(lead.notes)
                appendLine()
            }
            if (lead.quoteId != null) {
                appendLine("LINKED QUOTE")
                appendLine("───────────────────────────")
                appendLine("Quote ID: ${lead.quoteId}")
                appendLine()
            }
            appendLine("═══════════════════════════")
        }
        
        // Show in dialog
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Lead: ${lead.name}")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .setNeutralButton("Update Status") { _, _ ->
                // TODO: Show status update dialog
                Toast.makeText(requireContext(), "Status update coming soon", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun setupClickListeners() {
        android.util.Log.d("LeadsFragment", "Setting up click listeners. FAB found: ${::fabAddLead.isInitialized}")
        
        fabAddLead.setOnClickListener {
            android.util.Log.d("LeadsFragment", "FAB clicked - showing add lead modal")
            showAddLeadModal()
        }

        // Also setup fallback button click listener
        btnAddLeadFallback.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Fallback button clicked - showing add lead modal")
            showAddLeadModal()
        }

        // Setup header button click listener
        btnAddLeadHeader.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Header button clicked - showing add lead modal")
            showAddLeadModal()
        }

        // Setup empty state button click listener
        btnAddLeadEmpty.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Empty state button clicked - showing add lead modal")
            showAddLeadModal()
        }
        
        btnCancel.setOnClickListener {
            hideAddLeadModal()
        }
        
        btnAdd.setOnClickListener {
            addFirebaseLead()
        }
        
        // Close modal when clicking outside
        overlayAddLead.setOnClickListener {
            hideAddLeadModal()
        }
    }
    
    private fun observeLeads() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d("LeadsFragment", "Starting to observe leads")
                leadsViewModel.leads.collect { leads ->
                    android.util.Log.d("LeadsFragment", "Leads updated: ${leads.size} leads received")
                    leadsAdapter.submitList(leads)
                    
                    // Show/hide empty state
                    if (leads.isEmpty()) {
                        rvLeads.visibility = View.GONE
                        layoutEmptyLeads.visibility = View.VISIBLE
                        android.util.Log.d("LeadsFragment", "Showing empty state (no leads)")
                    } else {
                        rvLeads.visibility = View.VISIBLE
                        layoutEmptyLeads.visibility = View.GONE
                        android.util.Log.d("LeadsFragment", "Displaying ${leads.size} leads in RecyclerView")
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("LeadsFragment", "Leads observation cancelled")
            } catch (e: Exception) {
                android.util.Log.e("LeadsFragment", "Error observing leads", e)
                Toast.makeText(requireContext(), "Error loading leads: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddLeadModal() {
        overlayAddLead.visibility = View.VISIBLE
        clearForm()
    }
    
    private fun hideAddLeadModal() {
        overlayAddLead.visibility = View.GONE
    }
    
    private fun clearForm() {
        etFirstName.text.clear()
        etLastName.text.clear()
        etAddress.text.clear()
        etEmail.text.clear()
        etContact.text.clear()
        spinnerSource.text.clear()
    }
    
    private fun addFirebaseLead() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val source = spinnerSource.text.toString().trim().ifEmpty { "Other" }
        
        // Validation
        if (firstName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter first name", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter last name", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter address", Toast.LENGTH_SHORT).show()
            return
        }
        if (contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter contact information", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Generate reference number
        val reference = generateFirebaseLeadReference()
        val fullName = "$firstName $lastName"
        val contactInfo = if (email.isNotEmpty()) "$email | $contact" else contact
        
        // Add lead
        android.util.Log.d("LeadsFragment", "Adding lead: $fullName, $address, $contactInfo, $source")
        leadsViewModel.addLead(fullName, contactInfo, contactInfo, "")
        
        // Clear form and hide modal
        clearForm()
        hideAddLeadModal()
        Toast.makeText(requireContext(), "Lead added successfully!", Toast.LENGTH_SHORT).show()
    }
    
    private fun generateFirebaseLeadReference(): String {
        // Generate a simple reference number
        return (10000..99999).random().toString()
    }
}

// RecyclerView Adapter
class LeadsAdapter(
    private val onLeadClick: (FirebaseLead) -> Unit
) : RecyclerView.Adapter<LeadsAdapter.LeadViewHolder>() {
    
    private var leads: List<FirebaseLead> = emptyList()
    
    fun submitList(newLeads: List<FirebaseLead>) {
        leads = newLeads
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lead, parent, false)
        return LeadViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        holder.bind(leads[position])
    }
    
    override fun getItemCount(): Int = leads.size
    
    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReference = itemView.findViewById<android.widget.TextView>(R.id.tv_reference)
        private val tvName = itemView.findViewById<android.widget.TextView>(R.id.tv_name)
        private val tvAddress = itemView.findViewById<android.widget.TextView>(R.id.tv_address)
        private val clickableArea = itemView.findViewById<LinearLayout>(R.id.clickable_area) ?: itemView
        
        fun bind(lead: FirebaseLead) {
            tvReference.text = lead.id ?: "N/A"
            tvName.text = lead.name
            tvAddress.text = lead.email
            
            clickableArea.setOnClickListener { onLeadClick(lead) }
        }
    }
}