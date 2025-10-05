package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.leads.LeadsViewModel
import dev.solora.quotes.QuotesViewModel
import dev.solora.quote.QuoteOutputs
import kotlinx.coroutines.launch

class ClientDetailsFragment : Fragment() {
    
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val quotesViewModel: QuotesViewModel by viewModels()
    
    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var etSelectClient: AutoCompleteTextView
    private lateinit var etReferenceNumber: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etContactNumber: TextInputEditText
    private lateinit var btnSaveLead: Button
    private lateinit var btnSaveQuote: Button
    
    private var calculationOutputs: QuoteOutputs? = null
    private var calculatedAddress: String = ""
    private var selectedLead: dev.solora.data.FirebaseLead? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_client_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        setupClientDropdown()
        
        // Get data from arguments
        arguments?.let { args ->
            calculationOutputs = args.getSerializable("calculation_outputs") as? QuoteOutputs
            calculatedAddress = args.getString("calculated_address") ?: ""
        }
        
        // Pre-populate address from calculation
        if (calculatedAddress.isNotEmpty()) {
            etAddress.setText(calculatedAddress)
        }
        
        // Generate reference number
        etReferenceNumber.setText("QUOTE-${System.currentTimeMillis().toString().takeLast(5)}")
        
        observeLeads()
    }
    
    private fun initializeViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back_client_details)
        etSelectClient = view.findViewById(R.id.et_select_client)
        etReferenceNumber = view.findViewById(R.id.et_reference_number)
        etFirstName = view.findViewById(R.id.et_first_name)
        etLastName = view.findViewById(R.id.et_last_name)
        etAddress = view.findViewById(R.id.et_address)
        etEmail = view.findViewById(R.id.et_email)
        etContactNumber = view.findViewById(R.id.et_contact_number)
        btnSaveLead = view.findViewById(R.id.btn_save_lead)
        btnSaveQuote = view.findViewById(R.id.btn_save_quote_final)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnSaveLead.setOnClickListener {
            saveClientAsLead()
        }
        
        btnSaveQuote.setOnClickListener {
            saveQuoteWithClientDetails()
        }
        
        // Handle client selection
        etSelectClient.setOnItemClickListener { _, _, position, _ ->
            val leads = leadsViewModel.getLeadsForSelection()
            if (position < leads.size) {
                selectedLead = leads[position]
                selectedLead?.let { lead ->
                    populateClientDetails(lead)
                }
            }
        }
    }
    
    private fun setupClientDropdown() {
        // This will be populated when leads are observed
    }
    
    private fun observeLeads() {
        viewLifecycleOwner.lifecycleScope.launch {
            leadsViewModel.leads.collect { leads ->
                // Show only lead names in the dropdown
                val leadNames = leads.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, leadNames)
                etSelectClient.setAdapter(adapter)
                
                // Store leads for selection
                leadsViewModel.setLeadsForSelection(leads)
            }
        }
    }
    
    private fun populateClientDetails(lead: dev.solora.data.FirebaseLead) {
        etFirstName.setText(lead.name.split(" ").firstOrNull() ?: "")
        etLastName.setText(lead.name.split(" ").drop(1).joinToString(" "))
        etEmail.setText(lead.email)
        etContactNumber.setText(lead.phone)
        // Note: FirebaseLead doesn't have address field, so we keep the pre-populated address from calculation
    }
    
    private fun saveClientAsLead() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val contact = etContactNumber.text.toString().trim()
        
        // Validation
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter client name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter contact number", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clientName = "$firstName $lastName"
        
        // Save as lead
        leadsViewModel.addLead(clientName, email, contact, "Lead created from quote calculation")
        
        // Show success message
        Toast.makeText(requireContext(), "Client saved as lead successfully!", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveQuoteWithClientDetails() {
        val reference = etReferenceNumber.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val contact = etContactNumber.text.toString().trim()
        
        // Validation
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter client name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (address.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter contact number", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clientName = "$firstName $lastName"
        val contactInfo = if (email.isNotEmpty()) "$email | $contact" else contact
        
        calculationOutputs?.let { outputs ->
            // Save the quote
            quotesViewModel.saveQuoteFromCalculation(reference, clientName, address, outputs)
            
            // Show success message and navigate back
            Toast.makeText(requireContext(), "Quote saved successfully!", Toast.LENGTH_LONG).show()
            findNavController().popBackStack(R.id.quotesFragment, false)
        } ?: run {
            Toast.makeText(requireContext(), "No calculation data available", Toast.LENGTH_SHORT).show()
        }
    }
}
