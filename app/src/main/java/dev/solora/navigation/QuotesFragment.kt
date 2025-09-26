package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.quotes.CalculationState
import dev.solora.leads.LeadsViewModel
import kotlinx.coroutines.launch

class QuotesFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private var isFirebaseTest = false
    private var currentTab = 0 // 0: calculate, 1: view, 2: dashboard
    
    // UI Elements
    private lateinit var tabCalculate: TextView
    private lateinit var tabView: TextView
    private lateinit var tabDashboard: TextView
    private lateinit var contentCalculate: View
    private lateinit var contentView: View
    private lateinit var contentDashboard: View
    
    // Calculate tab elements
    private lateinit var etAddress: TextInputEditText
    private lateinit var etUsage: TextInputEditText
    private lateinit var etBill: TextInputEditText
    private lateinit var etTariff: TextInputEditText
    private lateinit var etPanel: TextInputEditText
    private lateinit var btnCalculate: Button
    
    // View tab elements
    private lateinit var tvPanels: TextView
    private lateinit var tvSystemSize: TextView
    private lateinit var tvInverterSize: TextView
    private lateinit var tvSavings: TextView
    private lateinit var btnSaveQuote: Button
    
    // Dashboard tab elements
    private lateinit var etReference: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etClientAddress: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var tvQuoteSummary: TextView
    private lateinit var btnSaveFinalQuote: Button
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quotes, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupTabs()
        setupCalculateTab()
        setupViewTab()
        setupDashboardTab()
        observeViewModel()
    }
    
    private fun initializeViews(view: View) {
        // Tab views
        tabCalculate = view.findViewById(R.id.tab_calculate)
        tabView = view.findViewById(R.id.tab_view)
        tabDashboard = view.findViewById(R.id.tab_dashboard)
        
        // Content views
        contentCalculate = view.findViewById(R.id.content_calculate)
        contentView = view.findViewById(R.id.content_view)
        contentDashboard = view.findViewById(R.id.content_dashboard)
        
        // Calculate tab elements
        etAddress = view.findViewById(R.id.et_address)
        etUsage = view.findViewById(R.id.et_usage)
        etBill = view.findViewById(R.id.et_bill)
        etTariff = view.findViewById(R.id.et_tariff)
        etPanel = view.findViewById(R.id.et_panel)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        
        // View tab elements
        tvPanels = view.findViewById(R.id.tv_panels)
        tvSystemSize = view.findViewById(R.id.tv_system_size)
        tvInverterSize = view.findViewById(R.id.tv_inverter_size)
        tvSavings = view.findViewById(R.id.tv_savings)
        btnSaveQuote = view.findViewById(R.id.btn_save_quote)
        
        // Dashboard tab elements
        etReference = view.findViewById(R.id.et_reference)
        etFirstName = view.findViewById(R.id.et_first_name)
        etLastName = view.findViewById(R.id.et_last_name)
        etClientAddress = view.findViewById(R.id.et_client_address)
        etEmail = view.findViewById(R.id.et_email)
        etContact = view.findViewById(R.id.et_contact)
        tvQuoteSummary = view.findViewById(R.id.tv_quote_summary)
        btnSaveFinalQuote = view.findViewById(R.id.btn_save_final_quote)
    }
    
    private fun setupTabs() {
        tabCalculate.setOnClickListener { switchToTab(0) }
        tabView.setOnClickListener { switchToTab(1) }
        tabDashboard.setOnClickListener { switchToTab(2) }
        
        // Initially show calculate tab
        switchToTab(0)
    }
    
    private fun switchToTab(tab: Int) {
        currentTab = tab
        
        // Update tab appearance
        updateTabAppearance()
        
        // Show/hide content
        contentCalculate.visibility = if (tab == 0) View.VISIBLE else View.GONE
        contentView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        contentDashboard.visibility = if (tab == 2) View.VISIBLE else View.GONE
    }
    
    private fun updateTabAppearance() {
        // Reset all tabs
        tabCalculate.alpha = 0.7f
        tabView.alpha = 0.7f
        tabDashboard.alpha = 0.7f
        
        tabCalculate.setBackgroundResource(R.drawable.tab_unselected)
        tabView.setBackgroundResource(R.drawable.tab_unselected)
        tabDashboard.setBackgroundResource(R.drawable.tab_unselected)
        
        // Highlight current tab
        when (currentTab) {
            0 -> {
                tabCalculate.alpha = 1.0f
                tabCalculate.setBackgroundResource(R.drawable.tab_selected)
            }
            1 -> {
                tabView.alpha = 1.0f
                tabView.setBackgroundResource(R.drawable.tab_selected)
            }
            2 -> {
                tabDashboard.alpha = 1.0f
                tabDashboard.setBackgroundResource(R.drawable.tab_selected)
            }
        }
    }
    
    private fun setupCalculateTab() {
        btnCalculate.setOnClickListener {
            val address = etAddress.text.toString().trim()
            val usageText = etUsage.text.toString().trim()
            val billText = etBill.text.toString().trim()
            val tariffText = etTariff.text.toString().trim()
            val panelText = etPanel.text.toString().trim()
            
            // Basic validation
            if (address.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (usageText.isEmpty() && billText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter either monthly usage or bill", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val usage = if (usageText.isNotEmpty()) usageText.toDouble() else null
                val bill = if (billText.isNotEmpty()) billText.toDouble() else null
                val tariff = if (tariffText.isNotEmpty()) tariffText.toDouble() else 2.50
                val panelWatt = if (panelText.isNotEmpty()) panelText.toInt() else 420
                
                // Use temporary values for reference and client name during calculation
                quotesViewModel.calculateAndSaveUsingAddress(
                    reference = "TEMP-${System.currentTimeMillis()}",
                    clientName = "Temporary Client",
                    address = address,
                    usageKwh = usage,
                    billRands = bill,
                    tariff = tariff,
                    panelWatt = panelWatt
                )
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupViewTab() {
        btnSaveQuote.setOnClickListener {
            // Move to dashboard tab for final quote information
            switchToTab(2)
        }
    }
    
    private fun setupDashboardTab() {
        btnSaveFinalQuote.setOnClickListener {
            val reference = etReference.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val address = etClientAddress.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            
            // Basic validation
            if (reference.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in required client details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Save the complete quote with client information and create lead
            quotesViewModel.lastQuote.value?.let { quote ->
                val fullName = "$firstName $lastName"
                val contactInfo = "$email | $contact"
                
                // Update the quote in the database with real client information
                quotesViewModel.updateQuoteWithClientDetails(
                    quoteId = quote.id,
                    reference = reference,
                    clientName = fullName,
                    address = address
                )
                
                // Create a lead from this quote with client details
                android.util.Log.d("QuotesFragment", "Creating lead from quote with client details")
                val updatedQuote = quote.copy(
                    reference = reference,
                    clientName = fullName,
                    address = address
                )
                leadsViewModel.createLeadFromQuote(
                    quote = updatedQuote,
                    contactInfo = contactInfo,
                    notes = "Lead created from quote ${reference} with full client details. Client expressed interested in ${String.format("%.2f", quote.systemKw)}kW solar system."
                )
                
                Toast.makeText(requireContext(), "Quote saved and lead created successfully!", Toast.LENGTH_LONG).show()
                
                // Navigate to the quote detail (small delay to ensure database update)
                view.postDelayed({
                    val bundle = Bundle().apply { putLong("id", quote.id) }
                    findNavController().navigate(R.id.quoteDetailFragment, bundle)
                }, 300)
                
            } ?: run {
                Toast.makeText(requireContext(), "Please calculate a quote first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe calculation state
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.calculationState.collect { state ->
                when (state) {
                    is CalculationState.Idle -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "calculate"
                    }
                    is CalculationState.Loading -> {
                        btnCalculate.isEnabled = false
                        btnCalculate.text = "calculating..."
                    }
                    is CalculationState.Success -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "calculate"
                        Toast.makeText(requireContext(), "Calculation complete!", Toast.LENGTH_SHORT).show()
                        
                        // Automatically switch to view tab to show results
                        switchToTab(1)
                    }
                    is CalculationState.Error -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "calculate"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // Observe last quote for results display
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.lastQuote.collect { quote ->
                if (quote != null) {
                    // Update view tab with results
                    tvPanels.text = quote.panels.toString()
                    tvSystemSize.text = "${String.format("%.1f", quote.systemKw)} kW"
                    tvInverterSize.text = "${String.format("%.1f", quote.inverterKw)} kW"
                    tvSavings.text = "R ${String.format("%.2f", quote.savingsRands)}"
                    
                    // Update dashboard tab summary
                    tvQuoteSummary.text = buildString {
                        appendLine("Number of Panels: ${quote.panels}")
                        appendLine("Total System Size: ${String.format("%.1f", quote.systemKw)} kW")
                        appendLine("Recommended Inverter: ${String.format("%.1f", quote.inverterKw)} kW")
                        appendLine("Estimated Monthly Savings: R ${String.format("%.2f", quote.savingsRands)}")
                    }
                    
                    // Pre-fill client address from calculation
                    if (etClientAddress.text.toString().isEmpty()) {
                        etClientAddress.setText(quote.address)
                    }
                } else {
                    tvQuoteSummary.text = "Complete calculation first to see quote details"
                }
            }
        }
    }
}