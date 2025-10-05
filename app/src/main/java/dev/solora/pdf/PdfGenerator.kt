package dev.solora.pdf

import android.content.Context
import android.os.Environment
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import dev.solora.data.FirebaseQuote
import dev.solora.settings.CompanySettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {
    
    fun generateQuotePdf(quote: FirebaseQuote): File? {
        return generateQuotePdf(quote, CompanySettings())
    }
    
    fun generateQuotePdf(quote: FirebaseQuote, companySettings: CompanySettings): File? {
        return try {
            // Create PDF directory if it doesn't exist
            val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Quotes")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val reference = if (quote.reference.isNotEmpty()) quote.reference else "REF-${quote.id?.takeLast(5) ?: "00000"}"
            val filename = "Solora_Quote_${reference}_$timestamp.pdf"
            val pdfFile = File(pdfDir, filename)
            
            // Generate HTML content
            val htmlContent = generateQuoteHtml(quote, companySettings)
            
            // Convert HTML to PDF
            val outputStream = FileOutputStream(pdfFile)
            HtmlConverter.convertToPdf(htmlContent, outputStream)
            outputStream.close()
            
            android.util.Log.d("PdfGenerator", "PDF generated successfully: ${pdfFile.absolutePath}")
            pdfFile
            
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error generating PDF: ${e.message}", e)
            null
        }
    }
    
    private fun generateQuoteHtml(quote: FirebaseQuote, companySettings: CompanySettings = CompanySettings()): String {
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
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Solora Quote - $reference</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background-color: white;
            padding: 40px;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
            border-bottom: 2px solid #FF6B35;
            padding-bottom: 20px;
        }
        .company-info h1 {
            color: #FF6B35;
            font-size: 32px;
            margin: 0;
            font-weight: bold;
        }
        .company-info p {
            color: #333;
            margin: 5px 0 0 0;
            font-size: 14px;
        }
        .quote-ref {
            color: #FF6B35;
            font-size: 16px;
            font-weight: bold;
            text-align: right;
        }
        .section {
            margin-bottom: 25px;
        }
        .section-title {
            color: #333;
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 15px;
            border-bottom: 1px solid #eee;
            padding-bottom: 5px;
        }
        .detail-row {
            display: flex;
            margin-bottom: 8px;
        }
        .detail-label {
            font-weight: bold;
            min-width: 180px;
            color: #333;
        }
        .detail-value {
            color: #333;
        }
        .highlight {
            color: #FF6B35;
            font-weight: bold;
        }
        .cost-section {
            margin-top: 20px;
        }
        .cost-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            padding: 5px 0;
        }
        .total-row {
            background-color: #f5f5f5;
            padding: 15px;
            margin-top: 10px;
            border-radius: 5px;
            font-weight: bold;
            font-size: 16px;
        }
        .total-label {
            color: #333;
        }
        .total-value {
            color: #FF6B35;
        }
        .footer {
            margin-top: 40px;
            text-align: center;
            color: #666;
            font-size: 12px;
            border-top: 1px solid #eee;
            padding-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="company-info">
                <h1>${if (companySettings.companyName.isNotEmpty()) companySettings.companyName else "SOLORA"}</h1>
                <p>${if (companySettings.companyAddress.isNotEmpty()) companySettings.companyAddress else "Solar Solutions"}</p>
                ${if (companySettings.companyPhone.isNotEmpty() || companySettings.companyEmail.isNotEmpty() || companySettings.companyWebsite.isNotEmpty()) {
                    val contactInfo = mutableListOf<String>()
                    if (companySettings.companyPhone.isNotEmpty()) contactInfo.add("Tel: ${companySettings.companyPhone}")
                    if (companySettings.companyEmail.isNotEmpty()) contactInfo.add("Email: ${companySettings.companyEmail}")
                    if (companySettings.companyWebsite.isNotEmpty()) contactInfo.add("Web: ${companySettings.companyWebsite}")
                    "<p style=\"font-size: 12px; color: #666; margin-top: 5px;\">${contactInfo.joinToString(" • ")}</p>"
                } else ""}
            </div>
            <div class="quote-ref">
                Ref no. $reference
            </div>
        </div>
        
        <div class="section">
            <div class="section-title">Client Information</div>
            <div class="detail-row">
                <div class="detail-label">Name:</div>
                <div class="detail-value">${quote.clientName.ifEmpty { "Temporary Client" }}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Address:</div>
                <div class="detail-value">${quote.address.ifEmpty { "Address not available" }}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Date:</div>
                <div class="detail-value">$dateText</div>
            </div>
        </div>
        
        <div class="section">
            <div class="section-title">System Specifications</div>
            <div class="detail-row">
                <div class="detail-label">Estimated Monthly Savings:</div>
                <div class="detail-value highlight">$monthlySavings</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Recommended Panels:</div>
                <div class="detail-value">$panelCount x 550W</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Total System Size:</div>
                <div class="detail-value">$systemSizeText</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Recommended Inverter:</div>
                <div class="detail-value">$inverterSizeText</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Coverage of monthly usage:</div>
                <div class="detail-value">${coverage}%</div>
            </div>
        </div>
        
        <div class="section cost-section">
            <div class="section-title">Cost Breakdown</div>
            <div class="cost-row">
                <div class="detail-label">System Cost:</div>
                <div class="detail-value">R ${String.format("%.2f", systemCost)}</div>
            </div>
            <div class="cost-row">
                <div class="detail-label">Tax:</div>
                <div class="detail-value">R ${String.format("%.2f", tax)}</div>
            </div>
            <div class="total-row">
                <div class="total-label">Total:</div>
                <div class="total-value">R ${String.format("%.2f", totalCost)}</div>
            </div>
        </div>
        
        <div class="footer">
            <p>This quote is valid for 30 days from the date of issue.</p>
            <p>For any questions, please contact us at ${if (companySettings.companyEmail.isNotEmpty()) companySettings.companyEmail else "info@solora.co.za"}</p>
            ${if (companySettings.quoteFooter.isNotEmpty()) "<p>${companySettings.quoteFooter}</p>" else ""}
            <p>Generated on ${SimpleDateFormat("dd MMMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date())}</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }
}
