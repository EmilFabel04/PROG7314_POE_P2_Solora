package dev.solora.pdf

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File

object PdfExporter {
    fun exportQuote(context: Context, reference: String, panels: Int, systemKw: Double, inverterKw: Double, savingsRands: Double): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, "Solora_Quote_${reference}.pdf")
        val writer = PdfWriter(file)
        val pdfDoc = PdfDocument(writer)
        val doc = Document(pdfDoc, PageSize.A4)
        doc.add(Paragraph("Solora Quote").setBold().setFontSize(20f).setFontColor(ColorConstants.ORANGE))
        doc.add(Paragraph("Reference: $reference"))
        doc.add(Paragraph("Panels: $panels"))
        doc.add(Paragraph("System size: ${systemKw} kW"))
        doc.add(Paragraph("Recommended inverter: ${inverterKw} kW"))
        doc.add(Paragraph("Estimated monthly savings: R ${"%.2f".format(savingsRands)}"))
        doc.close()
        return file
    }
}


