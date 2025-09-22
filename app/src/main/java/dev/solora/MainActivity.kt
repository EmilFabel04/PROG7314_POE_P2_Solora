package dev.solora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent { SoloraApp() }
	}
}

@Composable
fun SoloraApp() {
	val navController = rememberNavController()
	MaterialTheme {
		Surface(modifier = Modifier.fillMaxSize()) {
			NavHost(navController = navController, startDestination = "home") {
				composable("home") { HomeScreen(onNavigate = { route -> navController.navigate(route) }) }
				composable("quotes") { QuotesScreen() }
				composable("leads") { LeadsScreen() }
				composable("profile") { ProfileScreen() }
				composable("settings") { SettingsScreen() }
			}
		}
	}
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
	Scaffold(topBar = { SmallTopAppBar(title = { Text("Solora") }) }) { _ ->
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
		Button(onClick = { onNavigate("quotes") }, modifier = Modifier.fillMaxWidth()) { Text("Calculate Quote") }
		Button(onClick = { onNavigate("leads") }, modifier = Modifier.fillMaxWidth()) { Text("Leads") }
		Button(onClick = { onNavigate("profile") }, modifier = Modifier.fillMaxWidth()) { Text("Profile") }
		Button(onClick = { onNavigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
	}
}

@Composable fun QuotesScreen() { CenterText("Quotes") }
@Composable fun LeadsScreen() { CenterText("Leads") }
@Composable fun ProfileScreen() { CenterText("Profile") }
@Composable fun SettingsScreen() { CenterText("Settings") }

@Composable
fun CenterText(text: String) {
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
	}
}


