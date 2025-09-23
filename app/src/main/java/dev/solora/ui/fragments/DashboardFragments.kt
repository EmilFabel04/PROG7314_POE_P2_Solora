package dev.solora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.home.HomeScreen
import dev.solora.leads.LeadsScreenContent
import dev.solora.leads.LeadsViewModel
import dev.solora.notifications.NotificationsScreen
import dev.solora.profile.ChangePasswordScreen
import dev.solora.profile.EditProfileScreen
import dev.solora.profile.ProfileScreen
import dev.solora.profile.ProfileViewModel
import dev.solora.quotes.QuotesScreenContent
import dev.solora.quotes.QuotesViewModel
import dev.solora.settings.SettingsScreenContent
import dev.solora.theme.SoloraTheme

class HomeFragment : Fragment() {
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
                    HomeScreen(
                        onOpenQuotes = { navController.navigate(R.id.quotesFragment) },
                        onOpenLeads = { navController.navigate(R.id.leadsFragment) },
                        onOpenNotifications = { navController.navigate(R.id.notificationsFragment) },
                        onOpenSettings = { navController.navigate(R.id.settingsFragment) }
                    )
                }
            }
        }
    }
}

class QuotesFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by activityViewModels {
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
                    val quotes by quotesViewModel.quotes.collectAsState()
                    val lastQuote by quotesViewModel.lastQuote.collectAsState()
                    QuotesScreenContent(
                        quotes = quotes,
                        lastQuote = lastQuote,
                        onCalculate = { ref, name, address, usage, bill, tariff, panel ->
                            quotesViewModel.calculateAndSaveUsingAddress(ref, name, address, usage, bill, tariff, panel)
                        },
                        onQuoteSelected = { id ->
                            navController.navigate(
                                R.id.quoteDetailFragment,
                                bundleOf("id" to id)
                            )
                        }
                    )
                }
            }
        }
    }
}

class LeadsFragment : Fragment() {
    private val leadsViewModel: LeadsViewModel by activityViewModels {
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
                    val leads by leadsViewModel.leads.collectAsState()
                    LeadsScreenContent(
                        leads = leads,
                        onAdd = { ref, name, addr, contact ->
                            leadsViewModel.addLead(ref, name, addr, contact)
                        }
                    )
                }
            }
        }
    }
}

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private val authViewModel: dev.solora.auth.AuthViewModel by activityViewModels {
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
                    val profile by profileViewModel.profile.collectAsState()
                    ProfileScreen(
                        profile = profile,
                        onEditProfile = { navController.navigate(R.id.editProfileFragment) },
                        onChangePassword = { navController.navigate(R.id.changePasswordFragment) },
                        onOpenSettings = { navController.navigate(R.id.settingsFragment) },
                        onLogout = { authViewModel.logout() }
                    )
                }
            }
        }
    }
}

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme { SettingsScreenContent() }
            }
        }
    }
}

class NotificationsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme { NotificationsScreen() }
            }
        }
    }
}

class EditProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by activityViewModels {
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
                    val profile by profileViewModel.profile.collectAsState()
                    EditProfileScreen(
                        initial = profile,
                        onSave = { profileViewModel.updateProfile(it) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

class ChangePasswordFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by activityViewModels {
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
                    ChangePasswordScreen(
                        onSubmit = { current, new -> profileViewModel.changePassword(current, new) },
                        onDone = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
