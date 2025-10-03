package dev.solora.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.quotes.CalculationState
import dev.solora.leads.LeadsViewModel
import dev.solora.settings.SettingsViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuoteFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var isFirebaseTest = false
    private var currentTab = 0 // 0: calculate, 1: view, 2: dashboard

    // Tab containers
    private lateinit var tabCalculateContainer: LinearLayout
    private lateinit var tabViewContainer: LinearLayout
    private lateinit var tabDashboardContainer: LinearLayout

    // Tab text views
    private lateinit var tabCalculateText: TextView
    private lateinit var tabViewText: TextView
    private lateinit var tabDashboardText: TextView

    // Content views
    private lateinit var contentCalculate: View
    private lateinit var contentView: View
    private lateinit var contentDashboard: View
    
    // Calculate tab elements
    private lateinit var etAddress: TextInputEditText
    private lateinit var etUsage: TextInputEditText
    private lateinit var etBill: TextInputEditText
    private lateinit var etTariff: TextInputEditText
    private lateinit var etPanel: TextInputEditText
    private lateinit var btnCalculate: LinearLayout
    private lateinit var txtCalculate: TextView
    
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
    private lateinit var btnSaveFinalQuote: LinearLayout

    // Quote Details Section
    private lateinit var tvQuoteSummary: TextView
    private var layoutQuoteDetails: LinearLayout? = null
    private var txtNumberOfPanels: TextView? = null
    private var txtTotalSystemSize: TextView? = null
    private var txtRecommendedInverter: TextView? = null
    private var txtEstimatedMonthlySavings: TextView? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote, container, false)
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
        setupClientDropdown(view)
    }

    private fun initializeViews(view: View) {
        // Tab containers
        tabCalculateContainer = view.findViewById(R.id.tab_calculate)
        tabViewContainer = view.findViewById(R.id.tab_view)
        tabDashboardContainer = view.findViewById(R.id.tab_dashboard)

        // Tab TextViews
        tabCalculateText = view.findViewById(R.id.txt_tab_calculate)
        tabViewText = view.findViewById(R.id.txt_tab_view)
        tabDashboardText = view.findViewById(R.id.txt_tab_dashboard)

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
        txtCalculate = view.findViewById(R.id.txt_calculate)
        
        // View tab elements (quotes list)
        rvQuotesList = view.findViewById(R.id.rv_quotes_list)
        layoutEmptyQuotes = view.findViewById(R.id.layout_empty_quotes)
        
        // Dashboard tab elements - Client details
        etReference = view.findViewById(R.id.et_reference)
        etFirstName = view.findViewById(R.id.et_first_name)
        etLastName = view.findViewById(R.id.et_last_name)
        etClientAddress = view.findViewById(R.id.et_client_address)
        etEmail = view.findViewById(R.id.et_email)
        etContact = view.findViewById(R.id.et_contact)

        // Quote details elements
        tvQuoteSummary = view.findViewById(R.id.tv_quote_summary)
        layoutQuoteDetails = view.findViewById(R.id.quote_details_section)
        txtNumberOfPanels = view.findViewById(R.id.txt_number_of_panels)
        txtTotalSystemSize = view.findViewById(R.id.txt_total_system_size)
        txtRecommendedInverter = view.findViewById(R.id.txt_recommended_inverter)
        txtEstimatedMonthlySavings = view.findViewById(R.id.txt_estimated_monthly_savings)
        btnSaveFinalQuote = view.findViewById(R.id.btn_save_final_quote)
    }

    private fun setupTabs() {
        tabCalculateContainer.setOnClickListener { switchToTab(0) }
        tabViewContainer.setOnClickListener { switchToTab(1) }
        tabDashboardContainer.setOnClickListener { switchToTab(2) }

        // Initially show calculate tab
        switchToTab(0)
    }

    private fun switchToTab(tab: Int) {
        currentTab = tab

        // Show/hide content
        contentCalculate.visibility = if (tab == 0) View.VISIBLE else View.GONE
        contentView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        contentDashboard.visibility = if (tab == 2) View.VISIBLE else View.GONE

        // Update tab appearance
        updateTabAppearance()
    }

    private fun updateTabAppearance() {
        // Reset all tabs
        tabCalculateContainer.setBackgroundResource(R.drawable.tab_unselected_left)
        tabViewContainer.setBackgroundResource(R.drawable.tab_unselected_middle)
        tabDashboardContainer.setBackgroundResource(R.drawable.tab_unselected_right)

        tabCalculateText.setTextColor(resources.getColor(R.color.solora_orange, null))
        tabViewText.setTextColor(resources.getColor(R.color.solora_orange, null))
        tabDashboardText.setTextColor(resources.getColor(R.color.solora_orange, null))

        // Highlight current tab
        when (currentTab) {
            0 -> {
                tabCalculateContainer.setBackgroundResource(R.drawable.tab_selected_left)
                tabCalculateText.setTextColor(resources.getColor(android.R.color.white, null))
            }
            1 -> {
                tabViewContainer.setBackgroundResource(R.drawable.tab_selected_middle)
                tabViewText.setTextColor(resources.getColor(android.R.color.white, null))
            }
            2 -> {
                tabDashboardContainer.setBackgroundResource(R.drawable.tab_selected_right)
                tabDashboardText.setTextColor(resources.getColor(android.R.color.white, null))
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
                // Get current settings for defaults
                val currentSettings = settingsViewModel.settings.value
                android.util.Log.d("QuoteFragment", "Calculate clicked - Using settings: Tariff=${currentSettings.calculationSettings.defaultTariff}, Panel=${currentSettings.calculationSettings.defaultPanelWatt}W")
                
                val usage = if (usageText.isNotEmpty()) usageText.toDouble() else null
                val bill = if (billText.isNotEmpty()) billText.toDouble() else null
                val tariff = if (tariffText.isNotEmpty()) tariffText.toDouble() else currentSettings.calculationSettings.defaultTariff
                val panelWatt = if (panelText.isNotEmpty()) panelText.toInt() else currentSettings.calculationSettings.defaultPanelWatt
                
                android.util.Log.d("QuoteFragment", "Final calculation values: Tariff=$tariff, PanelWatt=$panelWatt")
                
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
            val bundle = Bundle().apply { putString("id", quote.id) }
            findNavController().navigate(R.id.quoteDetailFragment, bundle)
        }

        rvQuotesList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvQuotesList.adapter = quotesAdapter
        
        // Observe quotes from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                quotesViewModel.quotes.collect { quotes ->
                    android.util.Log.d("QuoteFragment", "Quotes updated: ${quotes.size} quotes")
                    
                    if (quotes.isEmpty()) {
                        layoutEmptyQuotes.visibility = View.VISIBLE
                        rvQuotesList.visibility = View.GONE
                        android.util.Log.d("QuoteFragment", "Showing empty state")
                    } else {
                        layoutEmptyQuotes.visibility = View.GONE
                        rvQuotesList.visibility = View.VISIBLE
                        quotesAdapter.submitList(quotes)
                        android.util.Log.d("QuoteFragment", "Displaying ${quotes.size} quotes in RecyclerView")
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("QuoteFragment", "Quotes observation cancelled")
            } catch (e: Exception) {
                android.util.Log.e("QuoteFragment", "Error observing quotes", e)
            }
        }
    }

    private fun setupClientDropdown(view: View) {
        val etClientDropdown = view.findViewById<AutoCompleteTextView>(R.id.et_client_dropdown)
        val btnDropdown = view.findViewById<ImageButton>(R.id.btn_dropdown)
        val tvNoClients = view.findViewById<TextView>(R.id.tv_no_clients)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                leadsViewModel.leads.collect { clients ->
                    if (clients.isEmpty()) {
                        etClientDropdown.setAdapter(null)
                        tvNoClients.visibility = View.VISIBLE
                    } else {
                        tvNoClients.visibility = View.GONE

                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.item_client_dropdown,
                            clients.map { it.name }
                        )
                        etClientDropdown.setAdapter(adapter)

                        etClientDropdown.setOnItemClickListener { _, _, pos, _ ->
                            val selectedClient = clients[pos]
                            etClientDropdown.setText(selectedClient.name, false)
                        }

                        btnDropdown.setOnClickListener {
                            if (!etClientDropdown.isPopupShowing) {
                                etClientDropdown.showDropDown()
                            } else {
                                etClientDropdown.dismissDropDown()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupDashboardTab() {
        btnSaveFinalQuote.setOnClickListener {
            saveQuoteWithClientDetails()
        }
    }

    private fun updateResultsTab(calculation: dev.solora.quote.QuoteOutputs) {
        // If the structured details layout exists, populate fields there and show it.
        if (layoutQuoteDetails != null &&
            txtNumberOfPanels != null &&
            txtTotalSystemSize != null &&
            txtRecommendedInverter != null &&
            txtEstimatedMonthlySavings != null
        ) {
            txtNumberOfPanels!!.text = calculation.panels.toString()
            txtTotalSystemSize!!.text = "${String.format("%.1f", calculation.systemKw)} kW"
            txtRecommendedInverter!!.text = "${String.format("%.1f", calculation.inverterKw)} kW"
            txtEstimatedMonthlySavings!!.text =
                "R ${String.format("%.2f", calculation.monthlySavingsRands)}"

            // Show structured layout, hide the old summary TextView
            layoutQuoteDetails!!.visibility = View.VISIBLE
            tvQuoteSummary.visibility = View.GONE
        } else {
            // Fallback: still populate the fields if they're there
            tvQuoteSummary.visibility = View.GONE // hide the placeholder

            txtNumberOfPanels?.text = calculation.panels.toString()
            txtTotalSystemSize?.text = String.format("%.1f kW", calculation.systemKw)
            txtRecommendedInverter?.text = String.format("%.1f kW", calculation.inverterKw)
            txtEstimatedMonthlySavings?.text =
                "R ${String.format("%.2f", calculation.monthlySavingsRands)}"

            // If structured layout is not available at all, show old summary instead
            if (layoutQuoteDetails == null) {
                tvQuoteSummary.visibility = View.VISIBLE
                tvQuoteSummary.text = """
            Number of Panels: ${calculation.panels}
            Total System Size: ${String.format("%.1f", calculation.systemKw)} kW
            Recommended Inverter: ${String.format("%.1f", calculation.inverterKw)} kW
            Estimated Monthly Savings: R ${String.format("%.2f", calculation.monthlySavingsRands)}
            Estimated Monthly Generation: ${
                    String.format(
                        "%.0f",
                        calculation.estimatedMonthlyGeneration
                    )
                } kWh
            Payback Period: ${calculation.paybackMonths} months
        """.trimIndent()
            } else {
                layoutQuoteDetails?.visibility = View.VISIBLE
            }
        }
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
        Log.d("QuoteFragment", "saveQuoteWithClientDetails called")

        quotesViewModel.lastCalculation.value?.let { calculation ->
            Log.d("QuoteFragment", "Found calculation to save: ${calculation.systemKw}kW system")

            val reference = etReference.text.toString().trim().ifEmpty { "QUOTE-${System.currentTimeMillis()}" }
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val clientName = when {
                firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
                firstName.isNotEmpty() -> firstName
                lastName.isNotEmpty() -> lastName
                else -> "Unknown Client"
            }
            val address = etClientAddress.text.toString().trim().ifEmpty { "Unknown Address" }
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val contactInfo = if (email.isNotEmpty() && contact.isNotEmpty()) "$email | $contact" else email.ifEmpty { contact }

            if (clientName == "Unknown Client") {
                Toast.makeText(requireContext(), "Please enter client name", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("QuoteFragment", "Calling saveQuoteFromCalculation...")
            quotesViewModel.saveQuoteFromCalculation(reference, clientName, address, calculation)

            // Collect the StateFlow in a lifecycle-aware manner
            viewLifecycleOwner.lifecycleScope.launch {
                quotesViewModel.lastQuote
                    .filterNotNull()
                    .first()
                    .let { savedQuote ->
                        savedQuote.id?.let { quoteId ->
                            Log.d("QuoteFragment", "Creating lead for quote ID: $quoteId")
                            leadsViewModel.createLeadFromQuote(
                                quoteId = quoteId,
                                clientName = clientName,
                                address = address,
                                contactInfo = contactInfo,
                                notes = "Lead created from quote $reference. System: ${String.format("%.2f", calculation.systemKw)}kW, Monthly savings: R${String.format("%.2f", calculation.monthlySavingsRands)}"
                            )

                            Toast.makeText(requireContext(), "Quote saved and lead created successfully!", Toast.LENGTH_LONG).show()
                            switchToTab(1)
                        } ?: run {
                            Log.e("QuoteFragment", "savedQuote has null ID")
                            Toast.makeText(requireContext(), "Quote saved but lead creation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } ?: run {
            Toast.makeText(requireContext(), "No calculation to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.calculationState.collect { state ->
                when (state) {
                    is CalculationState.Idle -> {
                        btnCalculate.isEnabled = true
                        txtCalculate.text = "calculate"
                    }
                    is CalculationState.Loading -> {
                        btnCalculate.isEnabled = false
                        txtCalculate.text = "calculating..."
                    }
                    is CalculationState.Success -> {
                        btnCalculate.isEnabled = true
                        txtCalculate.text = "calculate"
                        Toast.makeText(requireContext(), "Calculation complete! Enter client details to save.", Toast.LENGTH_LONG).show()

                        updateResultsTab(state.outputs)
                        switchToTab(2)
                    }
                    is CalculationState.Error -> {
                        btnCalculate.isEnabled = true
                        txtCalculate.text = "calculate"
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
                    android.util.Log.d("QuoteFragment", "Settings received: Tariff=${settings.calculationSettings.defaultTariff}, Panel=${settings.calculationSettings.defaultPanelWatt}W")
                    
                    // ALWAYS update fields with latest settings values
                    etTariff.setText(settings.calculationSettings.defaultTariff.toString())
                    etPanel.setText(settings.calculationSettings.defaultPanelWatt.toString())
                    
                    // Update hints as well
                    etTariff.hint = "Tariff (R/kWh)"
                    etPanel.hint = "Panel Wattage"
                    
                    android.util.Log.d("QuoteFragment", "Form fields updated with settings values")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when fragment is destroyed, don't show error
                android.util.Log.d("QuoteFragment", "Settings observation cancelled (fragment lifecycle ended)")
            } catch (e: Exception) {
                // Only show error for unexpected exceptions
                android.util.Log.e("QuoteFragment", "Error observing settings", e)
                Toast.makeText(requireContext(), "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}