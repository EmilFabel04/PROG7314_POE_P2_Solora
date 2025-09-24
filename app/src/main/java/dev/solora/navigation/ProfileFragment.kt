package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.profile.ProfileViewModel

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_edit_profile).setOnClickListener { findNavController().navigate(R.id.action_to_edit_profile) }
        view.findViewById<View>(R.id.btn_change_password).setOnClickListener { findNavController().navigate(R.id.action_to_change_password) }
        view.findViewById<View>(R.id.btn_settings).setOnClickListener { findNavController().navigate(R.id.action_to_settings) }
        view.findViewById<View>(R.id.btn_logout).setOnClickListener { findNavController().navigate(R.id.action_start_to_auth) }
    }
}


