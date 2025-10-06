package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.widget.ScrollView
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.quotes.CalculationState
import dev.solora.leads.LeadsViewModel
import dev.solora.settings.SettingsViewModel
import dev.solora.dashboard.DashboardViewModel
import dev.solora.dashboard.DashboardData
import kotlinx.coroutines.launch

class QuotesFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var isFirebaseTest = false
    private var currentTab = 0 // 0: calculate, 1: view, 2: dashboard
    
    // UI Elements
    private lateinit var tabCalculate: TextView
    private lateinit var tabView: TextView
    private lateinit var tabDashboard: TextView
    private lateinit var contentCalculate: View
    private lateinit var contentView: View
    private lateinit var contentDashboard: View
    private lateinit var btnBackQuotes: ImageButton
    
    // Dashboard elements
    private lateinit var dashboardContent: View
    
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
        observeDashboard()
        
        // Check if we're returning from client details (quote was saved)
        checkIfReturningFromQuoteSave()
        
        // Also check immediately if we should show view tab
        val savedQuote = quotesViewModel.lastQuote.value
        if (savedQuote != null && savedQuote.id != null) {
            // Switch to view tab immediately to show the saved quote
            switchToTab(1)
            android.util.Log.d("QuotesFragment", "OnViewCreated: Switching to view tab to show saved quote: ${savedQuote.reference}")
        }
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
        
        // Back button
        btnBackQuotes = view.findViewById(R.id.btn_back_quotes)
        
        // Dashboard content
        dashboardContent = LayoutInflater.from(requireContext()).inflate(R.layout.dashboard_content, null)
        
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
        // Back button click listener
        btnBackQuotes.setOnClickListener {
            android.util.Log.d("QuotesFragment", "Back button clicked - navigating to home")
            try {
                val parentFragment = parentFragment
                if (parentFragment is MainTabsFragment) {
                    val bottomNav = parentFragment.view?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                    bottomNav?.selectedItemId = R.id.homeFragment
                } else {
                    // Fallback to direct navigation
                    findNavController().navigate(R.id.homeFragment)
                }
            } catch (e: Exception) {
                android.util.Log.e("QuotesFragment", "Error navigating back to home: ${e.message}")
                // Fallback to direct navigation
                findNavController().navigate(R.id.homeFragment)
            }
        }
        
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
    
    private fun navigateToQuoteResults(outputs: dev.solora.quote.QuoteOutputs, address: String) {
        val bundle = Bundle().apply {
            putSerializable("calculation_outputs", outputs)
            putString("calculated_address", address)
        }
        findNavController().navigate(R.id.quoteResultsFragment, bundle)
    }
    
    private fun checkIfReturningFromQuoteSave() {
        // Check if we have a saved quote (indicates we just returned from saving a quote)
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.lastQuote.collect { quote ->
                if (quote != null && quote.id != null) {
                    // We have a saved quote, switch to view tab to show it
                    switchToTab(1)
                    android.util.Log.d("QuotesFragment", "Switching to view tab to show saved quote: ${quote.reference}")
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if we should switch to view tab when returning from client details
        val savedQuote = quotesViewModel.lastQuote.value
        if (savedQuote != null && savedQuote.id != null) {
            // Switch to view tab to show the saved quote
            switchToTab(1)
            android.util.Log.d("QuotesFragment", "OnResume: Switching to view tab to show saved quote: ${savedQuote.reference}")
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Also check on start in case onResume doesn't catch it
        val savedQuote = quotesViewModel.lastQuote.value
        if (savedQuote != null && savedQuote.id != null) {
            // Switch to view tab to show the saved quote
            switchToTab(1)
            android.util.Log.d("QuotesFragment", "OnStart: Switching to view tab to show saved quote: ${savedQuote.reference}")
        }
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
        quotesAdapter = QuotesListAdapter(
            onQuoteClick = { quote ->
                // Navigate to quote detail page
                android.util.Log.d("QuotesFragment", "Quote clicked: ID=${quote.id}, Reference=${quote.reference}")
                
                if (quote.id.isNullOrBlank()) {
                    android.util.Log.e("QuotesFragment", "ERROR: Quote ID is null or blank!")
                    Toast.makeText(requireContext(), "Error: Quote ID is missing", Toast.LENGTH_SHORT).show()
                    return@QuotesListAdapter
                }
                
                val bundle = Bundle().apply { putString("id", quote.id) }
                findNavController().navigate(R.id.quoteDetailFragment, bundle)
            },
            onQuoteLongPress = { quote ->
                // Show dialog with quote details
                showQuotePreviewDialog(quote)
            }
        )
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
    
    private fun showQuotePreviewDialog(quote: dev.solora.data.FirebaseQuote) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quote_preview, null)
        
        // Set quote data
        val address = quote.address.ifEmpty { "Address not available" }
        val systemSize = if (quote.systemKwp > 0) "${String.format("%.1f", quote.systemKwp)} kW" else "Not available"
        
        // Calculate estimated total cost based on system size (rough estimate: R8500 per kW)
        val totalCost = if (quote.systemKwp > 0) {
            val estimatedCost = quote.systemKwp * 8500
            "R ${String.format("%.0f", estimatedCost)}"
        } else {
            "Not available"
        }
        
        dialogView.findViewById<TextView>(R.id.tv_preview_address).text = address
        dialogView.findViewById<TextView>(R.id.tv_preview_system_size).text = systemSize
        dialogView.findViewById<TextView>(R.id.tv_preview_total_cost).text = totalCost
        
        // Create and show dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up button click listeners
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_close_dialog).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_view_full).setOnClickListener {
            dialog.dismiss()
            // Navigate to full quote detail
            if (!quote.id.isNullOrBlank()) {
                val bundle = Bundle().apply { putString("id", quote.id) }
                findNavController().navigate(R.id.quoteDetailFragment, bundle)
            }
        }
        
        dialog.show()
        android.util.Log.d("QuotesFragment", "Quote preview dialog shown for: $address")
    }
    
    private fun setupDashboardTab() {
        // Clear existing content and add dashboard
        val viewGroup = contentDashboard as? ViewGroup
        viewGroup?.let {
            it.removeAllViews()
            it.addView(dashboardContent)
        }
        
        // Initialize dashboard UI elements
        initializeDashboardViews()
        
        // Load dashboard data when tab is shown
        dashboardViewModel.loadDashboardData()
    }
    
    private fun initializeDashboardViews() {
        // Statistics cards
        // These will be populated by observeDashboard()
    }
    
    private fun observeDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.dashboardData.collect { dashboardData ->
                updateDashboardUI(dashboardData)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.isLoading.collect { isLoading ->
                // Show/hide loading indicator if needed
                if (isLoading) {
                    android.util.Log.d("QuotesFragment", "Dashboard loading...")
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.error.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Dashboard Error: $error", Toast.LENGTH_LONG).show()
                    android.util.Log.e("QuotesFragment", "Dashboard error: $error")
                }
            }
        }
    }
    
    private fun updateDashboardUI(dashboardData: DashboardData) {
        try {
            // Update statistics cards
            dashboardContent.findViewById<TextView>(R.id.tv_total_quotes).text = dashboardData.totalQuotes.toString()
            dashboardContent.findViewById<TextView>(R.id.tv_avg_system_size).text = String.format("%.1f", dashboardData.averageSystemSize)
            dashboardContent.findViewById<TextView>(R.id.tv_total_revenue).text = "R${String.format("%.0f", dashboardData.totalRevenue)}"
            dashboardContent.findViewById<TextView>(R.id.tv_avg_savings).text = "R${String.format("%.0f", dashboardData.averageMonthlySavings)}"
            
            // Update circle chart with system size distribution
            val circleChart = dashboardContent.findViewById<dev.solora.ui.views.CircleChartView>(R.id.circle_chart)
            val labels = listOf("0-3 kW", "3-6 kW", "6-10 kW", "10+ kW")
            val values = listOf(
                dashboardData.systemSizeDistribution.size0to3kw.toFloat(),
                dashboardData.systemSizeDistribution.size3to6kw.toFloat(),
                dashboardData.systemSizeDistribution.size6to10kw.toFloat(),
                dashboardData.systemSizeDistribution.size10kwPlus.toFloat()
            )
            circleChart.setChartDataSimple(labels, values)
            
            // Update legend
            dashboardContent.findViewById<TextView>(R.id.tv_legend_0_3kw).text = "0-3 kW (${dashboardData.systemSizeDistribution.size0to3kw})"
            dashboardContent.findViewById<TextView>(R.id.tv_legend_3_6kw).text = "3-6 kW (${dashboardData.systemSizeDistribution.size3to6kw})"
            dashboardContent.findViewById<TextView>(R.id.tv_legend_6_10kw).text = "6-10 kW (${dashboardData.systemSizeDistribution.size6to10kw})"
            dashboardContent.findViewById<TextView>(R.id.tv_legend_10kw_plus).text = "10+ kW (${dashboardData.systemSizeDistribution.size10kwPlus})"
            
            // Update monthly performance
            dashboardContent.findViewById<TextView>(R.id.tv_quotes_this_month).text = "${dashboardData.monthlyPerformance.quotesThisMonth} quotes"
            dashboardContent.findViewById<TextView>(R.id.tv_quotes_last_month).text = "${dashboardData.monthlyPerformance.quotesLastMonth} quotes"
            dashboardContent.findViewById<TextView>(R.id.tv_growth_percentage).text = "${String.format("%.1f", dashboardData.monthlyPerformance.growthPercentage)}%"
            
            // Update top locations
            updateTopLocations(dashboardData.topLocations)
            
            android.util.Log.d("QuotesFragment", "Dashboard UI updated successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("QuotesFragment", "Error updating dashboard UI: ${e.message}", e)
            Toast.makeText(requireContext(), "Error updating dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    private fun updateTopLocations(topLocations: List<dev.solora.dashboard.LocationStats>) {
        val layout = dashboardContent.findViewById<LinearLayout>(R.id.layout_top_locations)
        layout.removeAllViews()
        
        if (topLocations.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No location data available"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(0, 16, 0, 16)
            }
            layout.addView(emptyText)
            return
        }
        
        topLocations.take(5).forEachIndexed { index, location ->
            val locationView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            // Rank number
            val rankText = TextView(requireContext()).apply {
                text = "${index + 1}."
                textSize = 16f
                setTextColor(resources.getColor(R.color.solora_orange, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
                minWidth = 32
            }
            
            val locationText = TextView(requireContext()).apply {
                text = location.location
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(16, 0, 0, 0)
            }
            
            val countText = TextView(requireContext()).apply {
                text = "${location.count} quotes"
                textSize = 14f
                setTextColor(resources.getColor(R.color.solora_orange, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            locationView.addView(rankText)
            locationView.addView(locationText)
            locationView.addView(countText)
            layout.addView(locationView)
            
            // Add divider except for last item
            if (index < topLocations.take(5).size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    setPadding(0, 8, 0, 8)
                }
                layout.addView(divider)
            }
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
                            email = email,
                            phone = contact,
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
                        Toast.makeText(requireContext(), "Calculation complete!", Toast.LENGTH_LONG).show()
                        
                        // Update the dashboard tab with calculation results
                        updateResultsTab(state.outputs)
                        
                        // Navigate to quote results fragment
                        navigateToQuoteResults(state.outputs, etAddress.text.toString().trim())
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
    private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit,
    private val onQuoteLongPress: (dev.solora.data.FirebaseQuote) -> Unit
) : androidx.recyclerview.widget.ListAdapter<dev.solora.data.FirebaseQuote, QuotesListAdapter.QuoteViewHolder>(QuoteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote_simple, parent, false)
        return QuoteViewHolder(view, onQuoteClick, onQuoteLongPress)
    }
    
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class QuoteViewHolder(
        itemView: View,
        private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit,
        private val onQuoteLongPress: (dev.solora.data.FirebaseQuote) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        
        private val tvReference: TextView = itemView.findViewById(R.id.tv_reference)
        private val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        
        fun bind(quote: dev.solora.data.FirebaseQuote) {
            // Set reference number (use quote ID if reference is empty)
            tvReference.text = if (quote.reference.isNotEmpty()) {
                quote.reference
            } else {
                "REF-${quote.id?.takeLast(5) ?: "00000"}"
            }
            
            // Set address
            tvAddress.text = quote.address
            
            // Format date
            val dateText = quote.createdAt?.toDate()?.let {
                java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(it)
            } ?: "Unknown"
            tvDate.text = dateText
            
            itemView.setOnClickListener {
                onQuoteClick(quote)
            }
            
            itemView.setOnLongClickListener {
                onQuoteLongPress(quote)
                true // Return true to consume the long press event
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