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
        return View(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.isLoggedIn.collectLatest { loggedIn ->
                if (loggedIn) {
                    findNavController().navigate(R.id.action_start_to_main)
                } else {
                    findNavController().navigate(R.id.action_start_to_auth)
                }
            }
        }
    }
}


