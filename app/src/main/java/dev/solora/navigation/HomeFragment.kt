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
    
    override fun onResume() {
        super.onResume()
        // Reload recent quotes when fragment becomes visible
        loadRecentQuotes()
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
                // Get quotes from the last 7 days using REST API
                val quotesResult = apiService.getQuotes(search = null, limit = 100)
                if (quotesResult.isSuccess) {
                    val allQuotes = quotesResult.getOrNull() ?: emptyList()
                    
                    // Filter quotes from the last 7 days
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    
                    val recentQuotes = allQuotes.filter { quote ->
                        quote.createdAt?.toDate()?.let { createdAt ->
                            createdAt >= sevenDaysAgo
                        } ?: false
                    }.sortedByDescending { it.createdAt?.toDate() }
                    .take(5) // Get the 5 most recent quotes from last 7 days
                    
                    android.util.Log.d("HomeFragment", "Found ${recentQuotes.size} quotes from last 7 days out of ${allQuotes.size} total quotes")
                    displayRecentQuotes(recentQuotes)
                } else {
                    android.util.Log.e("HomeFragment", "Failed to load quotes via API: ${quotesResult.exceptionOrNull()?.message}")
                    displayEmptyQuotes()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error loading recent quotes: ${e.message}", e)
                displayEmptyQuotes()
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
        }
    }
    
    private fun createQuoteItemView(quote: FirebaseQuote): View {
        // Create MaterialCardView similar to view quotes page
        val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            radius = 6f
            elevation = 1f
            setCardBackgroundColor(android.graphics.Color.WHITE)
            isClickable = true
            isFocusable = true
            foreground = context.getDrawable(android.R.attr.selectableItemBackground)
        }
        
        // Main content layout
        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
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
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
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
        
        contentLayout.addView(iconView)
        contentLayout.addView(detailsLayout)
        contentLayout.addView(dateText)
        
        cardView.addView(contentLayout)
        
        // Click listener for quote item
        cardView.setOnClickListener {
            if (!quote.id.isNullOrBlank()) {
                val bundle = Bundle().apply { putString("id", quote.id) }
                findNavController().navigate(R.id.quoteDetailFragment, bundle)
            }
        }
        
        return cardView
    }
    
    private fun displayEmptyQuotes() {
        layoutRecentQuotes.removeAllViews()
        
        val emptyView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        
        val emptyText = TextView(requireContext()).apply {
            text = "No quotes from last 7 days"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
        }
        
        val subText = TextView(requireContext()).apply {
            text = "Create a new quote to see it here"
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