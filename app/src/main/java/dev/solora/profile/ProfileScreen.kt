package dev.solora.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreenContent() {
    val name = remember { mutableStateOf("") }
    val title = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profile")
        OutlinedTextField(name.value, { name.value = it }, label = { Text("Name") })
        OutlinedTextField(title.value, { title.value = it }, label = { Text("Job Title") })
        OutlinedTextField(email.value, { email.value = it }, label = { Text("Email") })
        Button(onClick = { /* save stub */ }) { Text("Save") }
    }
}


