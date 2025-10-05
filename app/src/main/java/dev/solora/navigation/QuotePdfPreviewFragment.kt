package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.os.Bundle
import dev.solora.R
import dev.solora.data.FirebaseQuote
import dev.solora.pdf.FileShareUtils
import dev.solora.pdf.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class QuotePdfPreviewFragment : Fragment() {
    
    private var quoteId: String = ""
    private lateinit var btnBack: ImageButton
    private lateinit var btnSharePdf: Button
    private lateinit var tvReference: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvMonthlySavings: TextView
    private lateinit var tvPanelCount: TextView
    private lateinit var tvSystemSize: TextView
    private lateinit var tvInverterSize: TextView
    private lateinit var tvCoverage: TextView
    private lateinit var tvSystemCost: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvTotalCost: TextView
    
    private var currentQuote: FirebaseQuote? = null
    private val pdfGenerator by lazy { PdfGenerator(requireContext()) }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote_pdf_preview, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        loadQuoteData()
    }
    
    private fun initializeViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back_pdf_preview)
        btnSharePdf = view.findViewById(R.id.btn_share_pdf)
        tvReference = view.findViewById(R.id.tv_reference)
        tvClientName = view.findViewById(R.id.tv_client_name)
        tvAddress = view.findViewById(R.id.tv_address)
        tvDate = view.findViewById(R.id.tv_date)
        tvMonthlySavings = view.findViewById(R.id.tv_monthly_savings)
        tvPanelCount = view.findViewById(R.id.tv_panel_count)
        tvSystemSize = view.findViewById(R.id.tv_system_size)
        tvInverterSize = view.findViewById(R.id.tv_inverter_size)
        tvCoverage = view.findViewById(R.id.tv_coverage)
        tvSystemCost = view.findViewById(R.id.tv_system_cost)
        tvTax = view.findViewById(R.id.tv_tax)
        tvTotalCost = view.findViewById(R.id.tv_total_cost)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnSharePdf.setOnClickListener {
            exportToPdf()
        }
    }
    
    private fun loadQuoteData() {
        // Get quote data from arguments
        quoteId = arguments?.getString("quoteId") ?: ""
        if (quoteId.isNotEmpty()) {
            // Load quote from ViewModel or Repository
            // For now, we'll use a placeholder - in real implementation, load from database
            displayQuoteData(createPlaceholderQuote(quoteId))
        }
    }
    
    private fun createPlaceholderQuote(quoteId: String): FirebaseQuote {
        // This is a placeholder - in real implementation, load from database
        return FirebaseQuote(
            id = quoteId,
            reference = "QUOTE-${quoteId.takeLast(5)}",
            clientName = "Sample Client",
            address = "123 Sample Street, Cape Town",
            systemKwp = 6.6,
            monthlySavings = 1170.0,
            createdAt = com.google.firebase.Timestamp.now()
        )
    }
    
    private fun displayQuoteData(quote: FirebaseQuote) {
        currentQuote = quote
        
        val reference = if (quote.reference.isNotEmpty()) quote.reference else "REF-${quote.id?.takeLast(5) ?: "00000"}"
        val dateText = quote.createdAt?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: "Unknown"
        
        val monthlySavings = if (quote.monthlySavings > 0) {
            "R ${String.format("%.2f", quote.monthlySavings)}"
        } else {
            "R 1,170.00"
        }
        
        val systemSize = quote.systemKwp
        val panelCount = if (systemSize > 0) {
            (systemSize * 1000 / 550).toInt()
        } else {
            12
        }
        
        val systemSizeText = if (systemSize > 0) "${String.format("%.1f", systemSize)}KW" else "6.6KW"
        val inverterSizeText = if (systemSize > 0) "${String.format("%.0f", systemSize)}KW" else "6KW"
        
        val coverage = if (systemSize > 0) {
            ((systemSize * 150) / 1000 * 100).toInt()
        } else {
            95
        }
        
        val systemCost = if (systemSize > 0) {
            systemSize * 8500
        } else {
            56700.0
        }
        
        val tax = systemCost * 0.15
        val totalCost = systemCost + tax
        
        // Display the data
        tvReference.text = reference
        tvClientName.text = quote.clientName.ifEmpty { "Temporary Client" }
        tvAddress.text = quote.address.ifEmpty { "Address not available" }
        tvDate.text = dateText
        tvMonthlySavings.text = monthlySavings
        tvPanelCount.text = "$panelCount x 550W"
        tvSystemSize.text = systemSizeText
        tvInverterSize.text = inverterSizeText
        tvCoverage.text = "${coverage}%"
        tvSystemCost.text = "R ${String.format("%.2f", systemCost)}"
        tvTax.text = "R ${String.format("%.2f", tax)}"
        tvTotalCost.text = "R ${String.format("%.2f", totalCost)}"
    }
    
    private fun exportToPdf() {
        currentQuote?.let { quote ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Show loading message
                    android.widget.Toast.makeText(requireContext(), "Generating PDF...", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // Generate PDF in background thread
                    val pdfFile = withContext(Dispatchers.IO) {
                        pdfGenerator.generateQuotePdf(quote)
                    }
                    
                    if (pdfFile != null) {
                        // Show success message
                        android.widget.Toast.makeText(requireContext(), "PDF generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Share the PDF file
                        val reference = if (quote.reference.isNotEmpty()) quote.reference else "REF-${quote.id?.takeLast(5) ?: "00000"}"
                        FileShareUtils.sharePdfFile(requireContext(), pdfFile, reference)
                        
                        android.util.Log.d("QuotePdfPreviewFragment", "PDF exported successfully: ${pdfFile.name}")
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
                        android.util.Log.e("QuotePdfPreviewFragment", "PDF generation failed for quote: ${quote.reference}")
                    }
                    
                } catch (e: Exception) {
                    android.widget.Toast.makeText(requireContext(), "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    android.util.Log.e("QuotePdfPreviewFragment", "PDF export error: ${e.message}", e)
                }
            }
        } ?: run {
            android.widget.Toast.makeText(requireContext(), "No quote data available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
