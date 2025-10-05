package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var btnAddLeadEmpty: Button
    private lateinit var overlayAddLead: View
    
    // Form elements
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etEmail: EditText
    private lateinit var etContact: EditText
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
        btnAddLeadEmpty = view.findViewById(R.id.btn_add_lead_empty)
        overlayAddLead = view.findViewById(R.id.overlay_add_lead)
        
        android.util.Log.d("LeadsFragment", "Views initialized. FAB found: ${fabAddLead != null}")
        if (fabAddLead != null) {
            android.util.Log.d("LeadsFragment", "FAB initialized - visibility will be controlled by observeLeads()")
            android.util.Log.d("LeadsFragment", "FAB initial visibility: ${fabAddLead.visibility}")
            fabAddLead.isClickable = true
            fabAddLead.isFocusable = true
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
        btnAdd = view.findViewById(R.id.btn_add)
        btnCancel = view.findViewById(R.id.btn_cancel)
        
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_lead_details, null)
        
        // Initialize views
        val tvLeadName = dialogView.findViewById<TextView>(R.id.tv_lead_name)
        val tvLeadEmail = dialogView.findViewById<TextView>(R.id.tv_lead_email)
        val tvLeadPhone = dialogView.findViewById<TextView>(R.id.tv_lead_phone)
        val tvLeadId = dialogView.findViewById<TextView>(R.id.tv_lead_id)
        val tvLeadDate = dialogView.findViewById<TextView>(R.id.tv_lead_date)
        val tvLeadStatus = dialogView.findViewById<TextView>(R.id.tv_lead_status)
        val tvLeadNotes = dialogView.findViewById<TextView>(R.id.tv_lead_notes)
        val tvQuoteId = dialogView.findViewById<TextView>(R.id.tv_quote_id)
        val cardLeadNotes = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.card_lead_notes)
        val cardLinkedQuote = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.card_linked_quote)
        
        // Format date
        val dateText = lead.createdAt?.toDate()?.let {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
        } ?: "Unknown date"
        
        // Populate data
        tvLeadName.text = lead.name
        tvLeadEmail.text = if (lead.email.isNotEmpty()) lead.email else "Not provided"
        tvLeadPhone.text = if (lead.phone.isNotEmpty()) lead.phone else "Not provided"
        tvLeadId.text = lead.id ?: "N/A"
        tvLeadDate.text = dateText
        tvLeadStatus.text = lead.status.uppercase()
        
        // Show/hide notes card
        if (!lead.notes.isNullOrEmpty()) {
            tvLeadNotes.text = lead.notes
            cardLeadNotes.visibility = View.VISIBLE
        } else {
            cardLeadNotes.visibility = View.GONE
        }
        
        // Show/hide linked quote card
        if (lead.quoteId != null) {
            tvQuoteId.text = "Quote ID: ${lead.quoteId}"
            cardLinkedQuote.visibility = View.VISIBLE
        } else {
            cardLinkedQuote.visibility = View.GONE
        }
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up button click listeners
        dialogView.findViewById<ImageButton>(R.id.btn_close_lead_details).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_close_lead_details_action).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_update_status).setOnClickListener {
            // TODO: Show status update dialog
            Toast.makeText(requireContext(), "Status update coming soon", Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
    
    private fun setupClickListeners() {
        android.util.Log.d("LeadsFragment", "Setting up click listeners. FAB found: ${::fabAddLead.isInitialized}")
        
        if (::fabAddLead.isInitialized) {
            fabAddLead.setOnClickListener {
                android.util.Log.d("LeadsFragment", "FAB clicked - showing add lead modal")
                showAddLeadModal()
            }
            android.util.Log.d("LeadsFragment", "FAB click listener set up successfully")
        } else {
            android.util.Log.e("LeadsFragment", "FAB not initialized - cannot set click listener")
        }

        // Also setup fallback button click listener
        btnAddLeadFallback.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Fallback button clicked - showing add lead modal")
            showAddLeadModal()
        }


        // Setup empty state button click listener
        btnAddLeadEmpty.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Empty state button clicked - showing add lead modal")
            showAddLeadModal()
        }
        
        btnAdd.setOnClickListener {
            addFirebaseLead()
        }
        
        btnCancel.setOnClickListener {
            android.util.Log.d("LeadsFragment", "Cancel button clicked - hiding add lead modal")
            hideAddLeadModal()
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
                    
                    // Show/hide empty state and FAB
                    if (leads.isEmpty()) {
                        rvLeads.visibility = View.GONE
                        layoutEmptyLeads.visibility = View.VISIBLE
                        if (::fabAddLead.isInitialized) {
                            fabAddLead.visibility = View.GONE
                            android.util.Log.d("LeadsFragment", "FAB visibility set to GONE")
                        }
                        android.util.Log.d("LeadsFragment", "Showing empty state (no leads) - hiding FAB")
                    } else {
                        rvLeads.visibility = View.VISIBLE
                        layoutEmptyLeads.visibility = View.GONE
                        if (::fabAddLead.isInitialized) {
                            fabAddLead.visibility = View.VISIBLE
                            android.util.Log.d("LeadsFragment", "FAB visibility set to VISIBLE")
                            android.util.Log.d("LeadsFragment", "FAB actual visibility after setting: ${fabAddLead.visibility}")
                        } else {
                            android.util.Log.e("LeadsFragment", "FAB not initialized when trying to show it!")
                        }
                        android.util.Log.d("LeadsFragment", "Displaying ${leads.size} leads in RecyclerView - showing FAB")
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
    }
    
    private fun addFirebaseLead() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val source = "Other"
        
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
        
        // Add lead with separate email and phone fields
        android.util.Log.d("LeadsFragment", "Adding lead: $fullName, email: $email, phone: $contact")
        leadsViewModel.addLead(fullName, email, contact, "")
        
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