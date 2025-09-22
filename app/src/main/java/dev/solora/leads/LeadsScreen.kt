package dev.solora.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun LeadsScreenVM() {
    val vm: LeadsViewModel = viewModel()
    LeadsScreenContent(
        leads = vm.leads.value,
        onAdd = { ref, name, addr, contact -> vm.addLead(ref, name, addr, contact) }
    )
}

@Composable
fun LeadsScreenContent(leads: List<dev.solora.data.Lead>, onAdd: (String, String, String, String) -> Unit) {
    val ref = remember { mutableStateOf("") }
    val name = remember { mutableStateOf("") }
    val addr = remember { mutableStateOf("") }
    val contact = remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Leads")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = ref.value, onValueChange = { ref.value = it }, label = { Text("Ref") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = name.value, onValueChange = { name.value = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = addr.value, onValueChange = { addr.value = it }, label = { Text("Address") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = contact.value, onValueChange = { contact.value = it }, label = { Text("Contact") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = { onAdd(ref.value, name.value, addr.value, contact.value) }, modifier = Modifier.fillMaxWidth()) { Text("Add Lead") }
        LazyColumn {
            items(leads) { lead ->
                Text("${lead.reference} • ${lead.name} • ${lead.address}", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}


