package dev.solora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.auth.AuthViewModel
import dev.solora.auth.LoginScreen
import dev.solora.auth.OnboardingScreen
import dev.solora.auth.RegisterScreen
import dev.solora.theme.SoloraTheme

class OnboardingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme {
                    val navController = findNavController()
                    OnboardingScreen(
                        onLogin = { navController.navigate(R.id.loginFragment) },
                        onCreateAccount = { navController.navigate(R.id.registerFragment) }
                    )
                }
            }
        }
    }
}

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme {
                    val navController = findNavController()
                    LoginScreen(
                        onLogin = { email, pass -> authViewModel.login(email, pass) },
                        onCreateAccount = { navController.navigate(R.id.registerFragment) }
                    )
                }
            }
        }
    }
}

class RegisterFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme {
                    val navController = findNavController()
                    RegisterScreen(
                        onRegister = { name, email, pass -> authViewModel.register(name, email, pass) },
                        onBackToLogin = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
