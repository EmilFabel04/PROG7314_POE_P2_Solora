package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.auth.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Wait for the DataStore to load and get the first value
            val hasSeenOnboarding = authViewModel.hasSeenOnboarding.first()
            
            if (!hasSeenOnboarding) {
                // First time -> Show onboarding
                findNavController().navigate(R.id.action_start_to_onboarding)
            } else {
                // Already seen onboarding -> Show login
                findNavController().navigate(R.id.action_start_to_login)
            }
        }
    }
}
