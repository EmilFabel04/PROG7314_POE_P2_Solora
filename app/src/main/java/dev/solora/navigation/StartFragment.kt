package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.auth.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartFragment : Fragment() {

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
            // Read DIRECTLY from DataStore to avoid ViewModel's cached default value
            val authRepository = AuthRepository(requireContext().applicationContext)
            val hasSeenOnboarding = authRepository.hasSeenOnboarding.first()
            
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
