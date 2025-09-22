package dev.solora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuoteCalculatorScreen(onCalculate: (String, Double?, Double, Int) -> Unit) {
    val address = remember { mutableStateOf("") }
    val usageKwh = remember { mutableStateOf("") }
    val tariff = remember { mutableStateOf("") }
    val panelWatt = remember { mutableStateOf("400") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Calculate Quote", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = address.value, onValueChange = { address.value = it }, label = { Text("Address or coords") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = usageKwh.value, onValueChange = { usageKwh.value = it }, label = { Text("Monthly usage (kWh) or empty if using bill") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tariff.value, onValueChange = { tariff.value = it }, label = { Text("Tariff (R/kWh)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = panelWatt.value, onValueChange = { panelWatt.value = it }, label = { Text("Panel size (W)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val usage = usageKwh.value.toDoubleOrNull()
            val t = tariff.value.toDoubleOrNull() ?: 2.5
            val w = panelWatt.value.toIntOrNull() ?: 400
            onCalculate(address.value, usage, t, w)
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteResultCard(panels: Int, systemKw: Double, inverterKw: Double, savingsRands: Double) {
    Card(colors = CardDefaults.cardColors(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Results", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row { Text("Panels: ", fontWeight = FontWeight.Medium); Text(panels.toString()) }
            Row { Text("System size: ", fontWeight = FontWeight.Medium); Text("${systemKw} kW") }
            Row { Text("Recommended inverter: ", fontWeight = FontWeight.Medium); Text("${inverterKw} kW") }
            Row { Text("Estimated monthly savings: ", fontWeight = FontWeight.Medium); Text("R ${"%.2f".format(savingsRands)}") }
        }
    }
}

@Composable
fun Center(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}


