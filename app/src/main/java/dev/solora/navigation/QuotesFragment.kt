package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import dev.solora.settings.SettingsViewModel
import kotlinx.coroutines.launch

class QuotesFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
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
    
    // View tab elements (quotes list)
    private lateinit var rvQuotesList: androidx.recyclerview.widget.RecyclerView
    private lateinit var layoutEmptyQuotes: View
    private lateinit var quotesAdapter: QuotesListAdapter
    
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
        observeSettings()
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
        
        // View tab elements (quotes list)
        rvQuotesList = view.findViewById(R.id.rv_quotes_list)
        layoutEmptyQuotes = view.findViewById(R.id.layout_empty_quotes)
        
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
        // Add NASA API test on long press
        btnCalculate.setOnLongClickListener {
            testNasaApi()
            true
        }
        
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
                // Get current settings for defaults
                val currentSettings = settingsViewModel.settings.value
                android.util.Log.d("QuotesFragment", "Calculate clicked - Using settings: Tariff=${currentSettings.calculationSettings.defaultTariff}, Panel=${currentSettings.calculationSettings.defaultPanelWatt}W")
                
                val usage = if (usageText.isNotEmpty()) usageText.toDouble() else null
                val bill = if (billText.isNotEmpty()) billText.toDouble() else null
                val tariff = if (tariffText.isNotEmpty()) tariffText.toDouble() else currentSettings.calculationSettings.defaultTariff
                val panelWatt = if (panelText.isNotEmpty()) panelText.toInt() else currentSettings.calculationSettings.defaultPanelWatt
                
                android.util.Log.d("QuotesFragment", "Final calculation values: Tariff=$tariff, PanelWatt=$panelWatt")
                
                // Use temporary values for reference and client name during calculation
                quotesViewModel.calculateAdvanced(
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
        // Setup RecyclerView
        quotesAdapter = QuotesListAdapter { quote ->
            // Navigate to quote detail page
            android.util.Log.d("QuotesFragment", "Quote clicked: ID=${quote.id}, Reference=${quote.reference}")
            
            if (quote.id.isNullOrBlank()) {
                android.util.Log.e("QuotesFragment", "ERROR: Quote ID is null or blank!")
                Toast.makeText(requireContext(), "Error: Quote ID is missing", Toast.LENGTH_SHORT).show()
                return@QuotesListAdapter
            }
            
            val bundle = Bundle().apply { putString("id", quote.id) }
            findNavController().navigate(R.id.quoteDetailFragment, bundle)
        }
        rvQuotesList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvQuotesList.adapter = quotesAdapter
        
        // Observe quotes from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                quotesViewModel.quotes.collect { quotes ->
                    android.util.Log.d("QuotesFragment", "Quotes updated: ${quotes.size} quotes")
                    
                    if (quotes.isEmpty()) {
                        layoutEmptyQuotes.visibility = View.VISIBLE
                        rvQuotesList.visibility = View.GONE
                        android.util.Log.d("QuotesFragment", "Showing empty state")
                    } else {
                        layoutEmptyQuotes.visibility = View.GONE
                        rvQuotesList.visibility = View.VISIBLE
                        quotesAdapter.submitList(quotes)
                        android.util.Log.d("QuotesFragment", "Displaying ${quotes.size} quotes in RecyclerView")
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("QuotesFragment", "Quotes observation cancelled")
            } catch (e: Exception) {
                android.util.Log.e("QuotesFragment", "Error observing quotes", e)
            }
        }
    }
    
    private fun setupDashboardTab() {
        btnSaveFinalQuote.setOnClickListener {
            saveQuoteWithClientDetails()
        }
    }
    
    private fun updateResultsTab(calculation: dev.solora.quote.QuoteOutputs) {
        // Update dashboard tab summary
        tvQuoteSummary.text = buildString {
            appendLine("Number of Panels: ${calculation.panels}")
            appendLine("Total System Size: ${String.format("%.1f", calculation.systemKw)} kW")
            appendLine("Recommended Inverter: ${String.format("%.1f", calculation.inverterKw)} kW")
            appendLine("Estimated Monthly Savings: R ${String.format("%.2f", calculation.monthlySavingsRands)}")
            appendLine("Estimated Monthly Generation: ${String.format("%.0f", calculation.estimatedMonthlyGeneration)} kWh")
            appendLine("Payback Period: ${calculation.paybackMonths} months")
            
            // Add NASA data if available
            calculation.detailedAnalysis?.let { analysis ->
                appendLine("")
                appendLine("NASA Solar Data:")
                analysis.locationData?.let { location ->
                    appendLine("Location: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}")
                    appendLine("Average Annual Irradiance: ${String.format("%.1f", location.averageAnnualIrradiance)} kWh/m²/day")
                    appendLine("Average Annual Sun Hours: ${String.format("%.1f", location.averageAnnualSunHours)} hours/day")
                }
                analysis.optimalMonth?.let { month ->
                    appendLine("Optimal Solar Month: ${getMonthName(month)}")
                }
                analysis.averageTemperature?.let { temp ->
                    appendLine("Average Temperature: ${String.format("%.1f", temp)}°C")
                }
            }
        }
        
        android.util.Log.d("QuotesFragment", "Results updated: ${calculation.systemKw}kW system, R${calculation.monthlySavingsRands} savings")
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }

    private fun saveQuoteWithClientDetails() {
        android.util.Log.d("QuotesFragment", "saveQuoteWithClientDetails called")
        
        quotesViewModel.lastCalculation.value?.let { calculation ->
            android.util.Log.d("QuotesFragment", "Found calculation to save: ${calculation.systemKw}kW system")
            
            // Get client details from the form
            val reference = etReference.text.toString().trim().ifEmpty { "QUOTE-${System.currentTimeMillis()}" }
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val clientName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                "$firstName $lastName"
            } else if (firstName.isNotEmpty()) {
                firstName
            } else if (lastName.isNotEmpty()) {
                lastName
            } else {
                "Unknown Client"
            }
            val address = etClientAddress.text.toString().trim().ifEmpty { "Unknown Address" }
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val contactInfo = if (email.isNotEmpty() && contact.isNotEmpty()) "$email | $contact" else email.ifEmpty { contact }
            
            android.util.Log.d("QuotesFragment", "Quote details - Ref: $reference, Client: $clientName, Address: $address")
            
            if (clientName == "Unknown Client") {
                Toast.makeText(requireContext(), "Please enter client name", Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("QuotesFragment", "Calling saveQuoteFromCalculation...")
            
            // Save the quote with all details
            quotesViewModel.saveQuoteFromCalculation(reference, clientName, address, calculation)
            
            // Wait for quote to be saved, then create lead
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Wait a bit for the quote to be saved
                    kotlinx.coroutines.delay(1000)
                    
                    val savedQuote = quotesViewModel.lastQuote.value
                    if (savedQuote != null && savedQuote.id != null) {
                        android.util.Log.d("QuotesFragment", "Creating lead for quote ID: ${savedQuote.id}")
                        
                        // Create a lead from this quote
                        leadsViewModel.createLeadFromQuote(
                            quoteId = savedQuote.id!!,
                            clientName = clientName,
                            address = address,
                            contactInfo = contactInfo,
                            notes = "Lead created from quote $reference. System: ${String.format("%.2f", calculation.systemKw)}kW, Monthly savings: R${String.format("%.2f", calculation.monthlySavingsRands)}"
                        )
                        
                        android.util.Log.d("QuotesFragment", "Lead created, showing success message")
                        Toast.makeText(requireContext(), "Quote saved and lead created successfully!", Toast.LENGTH_LONG).show()
                        
                        // Switch to view tab to show the saved quote in the list
                        android.util.Log.d("QuotesFragment", "Switching to view tab")
                        switchToTab(1)
                    } else {
                        android.util.Log.e("QuotesFragment", "savedQuote is null or has no ID")
                        Toast.makeText(requireContext(), "Quote saved but lead creation failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuotesFragment", "Error during quote save/lead creation", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(requireContext(), "No calculation to save", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(requireContext(), "Calculation complete! Enter client details to save.", Toast.LENGTH_LONG).show()
                        
                        // Update the dashboard tab with calculation results
                        updateResultsTab(state.outputs)
                        
                        // Automatically switch to dashboard tab to show results and save
                        switchToTab(2)
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
                    // Update dashboard tab summary
                    tvQuoteSummary.text = buildString {
                        appendLine("Number of Panels: ${(quote.systemKwp * 1000 / quote.panelWatt).toInt()}")
                        appendLine("Total System Size: ${String.format("%.1f", quote.systemKwp)} kW")
                        appendLine("Recommended Inverter: ${String.format("%.1f", quote.systemKwp * 0.8)} kW")
                        appendLine("Estimated Monthly Savings: R ${String.format("%.2f", quote.monthlySavings)}")
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
    
    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                settingsViewModel.settings.collect { settings ->
                    android.util.Log.d("QuotesFragment", "Settings received: Tariff=${settings.calculationSettings.defaultTariff}, Panel=${settings.calculationSettings.defaultPanelWatt}W")
                    
                    // ALWAYS update fields with latest settings values
                    etTariff.setText(settings.calculationSettings.defaultTariff.toString())
                    etPanel.setText(settings.calculationSettings.defaultPanelWatt.toString())
                    
                    // Update hints as well
                    etTariff.hint = "Tariff (R/kWh)"
                    etPanel.hint = "Panel Wattage"
                    
                    android.util.Log.d("QuotesFragment", "Form fields updated with settings values")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when fragment is destroyed, don't show error
                android.util.Log.d("QuotesFragment", "Settings observation cancelled (fragment lifecycle ended)")
            } catch (e: Exception) {
                // Only show error for unexpected exceptions
                android.util.Log.e("QuotesFragment", "Error observing settings", e)
                Toast.makeText(requireContext(), "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Temporary NASA API test method
    private fun testNasaApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(requireContext(), "Testing NASA API...", Toast.LENGTH_SHORT).show()
                
                // Get the address from the form
                val address = etAddress.text.toString().trim()
                if (address.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an address first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                android.util.Log.d("QuotesFragment", "Testing NASA API for address: $address")
                
                // Convert address to coordinates
                val geocodingService = dev.solora.quote.GeocodingService(requireContext())
                val locationResult = geocodingService.getCoordinatesFromAddress(address)
                
                if (!locationResult.success) {
                    Toast.makeText(requireContext(), "Address not found: ${locationResult.error}", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                android.util.Log.d("QuotesFragment", "Address converted to: lat=${locationResult.latitude}, lon=${locationResult.longitude}")
                
                // Test NASA API with actual coordinates
                val nasaClient = dev.solora.quote.NasaPowerClient()
                val result = nasaClient.getSolarDataWithFallback(locationResult.latitude, locationResult.longitude)
                
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    val message = "NASA API Success!\nIrradiance: ${String.format("%.2f", data?.averageAnnualIrradiance ?: 0.0)}\nSun Hours: ${String.format("%.2f", data?.averageAnnualSunHours ?: 0.0)}"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    android.util.Log.d("QuotesFragment", "NASA API Test Success: $message")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(requireContext(), "NASA API Failed: $error", Toast.LENGTH_LONG).show()
                    android.util.Log.e("QuotesFragment", "NASA API Test Failed: $error")
                }
                
                nasaClient.close()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "NASA API Error: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("QuotesFragment", "NASA API Test Exception: ${e.message}", e)
            }
        }
    }
    
}

// QuotesListAdapter for RecyclerView
class QuotesListAdapter(
    private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit
) : androidx.recyclerview.widget.ListAdapter<dev.solora.data.FirebaseQuote, QuotesListAdapter.QuoteViewHolder>(QuoteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view, onQuoteClick)
    }
    
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class QuoteViewHolder(
        itemView: View,
        private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        
        private val tvReference: TextView = itemView.findViewById(R.id.tv_quote_reference)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_quote_date)
        private val tvClientName: TextView = itemView.findViewById(R.id.tv_client_name)
        private val tvClientAddress: TextView = itemView.findViewById(R.id.tv_client_address)
        private val tvSystemSize: TextView = itemView.findViewById(R.id.tv_system_size)
        private val tvSavings: TextView = itemView.findViewById(R.id.tv_savings)
        
        fun bind(quote: dev.solora.data.FirebaseQuote) {
            tvReference.text = quote.reference
            tvClientName.text = quote.clientName
            tvClientAddress.text = quote.address
            tvSystemSize.text = "${String.format("%.1f", quote.systemKwp)} kW"
            tvSavings.text = "R ${String.format("%.0f", quote.monthlySavings)}"
            
            // Format date
            val dateText = quote.createdAt?.toDate()?.let {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
            } ?: "Unknown"
            tvDate.text = dateText
            
            itemView.setOnClickListener {
                onQuoteClick(quote)
            }
        }
    }
    
    class QuoteDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<dev.solora.data.FirebaseQuote>() {
        override fun areItemsTheSame(oldItem: dev.solora.data.FirebaseQuote, newItem: dev.solora.data.FirebaseQuote): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: dev.solora.data.FirebaseQuote, newItem: dev.solora.data.FirebaseQuote): Boolean {
            return oldItem == newItem
        }
    }
}