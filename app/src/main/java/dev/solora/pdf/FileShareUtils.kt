package dev.solora.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileShareUtils {
    
    fun sharePdfFile(context: Context, pdfFile: File, quoteReference: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Solora Quote - $quoteReference")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the quote for your solar system.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Quote PDF"))
            android.util.Log.d("FileShareUtils", "PDF shared successfully: ${pdfFile.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("FileShareUtils", "Error sharing PDF: ${e.message}", e)
        }
    }
    
    fun openPdfFile(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            android.util.Log.d("FileShareUtils", "PDF opened successfully: ${pdfFile.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("FileShareUtils", "Error opening PDF: ${e.message}", e)
        }
    }
}
