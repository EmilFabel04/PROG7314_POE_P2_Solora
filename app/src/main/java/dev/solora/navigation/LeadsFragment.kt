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
import dev.solora.leads.FirebaseLeadsViewModel
import dev.solora.data.FirebaseFirebaseLead
import kotlinx.coroutines.launch

class FirebaseLeadsFragment : Fragment() {
    
    private val leadsViewModel: FirebaseLeadsViewModel by viewModels()
    private lateinit var leadsAdapter: FirebaseLeadsAdapter
    
    // UI Elements
    private lateinit var rvFirebaseLeads: RecyclerView
    private lateinit var layoutEmptyFirebaseLeads: View
    private lateinit var fabAddFirebaseLead: FloatingActionButton
    private lateinit var btnAddFirebaseLeadFallback: Button
    private lateinit var btnAddFirebaseLeadHeader: Button
    private lateinit var btnAddFirebaseLeadEmpty: Button
    private lateinit var overlayAddFirebaseLead: View
    
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
        android.util.Log.d("FirebaseLeadsFragment", "===== UPDATED LEADS FRAGMENT ONCREATEVIEW CALLED =====")
        android.util.Log.d("FirebaseLeadsFragment", "Inflating layout: R.layout.fragment_leads")
        val view = inflater.inflate(R.layout.fragment_leads, container, false)
        android.util.Log.d("FirebaseLeadsFragment", "Layout inflated successfully. View type: ${view.javaClass.simpleName}")
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("FirebaseLeadsFragment", "===== UPDATED LEADS FRAGMENT ONVIEWCREATED CALLED =====")
        android.util.Log.d("FirebaseLeadsFragment", "View tree: ${view.javaClass.simpleName}")
        
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeFirebaseLeads()
        
