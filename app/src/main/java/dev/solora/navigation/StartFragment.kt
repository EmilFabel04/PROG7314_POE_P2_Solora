package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import dev.solora.R

class StartFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    private var isCheckingAuth = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        val btnGetStarted = view.findViewById<Button>(R.id.btn_get_started)
        val tvSkip = view.findViewById<TextView>(R.id.tv_skip)
        
        // Set up click listeners
        btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_start_to_auth)
        }
        
        tvSkip.setOnClickListener {
            findNavController().navigate(R.id.action_start_to_auth)
        }
        
        // Check authentication status
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.isLoggedIn.collectLatest { loggedIn ->
                if (isCheckingAuth) {
                    // Only auto-redirect on initial check, not after user actions
                    if (loggedIn) {
                        findNavController().navigate(R.id.action_start_to_main)
                    }
                    // Show the welcome screen for new users
                    isCheckingAuth = false
                }
            }
        }
    }
}


