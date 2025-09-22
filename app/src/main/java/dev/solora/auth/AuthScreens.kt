package dev.solora.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.solora.R

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onCreateAccount: () -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painter = painterResource(id = R.drawable.solora_logo), contentDescription = null)
        OutlinedTextField(email.value, { email.value = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onLogin(email.value, password.value) }, modifier = Modifier.fillMaxWidth()) { Text("Login") }
        Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String, String) -> Unit, onBackToLogin: () -> Unit) {
    val name = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painter = painterResource(id = R.drawable.solora_logo), contentDescription = null)
        OutlinedTextField(name.value, { name.value = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email.value, { email.value = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onRegister(name.value, email.value, password.value) }, modifier = Modifier.fillMaxWidth()) { Text("Create Account") }
        Button(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) { Text("Back to Login") }
    }
}


