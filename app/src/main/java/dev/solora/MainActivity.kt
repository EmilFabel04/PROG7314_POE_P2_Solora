package dev.solora

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.solora.auth.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        val bottomBar: BottomNavigationView = findViewById(R.id.bottom_nav)

        bottomBar.setupWithNavController(navController)
        configureBottomBarVisibility(navController, bottomBar)
        observeAuthentication(navController)
    }

    private fun configureBottomBarVisibility(navController: NavController, bottomBar: BottomNavigationView) {
        val bottomDestinations = setOf(
            R.id.homeFragment,
            R.id.quotesFragment,
            R.id.leadsFragment,
            R.id.profileFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomBar.isVisible = destination.id in bottomDestinations
        }
    }

    private fun observeAuthentication(navController: NavController) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoggedIn.collect { loggedIn ->
                    val currentDestination = navController.currentDestination?.id
                    if (loggedIn) {
                        if (currentDestination !in HOME_DESTINATIONS) {
                            navController.navigate(R.id.homeFragment) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    } else {
                        if (currentDestination !in AUTH_DESTINATIONS) {
                            navController.navigate(R.id.onboardingFragment) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val AUTH_DESTINATIONS = setOf(
            R.id.onboardingFragment,
            R.id.loginFragment,
            R.id.registerFragment
        )

        private val HOME_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.quotesFragment,
            R.id.leadsFragment,
            R.id.profileFragment,
            R.id.settingsFragment,
            R.id.notificationsFragment,
            R.id.editProfileFragment,
            R.id.changePasswordFragment,
            R.id.quoteDetailFragment
        )
    }
}
