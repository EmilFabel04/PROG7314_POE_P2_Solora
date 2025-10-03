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
import com.google.android.material.card.MaterialCardView
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.leads.LeadsViewModel
import dev.solora.settings.SettingsViewModel
import dev.solora.data.FirebaseQuote
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    
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
            Toast.makeText(requireContext(), "Push notifications coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        
        cardCalculateQuote.setOnClickListener {
            // Navigate to quotes tab and switch to calculate tab
            findNavController().navigate(R.id.quotesFragment)
        }
        
        cardAddLeads.setOnClickListener {
            // Navigate to leads fragment
            findNavController().navigate(R.id.leadsFragment)
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
                // Get recent quotes (last 5) from the quotes flow
                quotesViewModel.quotes.collect { quotes ->
                    val recentQuotes = quotes.take(5) // Get the 5 most recent quotes
                    displayRecentQuotes(recentQuotes)
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
            setPadding(0, 12, 0, 12)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        // Quote icon
        val iconView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setImageResource(android.R.drawable.ic_menu_report_image)
            setColorFilter(resources.getColor(R.color.solora_orange, null))
        }
        
        // Quote details
        val detailsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setPadding(16, 0, 0, 0)
        }
        
        // Reference and address
        val referenceText = TextView(requireContext()).apply {
            val reference = if (quote.reference.isNotEmpty()) quote.reference else "REF-${quote.id?.takeLast(5) ?: "00000"}"
            text = reference
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val addressText = TextView(requireContext()).apply {
            text = quote.address.ifEmpty { "Address not available" }
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#666666"))
        }
        
        // Date
        val dateText = TextView(requireContext()).apply {
            val dateString = quote.createdAt?.toDate()?.let {
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(it)
            } ?: "Unknown"
            text = dateString
            textSize = 12f
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
}