        android.util.Log.d("FirebaseLeadsFragment", "===== LEADS FRAGMENT SETUP COMPLETED =====")
    }
    
    private fun initializeViews(view: View) {
        rvFirebaseLeads = view.findViewById(R.id.rv_leads)
        layoutEmptyFirebaseLeads = view.findViewById(R.id.layout_empty_leads)
        fabAddFirebaseLead = view.findViewById(R.id.fab_add_lead)
        btnAddFirebaseLeadFallback = view.findViewById(R.id.btn_add_lead_fallback)
        btnAddFirebaseLeadHeader = view.findViewById(R.id.btn_add_lead_header)
        btnAddFirebaseLeadEmpty = view.findViewById(R.id.btn_add_lead_empty)
        overlayAddFirebaseLead = view.findViewById(R.id.overlay_add_lead)
        
        android.util.Log.d("FirebaseLeadsFragment", "Views initialized. FAB found: ${fabAddFirebaseLead != null}")
        if (fabAddFirebaseLead != null) {
            android.util.Log.d("FirebaseLeadsFragment", "FAB visibility: ${fabAddFirebaseLead.visibility}, alpha: ${fabAddFirebaseLead.alpha}")
            // Explicitly ensure FAB is visible
            fabAddFirebaseLead.visibility = View.VISIBLE
            fabAddFirebaseLead.alpha = 1.0f
            fabAddFirebaseLead.isClickable = true
            fabAddFirebaseLead.isFocusable = true
            android.util.Log.d("FirebaseLeadsFragment", "FAB visibility set to VISIBLE")
        } else {
            android.util.Log.e("FirebaseLeadsFragment", "FAB not found in layout!")
            // Show fallback button if FAB doesn't work
            btnAddFirebaseLeadFallback.visibility = View.VISIBLE
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
        leadsAdapter = FirebaseLeadsAdapter { lead ->
            // Handle lead click - could navigate to lead detail
            Toast.makeText(requireContext(), "FirebaseLead: ${lead.name}", Toast.LENGTH_SHORT).show()
        }
        
        rvFirebaseLeads.layoutManager = LinearLayoutManager(requireContext())
        rvFirebaseLeads.adapter = leadsAdapter
    }
    
    private fun setupClickListeners() {
        android.util.Log.d("FirebaseLeadsFragment", "Setting up click listeners. FAB found: ${::fabAddFirebaseLead.isInitialized}")
        
        fabAddFirebaseLead.setOnClickListener {
            android.util.Log.d("FirebaseLeadsFragment", "FAB clicked - showing add lead modal")
            showAddFirebaseLeadModal()
        }

        // Also setup fallback button click listener
        btnAddFirebaseLeadFallback.setOnClickListener {
            android.util.Log.d("FirebaseLeadsFragment", "Fallback button clicked - showing add lead modal")
            showAddFirebaseLeadModal()
        }

        // Setup header button click listener
        btnAddFirebaseLeadHeader.setOnClickListener {
            android.util.Log.d("FirebaseLeadsFragment", "Header button clicked - showing add lead modal")
            showAddFirebaseLeadModal()
        }

        // Setup empty state button click listener
        btnAddFirebaseLeadEmpty.setOnClickListener {
            android.util.Log.d("FirebaseLeadsFragment", "Empty state button clicked - showing add lead modal")
            showAddFirebaseLeadModal()
        }
        
        btnCancel.setOnClickListener {
            hideAddFirebaseLeadModal()
        }
        
        btnAdd.setOnClickListener {
            addFirebaseLead()
        }
        
        // Close modal when clicking outside
        overlayAddFirebaseLead.setOnClickListener {
            hideAddFirebaseLeadModal()
        }
    }
    
    private fun observeFirebaseLeads() {
        viewLifecycleOwner.lifecycleScope.launch {
            leadsViewModel.leads.collect { leads ->
                leadsAdapter.submitList(leads)
                
                // Show/hide empty state
                if (leads.isEmpty()) {
                    rvFirebaseLeads.visibility = View.GONE
                    layoutEmptyFirebaseLeads.visibility = View.VISIBLE
                } else {
                    rvFirebaseLeads.visibility = View.VISIBLE
                    layoutEmptyFirebaseLeads.visibility = View.GONE
                }
            }
        }
    }
    
    private fun showAddFirebaseLeadModal() {
        overlayAddFirebaseLead.visibility = View.VISIBLE
        clearForm()
    }
    
    private fun hideAddFirebaseLeadModal() {
        overlayAddFirebaseLead.visibility = View.GONE
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
        android.util.Log.d("FirebaseLeadsFragment", "Adding lead: $fullName, $address, $contactInfo, $source")
        leadsViewModel.addLead(fullName, contactInfo, contactInfo, notes)
        
        // Clear form and hide modal
        clearForm()
        hideAddFirebaseLeadModal()
        Toast.makeText(requireContext(), "FirebaseLead added successfully!", Toast.LENGTH_SHORT).show()
    }
    
    private fun generateFirebaseLeadReference(): String {
        // Generate a simple reference number
        return (10000..99999).random().toString()
    }
}

// RecyclerView Adapter
class FirebaseLeadsAdapter(
    private val onFirebaseLeadClick: (FirebaseLead) -> Unit
) : RecyclerView.Adapter<FirebaseLeadsAdapter.FirebaseLeadViewHolder>() {
    
    private var leads: List<FirebaseLead> = emptyList()
    
    fun submitList(newFirebaseLeads: List<FirebaseLead>) {
        leads = newFirebaseLeads
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FirebaseLeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lead, parent, false)
        return FirebaseLeadViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FirebaseLeadViewHolder, position: Int) {
        holder.bind(leads[position])
    }
    
    override fun getItemCount(): Int = leads.size
    
    inner class FirebaseLeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReference = itemView.findViewById<android.widget.TextView>(R.id.tv_reference)
        private val tvName = itemView.findViewById<android.widget.TextView>(R.id.tv_name)
        private val tvAddress = itemView.findViewById<android.widget.TextView>(R.id.tv_address)
        private val clickableArea = itemView.findViewById<LinearLayout>(R.id.clickable_area) ?: itemView
        
        fun bind(lead: FirebaseLead) {
            tvReference.text = lead.id ?: "N/A"
            tvName.text = lead.name
            tvAddress.text = lead.email
            
            clickableArea.setOnClickListener { onFirebaseLeadClick(lead) }
        }
    }
}