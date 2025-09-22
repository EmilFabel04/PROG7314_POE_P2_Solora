package dev.solora.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.ui.QuoteResultCard

@Composable
fun QuotesScreenVM() {
    val vm: QuotesViewModel = viewModel()
    QuotesScreenContent(vm)
}

@Composable
fun QuotesScreenContent(vm: QuotesViewModel) {
    val ref = remember { mutableStateOf("") }
    val address = remember { mutableStateOf("") }
    val usage = remember { mutableStateOf("") }
    val tariff = remember { mutableStateOf("2.5") }
    val panelWatt = remember { mutableStateOf("400") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Calculate Quote")
        OutlinedTextField(ref.value, { ref.value = it }, label = { Text("Reference") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address.value, { address.value = it }, label = { Text("Address or coords") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(usage.value, { usage.value = it }, label = { Text("Monthly usage kWh (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(tariff.value, { tariff.value = it }, label = { Text("Tariff R/kWh") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(panelWatt.value, { panelWatt.value = it }, label = { Text("Panel size W") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.calculateAndSave(ref.value, address.value, usage.value.toDoubleOrNull(), tariff.value.toDoubleOrNull() ?: 2.5, panelWatt.value.toIntOrNull() ?: 400) }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate & Save")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.quotes.value) { q ->
                QuoteResultCard(q.panels, q.systemKw, q.inverterKw, q.savingsRands)
            }
        }
    }
}


