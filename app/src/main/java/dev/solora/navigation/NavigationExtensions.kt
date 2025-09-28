package dev.solora.navigation

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.auth.AuthState
import dev.solora.auth.AuthViewModel

fun Fragment.observeAuthStateAndNavigate(authViewModel: AuthViewModel) {
    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        authViewModel.authState.collect { state ->
            when (state) {
                is AuthState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()

                    findNavController().navigate(
                        R.id.main_graph,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.auth_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )

                    authViewModel.clearAuthState()
                }

                is AuthState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    authViewModel.clearAuthState()
                }

                else -> Unit
            }
        }
    }
}