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
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.leads.LeadsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuoteDetailFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    
    private lateinit var tvReference: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvClientInfo: TextView
    private lateinit var tvEnergyDetails: TextView
    private lateinit var tvSystemDesign: TextView
    private lateinit var tvFinancialAnalysis: TextView
    private lateinit var tvEnvironmentalImpact: TextView
    private lateinit var btnConvertToLead: Button
    private lateinit var btnExportPdf: Button
    private lateinit var btnShare: Button
    
    private var currentQuote: dev.solora.data.Quote? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        
        val quoteId = requireArguments().getLong("id", 0L)
        android.util.Log.d("QuoteDetailFragment", "Looking for quote with ID: $quoteId")
        
        observeQuote(quoteId)
    }
    
    private fun initializeViews(view: View) {
        tvReference = view.findViewById(R.id.tv_reference)
        tvDate = view.findViewById(R.id.tv_date)
        tvClientInfo = view.findViewById(R.id.tv_client_info)
        tvEnergyDetails = view.findViewById(R.id.tv_energy_details)
        tvSystemDesign = view.findViewById(R.id.tv_system_design)
        tvFinancialAnalysis = view.findViewById(R.id.tv_financial_analysis)
        tvEnvironmentalImpact = view.findViewById(R.id.tv_environmental_impact)
        btnConvertToLead = view.findViewById(R.id.btn_convert_to_lead)
        btnExportPdf = view.findViewById(R.id.btn_export_pdf)
        btnShare = view.findViewById(R.id.btn_share)
    }
    
    private fun setupClickListeners() {
        btnConvertToLead.setOnClickListener {
            convertToLead()
        }
        
        btnExportPdf.setOnClickListener {
            exportToPdf()
        }
        
        btnShare.setOnClickListener {
            shareQuote()
        }
    }
    
    private fun observeQuote(quoteId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quoteById(quoteId).collect { quote ->
                android.util.Log.d("QuoteDetailFragment", "Received quote: ${quote?.id} - ${quote?.reference}")
                currentQuote = quote
                
                if (quote != null) {
                    populateQuoteDetails(quote)
                } else {
                    showQuoteNotFound()
                }
            }
        }
    }
    
    private fun populateQuoteDetails(quote: dev.solora.data.Quote) {
        // Header
        tvReference.text = "Quote ${quote.reference}"
        tvDate.text = "Generated on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quote.dateEpoch))}"
        
        // Client Information
        tvClientInfo.text = buildString {
            appendLine("Client: ${quote.clientName}")
            appendLine("Address: ${quote.address}")
            if (quote.latitude != null && quote.longitude != null) {
                appendLine("Location: ${String.format("%.4f", quote.latitude)}, ${String.format("%.4f", quote.longitude)}")
            }
        }
        
        // Energy Details
        tvEnergyDetails.text = buildString {
            quote.monthlyUsageKwh?.let { 
                appendLine("Monthly Usage: ${String.format("%.1f", it)} kWh") 
            }
            quote.monthlyBillRands?.let { 
                appendLine("Average Bill: R${String.format("%.2f", it)}") 
            }
            appendLine("Tariff Rate: R${String.format("%.2f", quote.tariff)}/kWh")
        }
        
        // System Design
        tvSystemDesign.text = buildString {
            appendLine("Panel Rating: ${quote.panelWatt}W each")
            appendLine("Number of Panels: ${quote.panels}")
            appendLine("System Size: ${String.format("%.2f", quote.systemKw)} kW")
            appendLine("Inverter Size: ${String.format("%.2f", quote.inverterKw)} kW")
            appendLine("Sun Hours: ${String.format("%.1f", quote.sunHours)} hours/day")
            
            // NASA API data if available
            if (quote.averageAnnualSunHours != null) {
                appendLine("Average Annual Sun Hours: ${String.format("%.1f", quote.averageAnnualSunHours)} hours/day")
            }
            quote.optimalMonth?.let { month ->
                val monthName = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
                appendLine("Optimal Month: $monthName")
            }
        }
        
        // Financial Analysis
        tvFinancialAnalysis.text = buildString {
            appendLine("Monthly Savings: R${String.format("%.2f", quote.savingsRands)}")
            
            val annualSavings = quote.annualSavingsRands ?: (quote.savingsRands * 12)
            appendLine("Annual Savings: R${String.format("%.2f", annualSavings)}")
            
            quote.systemCostRands?.let { 
                appendLine("System Cost: R${String.format("%.2f", it)}")
            }
            quote.paybackYears?.let { 
                appendLine("Payback Period: ${String.format("%.1f", it)} years")
            }
        }
        
        // Environmental Impact
        quote.co2SavingsKgPerYear?.let { co2Savings ->
            tvEnvironmentalImpact.text = buildString {
                appendLine("CO₂ Savings: ${String.format("%.0f", co2Savings)} kg/year")
                appendLine("Equivalent to: ${String.format("%.1f", co2Savings/1000)} tons CO₂/year")
                appendLine("Trees Equivalent: ${String.format("%.0f", co2Savings/22)} trees planted")
            }
            view?.findViewById<View>(R.id.card_environmental)?.visibility = View.VISIBLE
        } ?: run {
            view?.findViewById<View>(R.id.card_environmental)?.visibility = View.GONE
        }
    }
    
    private fun showQuoteNotFound() {
        tvReference.text = "Quote Not Found"
        tvClientInfo.text = "The requested quote could not be loaded. It may have been deleted or doesn't exist."
        tvEnergyDetails.text = ""
        tvSystemDesign.text = ""
        tvFinancialAnalysis.text = ""
        tvEnvironmentalImpact.text = ""
        
        btnConvertToLead.isEnabled = false
        btnShare.isEnabled = false
    }
    
    private fun convertToLead() {
        currentQuote?.let { quote ->
            android.util.Log.d("QuoteDetailFragment", "Converting quote ${quote.reference} to lead")
            
            // Create lead from quote using LeadsViewModel
            leadsViewModel.createLeadFromQuote(
                quote = quote,
                contactInfo = "", // Will be filled in later by the consultant
                notes = "Lead converted from quote ${quote.reference}. System: ${String.format("%.2f", quote.systemKw)}kW, Monthly savings: R${String.format("%.2f", quote.savingsRands)}"
            )
            
            Toast.makeText(
                requireContext(), 
                "Quote successfully converted to lead! Check the Leads tab.", 
                Toast.LENGTH_LONG
            ).show()
            
            // Navigate to leads tab
            findNavController().navigate(R.id.leadsFragment)
            
        } ?: run {
            Toast.makeText(requireContext(), "No quote available to convert", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportToPdf() {
        Toast.makeText(requireContext(), "PDF export functionality coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun shareQuote() {
        currentQuote?.let { quote ->
            val shareText = buildString {
                appendLine("📊 SOLAR QUOTE - ${quote.reference}")
                appendLine("═══════════════════════════════════")
                appendLine()
                appendLine("👤 CLIENT: ${quote.clientName}")
                appendLine("📍 LOCATION: ${quote.address}")
                appendLine()
                appendLine("🌞 SOLAR SYSTEM:")
                appendLine("• System Size: ${String.format("%.2f", quote.systemKw)} kW")
                appendLine("• Panels: ${quote.panels} x ${quote.panelWatt}W")
                appendLine("• Inverter: ${String.format("%.2f", quote.inverterKw)} kW")
                appendLine()
                appendLine("💰 FINANCIAL BENEFITS:")
                appendLine("• Monthly Savings: R${String.format("%.2f", quote.savingsRands)}")
                appendLine("• Annual Savings: R${String.format("%.2f", quote.savingsRands * 12)}")
                quote.paybackYears?.let { 
                    appendLine("• Payback Period: ${String.format("%.1f", it)} years")
                }
                appendLine()
                quote.co2SavingsKgPerYear?.let { co2 ->
                    appendLine("🌱 ENVIRONMENTAL IMPACT:")
                    appendLine("• CO₂ Reduction: ${String.format("%.0f", co2)} kg/year")
                    appendLine()
                }
                appendLine("Generated by Solora Solar Solutions")
                appendLine("Professional Solar Installations")
            }
            
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Solar Quote - ${quote.reference}")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Quote"))
        } ?: run {
            Toast.makeText(requireContext(), "Quote not available for sharing", Toast.LENGTH_SHORT).show()
        }
    }
}