package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import dev.solora.R
import dev.solora.auth.AuthViewModel
import dev.solora.auth.LoginScreen
import dev.solora.auth.OnboardingScreen
import dev.solora.auth.RegisterScreen

class OnboardingFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OnboardingScreen(
                    onLogin = { findNavController().navigate(R.id.action_onboarding_to_login) },
                    onCreateAccount = { findNavController().navigate(R.id.action_onboarding_to_register) }
                )
            }
        }
    }
}

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LoginScreen(
                    onLogin = { email, pass ->
                        authViewModel.login(email, pass)
                        findNavController().navigate(R.id.action_login_to_main)
                    },
                    onCreateAccount = { findNavController().navigate(R.id.action_login_to_register) }
                )
            }
        }
    }
}

class RegisterFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RegisterScreen(
                    onRegister = { name, email, pass ->
                        authViewModel.register(name, email, pass)
                        findNavController().navigate(R.id.action_register_to_main)
                    },
                    onBackToLogin = { findNavController().popBackStack() }
                )
            }
        }
    }
}


