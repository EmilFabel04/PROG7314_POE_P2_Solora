package dev.solora.quotes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.data.Quote
import dev.solora.pdf.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(id: Long, onBack: () -> Unit) {
    val vm: QuotesViewModel = viewModel()
    val quote by vm.quoteById(id).collectAsState()
    QuoteDetailScaffold(quote = quote, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    reference: String,
    panels: Int,
    systemKw: Double,
    inverterKw: Double,
    savings: Double,
    onBack: () -> Unit = {}
) {
    val fallbackQuote = remember(reference, panels, systemKw, inverterKw, savings) {
        Quote(
            reference = reference,
            clientName = "",
            address = "",
            monthlyUsageKwh = null,
            monthlyBillRands = null,
            tariff = 0.0,
            panelWatt = 0,
            sunHours = 0.0,
            panels = panels,
            systemKw = systemKw,
            inverterKw = inverterKw,
            savingsRands = savings
        )
    }
    QuoteDetailScaffold(quote = fallbackQuote, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDetailScaffold(quote: Quote?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (quote == null) {
                Text(
                    text = "Loading quote…",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                QuoteHeaderCard(quote)
                QuoteSystemCard(quote)
                QuoteInputsCard(quote)

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val file = PdfExporter.exportQuote(
                            ctx,
                            quote.reference,
                            quote.panels,
                            quote.systemKw,
                            quote.inverterKw,
                            quote.savingsRands
                        )
                        Toast.makeText(ctx, "Exported to ${'$'}{file.absolutePath}", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export to PDF")
                }
            }
        }
    }
}

@Composable
private fun QuoteHeaderCard(quote: Quote) {
    if (quote.reference.isBlank() && quote.clientName.isBlank() && quote.address.isBlank()) {
        return
    }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (quote.reference.isNotBlank()) {
                Text(
                    quote.reference,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (quote.clientName.isNotBlank()) {
                Text(
                    quote.clientName,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (quote.address.isNotBlank()) {
                Text(
                    quote.address,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuoteSystemCard(quote: Quote) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("System overview", fontWeight = FontWeight.SemiBold)
            DetailRow(label = "System size", value = "${'$'}{quote.systemKw} kW")
            DetailRow(label = "Panels", value = quote.panels.toString())
            DetailRow(label = "Inverter", value = "${'$'}{quote.inverterKw} kW")
            DetailRow(
                label = "Estimated savings",
                value = "R ${'$'}{"%.2f".format(quote.savingsRands)} per month"
            )
        }
    }
}

@Composable
private fun QuoteInputsCard(quote: Quote) {
    val rows = mutableListOf<Pair<String, String>>()
    quote.monthlyUsageKwh?.let { rows += "Monthly usage" to "${'$'}{"%.0f".format(it)} kWh" }
    quote.monthlyBillRands?.let { rows += "Monthly bill" to "R ${'$'}{"%.2f".format(it)}" }
    if (quote.tariff > 0) {
        rows += "Tariff" to "R ${'$'}{"%.2f".format(quote.tariff)} per kWh"
    }
    if (quote.panelWatt > 0) {
        rows += "Panel size" to "${'$'}{quote.panelWatt} W panels"
    }
    if (quote.sunHours > 0) {
        rows += "Sun hours used" to "${'$'}{"%.1f".format(quote.sunHours)} h/day"
    }

    if (rows.isEmpty()) {
        return
    }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Inputs", fontWeight = FontWeight.SemiBold)
            rows.forEach { (label, value) -> DetailRow(label = label, value = value) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
