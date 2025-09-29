package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import dev.solora.R

class StartFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Show a simple loading view while checking auth state
        return inflater.inflate(android.R.layout.activity_list_item, container, false).apply {
            findViewById<android.widget.TextView>(android.R.id.text1)?.apply {
                text = "Loading..."
                gravity = android.view.Gravity.CENTER
                textSize = 18f
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add a small delay to prevent immediate navigation on slow devices
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            kotlinx.coroutines.delay(500) // Give UI time to render
            
            authViewModel.isLoggedIn.collectLatest { loggedIn ->
                android.util.Log.d("StartFragment", "Auth state: loggedIn = $loggedIn")
                
                if (loggedIn) {
                    android.util.Log.d("StartFragment", "User logged in, navigating to main")
                    findNavController().navigate(R.id.action_start_to_main)
                } else {
                    android.util.Log.d("StartFragment", "User not logged in, navigating to auth flow")
                    findNavController().navigate(R.id.action_start_to_auth)
                }
            }
        }
    }
}


