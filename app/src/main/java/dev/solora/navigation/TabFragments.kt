package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.solora.home.HomeScreen
import dev.solora.leads.LeadsScreenVM
import dev.solora.profile.ProfileScreen
import dev.solora.profile.ProfileViewModel
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import dev.solora.quotes.QuotesScreenVM

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HomeScreen(
                    onOpenQuotes = { findNavController().navigate(R.id.quotesFragment) },
                    onOpenLeads = { findNavController().navigate(R.id.leadsFragment) },
                    onOpenNotifications = { findNavController().navigate(R.id.action_to_notifications) },
                    onOpenSettings = { findNavController().navigate(R.id.action_to_settings) }
                )
            }
        }
    }
}

class QuotesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                QuotesScreenVM(onQuoteSelected = { id ->
                    val bundle = Bundle().apply { putLong("id", id) }
                    findNavController().navigate(R.id.quoteDetailFragment, bundle)
                })
            }
        }
    }
}

class LeadsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply { setContent { LeadsScreenVM() } }
    }
}

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val profileState by profileViewModel.profile.collectAsStateWithLifecycle()
                ProfileScreen(
                    profile = profileState,
                    onEditProfile = { findNavController().navigate(R.id.action_to_edit_profile) },
                    onChangePassword = { findNavController().navigate(R.id.action_to_change_password) },
                    onOpenSettings = { findNavController().navigate(R.id.action_to_settings) },
                    onLogout = {
                        findNavController().navigate(R.id.action_start_to_auth)
                    }
                )
            }
        }
    }
}


