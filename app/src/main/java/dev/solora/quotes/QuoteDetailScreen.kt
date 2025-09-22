package dev.solora.quotes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.solora.pdf.PdfExporter

@Composable
fun QuoteDetailScreen(reference: String, panels: Int, systemKw: Double, inverterKw: Double, savings: Double) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quote $reference")
        Text("Panels: $panels")
        Text("System: ${systemKw} kW, Inverter: ${inverterKw} kW")
        Text("Savings: R ${"%.2f".format(savings)}")
        Button(onClick = {
            val file = PdfExporter.exportQuote(ctx, reference, panels, systemKw, inverterKw, savings)
            Toast.makeText(ctx, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }) { Text("Export to PDF") }
    }
}


