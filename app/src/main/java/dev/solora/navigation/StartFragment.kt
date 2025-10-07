package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.auth.AuthViewModel
import dev.solora.R

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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Wait for both flows to emit their values
            val isLoggedIn = authViewModel.isLoggedIn.value
            val hasAppData = authViewModel.hasAppData.value
            
            when {
                // Case 1: User is logged in -> Go to main app
                isLoggedIn -> {
                    findNavController().navigate(R.id.action_start_to_main)
                }
                // Case 2 & 3: Not logged in -> Go to auth with flag indicating first-time
                else -> {
                    val bundle = bundleOf("show_onboarding" to !hasAppData)
                    findNavController().navigate(R.id.action_start_to_auth, bundle)
                }
            }
        }
    }
}


