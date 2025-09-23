package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.solora.profile.ChangePasswordScreen
import dev.solora.profile.EditProfileScreen
import dev.solora.profile.ProfileViewModel
import dev.solora.quotes.QuoteDetailScreen
import dev.solora.quotes.QuotesViewModel

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent { dev.solora.settings.SettingsScreenContent() }
        }
    }
}

class NotificationsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // Reuse the compose NotificationsScreen from previous MainActivity if needed
                // For now show a simple placeholder
                dev.solora.ui.Center("Notifications")
            }
        }
    }
}

class EditProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val profile = profileViewModel.profile.value
                EditProfileScreen(
                    initial = profile,
                    onSave = { profileViewModel.updateProfile(it) },
                    onBack = { findNavController().popBackStack() }
                )
            }
        }
    }
}

class ChangePasswordFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ChangePasswordScreen(
                    onSubmit = { current, new -> profileViewModel.changePassword(current, new) },
                    onDone = { findNavController().popBackStack() }
                )
            }
        }
    }
}

class QuoteDetailFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val id = requireArguments().getLong("id", 0L)
                QuoteDetailScreen(id = id, onBack = { findNavController().popBackStack() })
            }
        }
    }
}


