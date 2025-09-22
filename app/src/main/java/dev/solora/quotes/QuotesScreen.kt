package dev.solora.quotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.data.Quote

@Composable
fun QuotesScreenVM(onQuoteSelected: (Long) -> Unit) {
    val vm: QuotesViewModel = viewModel()
    val quotes by vm.quotes.collectAsState()
    val lastQuote by vm.lastQuote.collectAsState()
    QuotesScreenContent(
        quotes = quotes,
        lastQuote = lastQuote,
        onCalculate = { ref, name, address, usage, bill, tariff, panel ->
            vm.calculateAndSaveUsingAddress(ref, name, address, usage, bill, tariff, panel)
        },
        onQuoteSelected = onQuoteSelected
    )
}

@Composable
fun QuotesScreenContent(
    quotes: List<Quote>,
    lastQuote: Quote?,
    onCalculate: (
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int
    ) -> Unit,
    onQuoteSelected: (Long) -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Quote Info", "Saved Quotes", "Dashboard")

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Quotes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            TabRow(selectedTabIndex = tabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                }
            }
            Spacer(Modifier.height(16.dp))
            when (tabIndex) {
                0 -> QuoteFormTab(onCalculate = onCalculate, lastQuote = lastQuote)
                1 -> SavedQuotesTab(quotes = quotes, onQuoteSelected = onQuoteSelected)
                2 -> QuoteDashboardTab(quotes = quotes)
            }
        }
    }
}

@Composable
private fun QuoteFormTab(
    onCalculate: (
        reference: String,
        clientName: String,
        address: String,
        usageKwh: Double?,
        billRands: Double?,
        tariff: Double,
        panelWatt: Int
    ) -> Unit,
    lastQuote: Quote?
) {
    var reference by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var usage by remember { mutableStateOf("") }
    var bill by remember { mutableStateOf("") }
    var tariff by remember { mutableStateOf("2.50") }
    var panel by remember { mutableStateOf("420") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quote Information", fontWeight = FontWeight.SemiBold)
                QuoteTextField(value = reference, label = "Quote reference") { reference = it }
                QuoteTextField(value = clientName, label = "Client name") { clientName = it }
                QuoteTextField(value = address, label = "Site address or coordinates") { address = it }
                QuoteTextField(value = usage, label = "Monthly usage (kWh)") { usage = it }
                QuoteTextField(value = bill, label = "Average bill (R)") { bill = it }
                QuoteTextField(value = tariff, label = "Tariff (R/kWh)") { tariff = it }
                QuoteTextField(value = panel, label = "Panel size (W)") { panel = it }
                Button(
                    onClick = {
                        onCalculate(
                            reference,
                            clientName,
                            address,
                            usage.toDoubleOrNull(),
                            bill.toDoubleOrNull(),
                            tariff.toDoubleOrNull() ?: 2.5,
                            panel.toIntOrNull() ?: 420
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate & Save")
                }
            }
        }
        if (lastQuote != null) {
            QuoteSummaryCard(lastQuote)
        }
    }
}

@Composable
private fun SavedQuotesTab(quotes: List<Quote>, onQuoteSelected: (Long) -> Unit) {
    if (quotes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quotes captured yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(quotes) { quote ->
            QuoteListCard(quote = quote, onView = { onQuoteSelected(quote.id) })
        }
    }
}

@Composable
private fun QuoteDashboardTab(quotes: List<Quote>) {
    val totalQuotes = quotes.size
    val totalSavings = quotes.sumOf { it.savingsRands }
    val averageSystemSize = if (totalQuotes > 0) quotes.sumOf { it.systemKw } / totalQuotes else 0.0
    val averagePanels = if (totalQuotes > 0) quotes.sumOf { it.panels } / totalQuotes.toDouble() else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardTile(title = "Quotes", value = totalQuotes.toString(), accent = MaterialTheme.colorScheme.primary)
            DashboardTile(title = "Avg System", value = "${"%.1f".format(averageSystemSize)} kW", accent = Color(0xFFFFA726))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardTile(title = "Avg Panels", value = "${"%.1f".format(averagePanels)}", accent = Color(0xFF29B6F6))
            DashboardTile(title = "Monthly Savings", value = "R ${"%.0f".format(totalSavings)}", accent = Color(0xFF66BB6A))
        }
        if (quotes.isNotEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recent Quotes", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    quotes.take(3).forEach { quote ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(quote.reference, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "${quote.clientName} • ${quote.systemKw} kW system",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteSummaryCard(quote: Quote) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Latest calculation", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            SummaryRow(label = "Panels", value = quote.panels.toString())
            SummaryRow(label = "System Size", value = "${quote.systemKw} kW")
            SummaryRow(label = "Inverter", value = "${quote.inverterKw} kW")
            SummaryRow(label = "Savings", value = "R ${"%.2f".format(quote.savingsRands)}")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuoteListCard(quote: Quote, onView: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(quote.reference, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(quote.clientName, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = onView) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "View quote")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricChip(title = "System", value = "${quote.systemKw} kW")
                MetricChip(title = "Panels", value = quote.panels.toString())
                MetricChip(title = "Savings", value = "R ${"%.0f".format(quote.savingsRands)}")
            }
        }
    }
}

@Composable
private fun MetricChip(title: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.DashboardTile(title: String, value: String, accent: Color) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun QuoteTextField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}
