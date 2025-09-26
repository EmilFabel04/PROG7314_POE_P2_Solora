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
        tvReference.text = quote.reference
        tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quote.dateEpoch))
        
        // Client Information
        tvClientInfo.text = buildString {
            appendLine("To: ${quote.clientName}")
            appendLine("${quote.address}")
            appendLine()
            appendLine("From: Solora")
            appendLine("+27 (0)82 123 4567")
            appendLine("info@solora.co.za")
        }
        
        // Energy Details - Current Energy Information
        tvEnergyDetails.text = buildString {
            appendLine("CURRENT ENERGY INFORMATION")
            appendLine()
            quote.monthlyUsageKwh?.let { 
                appendLine("Monthly Usage                    ${String.format("%.0f", it)} kWh") 
            }
            quote.monthlyBillRands?.let { 
                appendLine("Average Bill                     R ${String.format("%.2f", it)}") 
            }
            appendLine("Tariff Rate                      R ${String.format("%.2f", quote.tariff)}/kWh")
        }
        
        // System Design - Solar System Specifications
        tvSystemDesign.text = buildString {
            appendLine("RECOMMENDED SYSTEM")
            appendLine()
            appendLine("Panel                            ${quote.panelWatt}W")
            appendLine("Quantity                         ${quote.panels}")
            appendLine("Recommended Inverter             ${String.format("%.0f", quote.inverterKw)}kW")
            appendLine("Total System Size                ${String.format("%.2f", quote.systemKw)}kW")
            appendLine("Percentage of monthly usage      ${String.format("%.0f", (quote.systemKw * quote.sunHours * 30) / (quote.monthlyUsageKwh ?: 1000) * 100)}%")
        }
        
        // Financial Analysis - Quotation breakdown like in Figma
        tvFinancialAnalysis.text = buildString {
            appendLine("QUOTATION")
            appendLine()
            
            val systemCost = quote.systemCostRands ?: (quote.systemKw * 15000)
            val vatAmount = systemCost * 0.15
            val totalCost = systemCost + vatAmount
            
            appendLine("Solar System                     R ${String.format("%.2f", systemCost)}")
            appendLine("Installation                     R 0.00")
            appendLine("VAT                              R ${String.format("%.2f", vatAmount)}")
            appendLine()
            appendLine("Subtotal                         R ${String.format("%.2f", systemCost)}")
            appendLine("Tax                              R ${String.format("%.2f", vatAmount)}")
            appendLine("Total Due                        R ${String.format("%.2f", totalCost)}")
            appendLine()
            appendLine("ESTIMATED MONTHLY SAVINGS")
            appendLine("R ${String.format("%.2f", quote.savingsRands)}")
            appendLine()
            quote.paybackYears?.let { 
                appendLine("PAYBACK PERIOD")
                appendLine("${String.format("%.1f", it)} years")
            }
        }
        
        // Hide environmental impact to match clean design
        view?.findViewById<View>(R.id.card_environmental)?.visibility = View.GONE
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
            val systemCost = quote.systemCostRands ?: (quote.systemKw * 15000)
            val vatAmount = systemCost * 0.15
            val totalCost = systemCost + vatAmount
            
            val shareText = buildString {
                appendLine("SOLAR QUOTE - ${quote.reference}")
                appendLine("═══════════════════════════════════")
                appendLine()
                appendLine("CLIENT: ${quote.clientName}")
                appendLine("LOCATION: ${quote.address}")
                appendLine()
                appendLine("RECOMMENDED SYSTEM:")
                appendLine("Panel Rating: ${quote.panelWatt}W")
                appendLine("Quantity: ${quote.panels}")
                appendLine("System Size: ${String.format("%.2f", quote.systemKw)} kW")
                appendLine("Inverter: ${String.format("%.2f", quote.inverterKw)} kW")
                appendLine()
                appendLine("QUOTATION:")
                appendLine("Solar System: R ${String.format("%.2f", systemCost)}")
                appendLine("Installation: R 0.00")
                appendLine("VAT: R ${String.format("%.2f", vatAmount)}")
                appendLine("Total Due: R ${String.format("%.2f", totalCost)}")
                appendLine()
                appendLine("ESTIMATED MONTHLY SAVINGS:")
                appendLine("R ${String.format("%.2f", quote.savingsRands)}")
                quote.paybackYears?.let { 
                    appendLine("PAYBACK PERIOD: ${String.format("%.1f", it)} years")
                }
                appendLine()
                appendLine("Solora Solar Solutions")
                appendLine("+27 (0)82 123 4567")
                appendLine("info@solora.co.za")
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