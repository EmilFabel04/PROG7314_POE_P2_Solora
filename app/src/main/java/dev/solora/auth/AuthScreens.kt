package dev.solora.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.solora.R

@Composable
fun OnboardingScreen(onLogin: () -> Unit, onCreateAccount: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(painter = painterResource(id = R.drawable.solora_logo), contentDescription = null)
                Text(
                    "Solora Mobile",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Capture leads, calculate accurate quotes and stay on top of follow-ups wherever you are.",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Login") }
                Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onCreateAccount: () -> Unit) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    AuthFormScaffold(
        title = "Welcome back",
        subtitle = "Login to access your dashboard",
        primaryActionLabel = "Login",
        secondaryActionLabel = "Create account",
        onSecondaryAction = onCreateAccount,
        onPrimaryAction = { onLogin(email.value, password.value) }
    ) {
        OutlinedTextField(email.value, { email.value = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String, String) -> Unit, onBackToLogin: () -> Unit) {
    val name = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    AuthFormScaffold(
        title = "Create account",
        subtitle = "Set up your Solora consultant profile",
        primaryActionLabel = "Create account",
        secondaryActionLabel = "Back to login",
        onSecondaryAction = onBackToLogin,
        onPrimaryAction = { onRegister(name.value, email.value, password.value) }
    ) {
        OutlinedTextField(name.value, { name.value = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email.value, { email.value = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password.value, { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AuthFormScaffold(
    title: String,
    subtitle: String,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    onPrimaryAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.solora_logo), contentDescription = null)
            Spacer(Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    content()
                    Button(onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth()) { Text(primaryActionLabel) }
                    TextButton(onClick = onSecondaryAction, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}
