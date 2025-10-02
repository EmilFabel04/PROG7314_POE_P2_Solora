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
import android.widget.Switch
import android.view.View.OnClickListener
import android.content.Context

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val open = OnClickListener { idView ->
            when (idView.id) {
                R.id.row_edit_profile -> findNavController().navigate(R.id.action_to_edit_profile)
                R.id.row_change_password -> findNavController().navigate(R.id.action_to_change_password)
                R.id.row_authentication -> findNavController().navigate(R.id.action_to_authentication)
                R.id.row_logout -> findNavController().navigate(R.id.action_start_to_auth)
            }
        }
        view.findViewById<View>(R.id.row_edit_profile).setOnClickListener(open)
        view.findViewById<View>(R.id.row_change_password).setOnClickListener(open)
        view.findViewById<View>(R.id.row_authentication).setOnClickListener(open)
        view.findViewById<View>(R.id.row_logout).setOnClickListener(open)

        val shared = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val switchPush = view.findViewById<Switch>(R.id.switch_push)
        switchPush.isChecked = shared.getBoolean("push_enabled", true)
        switchPush.setOnCheckedChangeListener { _, isChecked ->
            shared.edit().putBoolean("push_enabled", isChecked).apply()
        }
    }
}


