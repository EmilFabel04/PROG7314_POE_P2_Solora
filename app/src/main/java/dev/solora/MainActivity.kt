package dev.solora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.solora.auth.AuthViewModel
import dev.solora.auth.LoginScreen
import dev.solora.auth.OnboardingScreen
import dev.solora.auth.RegisterScreen
import dev.solora.home.HomeScreen
import dev.solora.leads.LeadsScreenVM
import dev.solora.profile.ChangePasswordScreen
import dev.solora.profile.EditProfileScreen
import dev.solora.profile.ProfileScreen
import dev.solora.profile.ProfileViewModel
import dev.solora.quotes.QuoteDetailScreen
import dev.solora.quotes.QuotesScreenVM
import dev.solora.settings.SettingsScreenContent
import dev.solora.theme.SoloraTheme

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
    val profileVm: ProfileViewModel = viewModel()
    val isLoggedIn by authVm.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomDestinations = listOf("home", "quotes", "leads", "profile")

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (currentRoute !in bottomDestinations) {
                navController.navigate("home") {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute !in listOf("onboarding", "login", "register")) {
                navController.navigate("onboarding") {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    SoloraTheme {
        Scaffold(
            bottomBar = {
                if (isLoggedIn && currentRoute in bottomDestinations) {
                    SoloraBottomBar(navController = navController, currentRoute = currentRoute ?: "home")
                }
            }
        ) { inner ->
            Surface(modifier = Modifier.fillMaxSize().padding(inner)) {
                NavGraph(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    authVm = authVm,
                    profileVm = profileVm
                )
            }
        }
    }
}

@Composable
private fun NavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    authVm: AuthViewModel,
    profileVm: ProfileViewModel
) {
    val profileState by profileVm.profile.collectAsState()

    NavHost(navController = navController, startDestination = if (isLoggedIn) "home" else "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                onLogin = { navController.navigate("login") },
                onCreateAccount = { navController.navigate("register") }
            )
        }
        composable("login") {
            LoginScreen(
                onLogin = { email, pass ->
                    authVm.login(email, pass)
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onCreateAccount = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegister = { name, email, pass ->
                    authVm.register(name, email, pass)
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(
                onOpenQuotes = { navController.navigate("quotes") },
                onOpenLeads = { navController.navigate("leads") },
                onOpenNotifications = { navController.navigate("notifications") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("quotes") {
            QuotesScreenVM(onQuoteSelected = { id -> navController.navigate("quote-detail/$id") })
        }
        composable("leads") { LeadsScreenVM() }
        composable("profile") {
            ProfileScreen(
                profile = profileState,
                onEditProfile = { navController.navigate("edit-profile") },
                onChangePassword = { navController.navigate("change-password") },
                onOpenSettings = { navController.navigate("settings") },
                onLogout = {
                    authVm.logout()
                    navController.navigate("onboarding") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }
        composable("settings") { SettingsScreenContent() }
        composable("notifications") { NotificationsScreen() }
        composable("edit-profile") {
            EditProfileScreen(
                initial = profileState,
                onSave = { profileVm.updateProfile(it) },
                onBack = { navController.popBackStack() }
            )
        }
        composable("change-password") {
            ChangePasswordScreen(
                onSubmit = { current, new -> profileVm.changePassword(current, new) },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            "quote-detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            QuoteDetailScreen(id = id, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SoloraBottomBar(navController: NavHostController, currentRoute: String) {
    val items = listOf(
        Triple("home", Icons.Filled.Home, "Home"),
        Triple("quotes", Icons.Filled.RequestQuote, "Quotes"),
        Triple("leads", Icons.Filled.FormatListBulleted, "Leads"),
        Triple("profile", Icons.Filled.Person, "Profile")
    )
    NavigationBar {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotificationCard(title = "New quote ready", message = "System sizing complete for K. Naidoo", time = "5 min ago")
            NotificationCard(title = "Lead follow-up", message = "Schedule visit for P. Jacobs", time = "Today, 15:00")
            NotificationCard(title = "Reminder", message = "Upload site photos for Mandela St.", time = "Yesterday")
            NotificationCard(title = "System check", message = "Battery health report available", time = "Yesterday")
        }
    }
}

@Composable
private fun NotificationCard(title: String, message: String, time: String) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(time, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
