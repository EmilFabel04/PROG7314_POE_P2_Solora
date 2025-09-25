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
import kotlinx.coroutines.launch

class QuoteDetailFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvReference = view.findViewById<TextView>(R.id.tv_reference)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)
        val btnExportPdf = view.findViewById<Button>(R.id.btn_export_pdf)
        val btnShare = view.findViewById<Button>(R.id.btn_share)
        
        val quoteId = requireArguments().getLong("id", 0L)
        
        // Observe the specific quote
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quoteById(quoteId).collect { quote ->
                if (quote != null) {
                    tvReference.text = "Quote ${quote.reference}"
                    
                    val detailedSummary = buildString {
                        appendLine("📋 CLIENT INFORMATION")
                        appendLine("Client: ${quote.clientName}")
                        appendLine("Address: ${quote.address}")
                        
                        // Location data if available
                        if (quote.latitude != null && quote.longitude != null) {
                            appendLine("Location: ${String.format("%.4f", quote.latitude)}, ${String.format("%.4f", quote.longitude)}")
                        }
                        appendLine()
                        
                        appendLine("⚡ ENERGY DETAILS")
                        quote.monthlyUsageKwh?.let { appendLine("Monthly Usage: ${String.format("%.1f", it)} kWh") }
                        quote.monthlyBillRands?.let { appendLine("Average Bill: R${String.format("%.2f", it)}") }
                        appendLine("Tariff Rate: R${String.format("%.2f", quote.tariff)}/kWh")
                        appendLine()
                        
                        appendLine("🌞 SOLAR SYSTEM DESIGN")
                        appendLine("Panel Rating: ${quote.panelWatt}W each")
                        appendLine("Number of Panels: ${quote.panels}")
                        appendLine("System Size: ${String.format("%.2f", quote.systemKw)} kW")
                        appendLine("Inverter Size: ${String.format("%.2f", quote.inverterKw)} kW")
                        appendLine("Sun Hours: ${String.format("%.1f", quote.sunHours)} hours/day")
                        appendLine()
                        
                        // NASA API solar data if available
                        if (quote.averageAnnualIrradiance != null || quote.averageAnnualSunHours != null) {
                            appendLine("🛰️ NASA SOLAR DATA")
                            quote.averageAnnualIrradiance?.let { 
                                appendLine("Annual Solar Irradiance: ${String.format("%.2f", it)} kWh/m²/day") 
                            }
                            quote.averageAnnualSunHours?.let { 
                                appendLine("Average Sun Hours: ${String.format("%.1f", it)} hours/day") 
                            }
                            quote.optimalMonth?.let { month ->
                                val monthName = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                                      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
                                appendLine("Optimal Month: $monthName")
                                quote.optimalMonthIrradiance?.let { 
                                    appendLine("Peak Irradiance: ${String.format("%.2f", it)} kWh/m²/day") 
                                }
                            }
                            appendLine()
                        }
                        
                        // Environmental data if available
                        if (quote.temperature != null || quote.windSpeed != null || quote.humidity != null) {
                            appendLine("🌡️ ENVIRONMENTAL CONDITIONS")
                            quote.temperature?.let { appendLine("Temperature: ${String.format("%.1f", it)}°C") }
                            quote.windSpeed?.let { appendLine("Wind Speed: ${String.format("%.1f", it)} m/s") }
                            quote.humidity?.let { appendLine("Humidity: ${String.format("%.1f", it)}%") }
                            appendLine()
                        }
                        
                        appendLine("💰 FINANCIAL ANALYSIS")
                        appendLine("Monthly Savings: R${String.format("%.2f", quote.savingsRands)}")
                        quote.annualSavingsRands?.let { 
                            appendLine("Annual Savings: R${String.format("%.2f", it)}") 
                        } ?: run {
                            appendLine("Annual Savings: R${String.format("%.2f", quote.savingsRands * 12)}")
                        }
                        quote.systemCostRands?.let { 
                            appendLine("System Cost: R${String.format("%.2f", it)}") 
                        }
                        quote.paybackYears?.let { 
                            appendLine("Payback Period: ${String.format("%.1f", it)} years") 
                        }
                        appendLine()
                        
                        // Environmental impact if available
                        quote.co2SavingsKgPerYear?.let {
                            appendLine("🌱 ENVIRONMENTAL IMPACT")
                            appendLine("CO₂ Savings: ${String.format("%.0f", it)} kg/year")
                            appendLine("Equivalent to: ${String.format("%.1f", it/1000)} tons CO₂/year")
                            appendLine()
                        }
                        
                        appendLine("📅 Quote Generated: ${java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(quote.dateEpoch))}")
                    }
                    
                    tvSummary.text = detailedSummary
                } else {
                    tvReference.text = "Quote Not Found"
                    tvSummary.text = "The requested quote could not be loaded. It may have been deleted or doesn't exist."
                }
            }
        }
        
        // Handle export button (for now just shows success message)
        btnExportPdf.setOnClickListener {
            Toast.makeText(requireContext(), "PDF export functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Handle share button
        btnShare.setOnClickListener {
            quotesViewModel.quoteById(quoteId).value?.let { quote ->
                val shareText = "Solar Quote for ${quote.clientName}\n\n" +
                        "System Size: ${String.format("%.2f", quote.systemKw)} kW\n" +
                        "Panels: ${quote.panels} x ${quote.panelWatt}W\n" +
                        "Monthly Savings: R${String.format("%.2f", quote.savingsRands)}\n" +
                        "Annual Savings: R${String.format("%.2f", quote.savingsRands * 12)}\n\n" +
                        "Generated by Solora Solar Solutions"
                
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
}


