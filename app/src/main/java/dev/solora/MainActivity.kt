package dev.solora

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import dev.solora.auth.AuthViewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // getting the preferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val onboardingSeen = prefs.getBoolean("onboarding_seen", false)

        // getting the nav controller
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // inflating the root graph and casting it to the nav graph
        val rootGraph = navController.navInflater.inflate(R.navigation.nav_root) as NavGraph

        // getting the login state
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

        // choosing which nested graph to start at
        rootGraph.setStartDestination(
            when {
                !onboardingSeen -> R.id.auth_graph
                isLoggedIn -> R.id.main_graph
                else -> R.id.auth_graph
            }
        )

        // applying the root graph
        navController.graph = rootGraph

        // if onboarding is already seen but not logged in, skip to login in the auth graph
        if (onboardingSeen && !isLoggedIn) {
            navController.navigate(R.id.loginFragment)
        }

        // observing the login state reactively - keeps user logged in
        lifecycleScope.launchWhenStarted {
            authViewModel.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    navController.navigate(
                        R.id.main_graph,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.auth_graph, true)
                            .build()
                    )
                }
            }
        }
    }
}
