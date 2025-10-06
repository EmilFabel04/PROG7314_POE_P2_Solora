package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.leads.LeadsViewModel
import dev.solora.settings.SettingsViewModel
import dev.solora.data.FirebaseQuote
import dev.solora.api.FirebaseFunctionsApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.widget.Button
import java.util.Calendar

class HomeFragment : Fragment() {
    
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val apiService = FirebaseFunctionsApi()
    
    // UI Elements
    private lateinit var tvCompanyName: TextView
    private lateinit var tvConsultantName: TextView
    private lateinit var tvQuotesCount: TextView
    private lateinit var tvLeadsCount: TextView
    private lateinit var btnNotifications: ImageView
    private lateinit var btnSettings: ImageView
    private lateinit var cardCalculateQuote: MaterialCardView
    private lateinit var cardAddLeads: MaterialCardView
    private lateinit var layoutRecentQuotes: LinearLayout
    
    // Date Filter Elements
    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    
    // Date Filter Variables
    private var fromDate: Date? = null
    private var toDate: Date? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        observeData()
        loadRecentQuotes()
        performApiRefresh()
    }
    
    private fun initializeViews(view: View) {
        tvCompanyName = view.findViewById(R.id.tv_company_name)
        tvConsultantName = view.findViewById(R.id.tv_consultant_name)
        tvQuotesCount = view.findViewById(R.id.tv_quotes_count)
        tvLeadsCount = view.findViewById(R.id.tv_leads_count)
        btnNotifications = view.findViewById(R.id.btn_notifications)
        btnSettings = view.findViewById(R.id.btn_settings)
        cardCalculateQuote = view.findViewById(R.id.card_calculate_quote)
        cardAddLeads = view.findViewById(R.id.card_add_leads)
        layoutRecentQuotes = view.findViewById(R.id.layout_recent_quotes)
        
        // Date Filter Elements
        tvDateFrom = view.findViewById(R.id.tv_date_from)
        tvDateTo = view.findViewById(R.id.tv_date_to)
        btnClearFilter = view.findViewById(R.id.btn_clear_filter)
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter)
    }
    
    private fun setupClickListeners() {
        btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_to_notifications)
        }
        
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        
        cardCalculateQuote.setOnClickListener {
            // Navigate to quotes tab using bottom navigation selection
            try {
                val parentFragment = parentFragment
                if (parentFragment is MainTabsFragment) {
                    val bottomNav = parentFragment.view?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                    bottomNav?.selectedItemId = R.id.quotesFragment
                } else {
                    // Fallback to direct navigation if parent access fails
                    findNavController().navigate(R.id.quotesFragment)
                }
            } catch (e: Exception) {
                // Fallback to direct navigation
                findNavController().navigate(R.id.quotesFragment)
            }
        }
        
        cardAddLeads.setOnClickListener {
            // Navigate to leads tab using bottom navigation selection
            try {
                val parentFragment = parentFragment
                if (parentFragment is MainTabsFragment) {
                    val bottomNav = parentFragment.view?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                    bottomNav?.selectedItemId = R.id.leadsFragment
                } else {
                    // Fallback to direct navigation if parent access fails
                    findNavController().navigate(R.id.leadsFragment)
                }
            } catch (e: Exception) {
                // Fallback to direct navigation
                findNavController().navigate(R.id.leadsFragment)
            }
        }
        
        // Date Filter Click Listeners
        tvDateFrom.setOnClickListener {
            showDatePicker { date ->
                fromDate = date
                tvDateFrom.text = dateFormat.format(date)
            }
        }
        
        tvDateTo.setOnClickListener {
            showDatePicker { date ->
                toDate = date
                tvDateTo.text = dateFormat.format(date)
            }
        }
        
        btnClearFilter.setOnClickListener {
            clearDateFilter()
        }
        
        btnApplyFilter.setOnClickListener {
            applyDateFilter()
        }
    }
    
    private fun observeData() {
        // Observe quotes count
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quotes.collect { quotes ->
                tvQuotesCount.text = quotes.size.toString()
            }
        }
        
        // Observe leads count
        viewLifecycleOwner.lifecycleScope.launch {
            leadsViewModel.leads.collect { leads ->
                tvLeadsCount.text = leads.size.toString()
            }
        }
        
        // Observe settings to update company and consultant info
        viewLifecycleOwner.lifecycleScope.launch {
            settingsViewModel.settings.collect { settings ->
                updateCompanyInfo(settings.companySettings)
            }
        }
    }
    
    private fun updateCompanyInfo(companySettings: dev.solora.settings.CompanySettings) {
        // Update company name
        val companyName = if (companySettings.companyName.isNotEmpty()) {
            companySettings.companyName
        } else {
            "SOLORA"
        }
        tvCompanyName.text = companyName
        
        // Update consultant name
        val consultantName = if (companySettings.consultantName.isNotEmpty()) {
            "Consultant: ${companySettings.consultantName}"
        } else {
            "Consultant: Not Set"
        }
        tvConsultantName.text = consultantName
    }
    
    private fun loadRecentQuotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get recent quotes from the quotes flow
                quotesViewModel.quotes.collect { quotes ->
                    val filteredQuotes = filterQuotesByDate(quotes)
                    val recentQuotes = filteredQuotes.take(5) // Get the 5 most recent quotes
                    displayRecentQuotes(recentQuotes)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error loading recent quotes: ${e.message}", e)
                displayEmptyQuotes()
            }
        }
    }
    
    private fun filterQuotesByDate(quotes: List<FirebaseQuote>): List<FirebaseQuote> {
        if (fromDate == null && toDate == null) {
            return quotes // No filter applied
        }
        
        return quotes.filter { quote ->
            try {
                // Convert Firebase Timestamp to Date
                val quoteDate = quote.createdAt?.toDate()
                if (quoteDate == null) {
                    return@filter true // Include quote if no date available
                }
                
                val isAfterFromDate = fromDate?.let { 
                    // Set time to start of day for comparison
                    val calendar = Calendar.getInstance()
                    calendar.time = it
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    quoteDate.after(calendar.time) || quoteDate == calendar.time
                } ?: true
                
                val isBeforeToDate = toDate?.let { 
                    // Set time to end of day for comparison
                    val calendar = Calendar.getInstance()
                    calendar.time = it
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    quoteDate.before(calendar.time) || quoteDate == calendar.time
                } ?: true
                
                isAfterFromDate && isBeforeToDate
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error filtering quote by date: ${e.message}", e)
                true // Include quote if date filtering fails
            }
        }
    }
    
    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun clearDateFilter() {
        fromDate = null
        toDate = null
        tvDateFrom.text = "Select Date"
        tvDateTo.text = "Select Date"
        applyDateFilter()
    }
    
    private fun applyDateFilter() {
        // Refresh the quotes display with the current filter
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                quotesViewModel.quotes.value.let { quotes ->
                    val filteredQuotes = filterQuotesByDate(quotes)
                    val recentQuotes = filteredQuotes.take(5)
                    displayRecentQuotes(recentQuotes)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error applying date filter: ${e.message}", e)
            }
        }
    }
    
    private fun displayRecentQuotes(quotes: List<FirebaseQuote>) {
        layoutRecentQuotes.removeAllViews()
        
        if (quotes.isEmpty()) {
            displayEmptyQuotes()
            return
        }
        
        quotes.forEach { quote ->
            val quoteView = createQuoteItemView(quote)
            layoutRecentQuotes.addView(quoteView)
            
            // Add divider except for last item
            if (quote != quotes.last()) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    setPadding(0, 8, 0, 8)
                }
                layoutRecentQuotes.addView(divider)
            }
        }
    }
    
    private fun createQuoteItemView(quote: FirebaseQuote): View {
        val quoteView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        // Quote icon
        val iconView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(36, 36)
            setImageResource(android.R.drawable.ic_menu_report_image)
            setColorFilter(resources.getColor(R.color.solora_orange, null))
        }
        
        // Quote details
        val detailsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setPadding(12, 0, 0, 0)
        }
        
        // Reference and address
        val referenceText = TextView(requireContext()).apply {
            val reference = if (quote.reference.isNotEmpty()) quote.reference else "REF-${quote.id?.takeLast(5) ?: "00000"}"
            text = reference
            textSize = 13f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val addressText = TextView(requireContext()).apply {
            text = quote.address.ifEmpty { "Address not available" }
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#666666"))
        }
        
        // Date
        val dateText = TextView(requireContext()).apply {
            val dateString = quote.createdAt?.toDate()?.let {
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(it)
            } ?: "Unknown"
            text = dateString
            textSize = 11f
            setTextColor(resources.getColor(R.color.solora_orange, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        detailsLayout.addView(referenceText)
        detailsLayout.addView(addressText)
        
        quoteView.addView(iconView)
        quoteView.addView(detailsLayout)
        quoteView.addView(dateText)
        
        // Click listener for quote item
        quoteView.setOnClickListener {
            if (!quote.id.isNullOrBlank()) {
                val bundle = Bundle().apply { putString("id", quote.id) }
                findNavController().navigate(R.id.quoteDetailFragment, bundle)
            }
        }
        
        return quoteView
    }
    
    private fun displayEmptyQuotes() {
        layoutRecentQuotes.removeAllViews()
        
        val emptyView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        
        val emptyText = TextView(requireContext()).apply {
            text = "No recent quotes"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
        }
        
        val subText = TextView(requireContext()).apply {
            text = "Create your first quote to get started"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#999999"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        
        emptyView.addView(emptyText)
        emptyView.addView(subText)
        layoutRecentQuotes.addView(emptyView)
    }
    
    private fun performApiRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Refresh quotes count via API
                val quotesResult = apiService.getQuotes(search = null, limit = 100) // Set a reasonable limit
                if (quotesResult.isSuccess) {
                    val quotes = quotesResult.getOrNull() ?: emptyList()
                    tvQuotesCount.text = quotes.size.toString()
                    android.util.Log.d("HomeFragment", "Refreshed quotes count via API: ${quotes.size}")
                }
                
                // Refresh leads count via API
                val leadsResult = apiService.getLeads(search = null, status = null, limit = 100) // Set a reasonable limit
                if (leadsResult.isSuccess) {
                    val leads = leadsResult.getOrNull() ?: emptyList()
                    tvLeadsCount.text = leads.size.toString()
                    android.util.Log.d("HomeFragment", "Refreshed leads count via API: ${leads.size}")
                }
                
                // Refresh settings via API to ensure company info is up to date
                val settingsResult = apiService.getSettings()
                if (settingsResult.isSuccess) {
                    val settingsData = settingsResult.getOrNull()
                    if (settingsData != null) {
                        val companyName = settingsData["companyName"] as? String ?: "SOLORA"
                        val consultantName = settingsData["consultantName"] as? String ?: "Not Set"
                        
                        tvCompanyName.text = companyName
                        tvConsultantName.text = "Consultant: $consultantName"
                        android.util.Log.d("HomeFragment", "Refreshed company info via API: $companyName, $consultantName")
                    }
                }
                
                // Sync any pending data
                val syncData = mapOf<String, Any>(
                    "timestamp" to System.currentTimeMillis(),
                    "source" to "home_refresh"
                )
                val syncResult = apiService.syncData(syncData)
                if (syncResult.isSuccess) {
                    android.util.Log.d("HomeFragment", "Data sync completed via API")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error during API refresh: ${e.message}", e)
            }
        }
    }
}