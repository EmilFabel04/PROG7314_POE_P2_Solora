package dev.solora.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.data.Lead

@Composable
fun LeadsScreenVM() {
    val vm: LeadsViewModel = viewModel()
    val leads by vm.leads.collectAsState()
    LeadsScreenContent(
        leads = leads,
        onAdd = { ref, name, addr, contact -> vm.addLead(ref, name, addr, contact) }
    )
}

@Composable
fun LeadsScreenContent(leads: List<Lead>, onAdd: (String, String, String, String) -> Unit) {
    var ref by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Leads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            SummaryBanner(totalLeads = leads.size)
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add new lead", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Reference number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact details") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        onAdd(ref, name, addr, contact)
                        ref = ""
                        name = ""
                        addr = ""
                        contact = ""
                    }, modifier = Modifier.fillMaxWidth(), enabled = ref.isNotBlank() && name.isNotBlank()) {
                        Text("Save lead")
                    }
                }
            }

            if (leads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No leads captured yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                Text("Active leads", fontWeight = FontWeight.SemiBold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(leads) { lead ->
                        LeadCard(lead = lead)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBanner(totalLeads: Int) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Total leads", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(totalLeads.toString(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            }
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun LeadCard(lead: Lead) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(lead.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(lead.reference, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Text(lead.address)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(lead.contact, fontWeight = FontWeight.Medium)
            }
        }
    }
}
