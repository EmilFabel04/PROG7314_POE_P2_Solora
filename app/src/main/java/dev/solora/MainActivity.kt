package dev.solora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.LayoutInflater
import androidx.compose.ui.res.painterResource
import dev.solora.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.solora.quote.QuoteCalculator
import dev.solora.quote.QuoteInputs
import dev.solora.quotes.QuotesScreenVM
import dev.solora.leads.LeadsScreenVM
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.solora.theme.SoloraTheme
import dev.solora.profile.ProfileScreenContent
import dev.solora.settings.SettingsScreenContent
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.auth.AuthViewModel
import dev.solora.auth.LoginScreen
import dev.solora.auth.RegisterScreen

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent { SoloraRoot() }
	}
}

@Composable
fun SoloraRoot() {
	val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    SoloraTheme {
        var selected by remember { mutableStateOf("home") }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("home", Icons.Filled.Home, "Home"),
                        Triple("quotes", Icons.Filled.RequestQuote, "Quotes"),
                        Triple("leads", Icons.Filled.FormatListBulleted, "Leads"),
                        Triple("profile", Icons.Filled.Person, "Profile")
                    )
                    items.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = selected == route,
                            onClick = { selected = route; navController.navigate(route) },
                            icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { inner ->
            Surface(modifier = Modifier.fillMaxSize().padding(inner)) {
                val start = if (authVm.isLoggedIn.value) "home" else "login"
                NavHost(navController = navController, startDestination = start) {
                    composable("login") {
                        LoginScreen(
                            onLogin = { e, p -> authVm.login(e, p); navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                            onCreateAccount = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegister = { n, e, p -> authVm.register(n, e, p); navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                            onBackToLogin = { navController.popBackStack() }
                        )
                    }
                    composable("home") { HomeScreen(onNavigate = { route -> navController.navigate(route) }) }
                    composable("quotes") { QuotesScreenVM() }
                    composable("leads") { LeadsScreenVM() }
                    composable("profile") { ProfileScreenContent() }
                    composable("settings") { SettingsScreenContent() }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Solora") }) }) { _ ->
        HomeContent(onNavigate)
	}
}

@Composable
fun HomeContent(onNavigate: (String) -> Unit) {
    ColumnWithButtons(onNavigate)
}

@Composable
fun ColumnWithButtons(onNavigate: (String) -> Unit) {
	Column(
		modifier = Modifier.fillMaxSize().padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Image(painter = painterResource(id = R.drawable.solora_logo), contentDescription = "Solora logo")
		Button(onClick = { onNavigate("quotes") }, modifier = Modifier.fillMaxWidth()) { Text("Calculate Quote") }
		Button(onClick = { onNavigate("leads") }, modifier = Modifier.fillMaxWidth()) { Text("Leads") }
		Button(onClick = { onNavigate("profile") }, modifier = Modifier.fillMaxWidth()) { Text("Profile") }
		Button(onClick = { onNavigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
	}
}

@Composable fun LeadsScreen() { CenterText("Leads") }
@Composable fun ProfileScreen() { CenterText("Profile") }
@Composable fun SettingsScreen() { CenterText("Settings") }

@Composable
fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium) }
}


