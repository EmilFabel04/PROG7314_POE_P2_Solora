package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.profile.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    
    // UI Elements
    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvBio: TextView
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners(view)
        observeViewModel()
    }
    
    private fun initializeViews(view: View) {
        tvAvatar = view.findViewById(R.id.tv_avatar)
        tvName = view.findViewById(R.id.tv_name)
        tvTitle = view.findViewById(R.id.tv_title)
        tvEmail = view.findViewById(R.id.tv_email)
        tvPhone = view.findViewById(R.id.tv_phone)
        tvLocation = view.findViewById(R.id.tv_location)
        tvBio = view.findViewById(R.id.tv_bio)
    }
    
    private fun setupClickListeners(view: View) {
        // Set up click listeners for the action items
        val actionItems = listOf(
            R.id.btn_edit_profile to R.id.action_to_edit_profile,
            R.id.btn_change_password to R.id.action_to_change_password,
            R.id.btn_settings to R.id.action_to_settings
        )
        
        actionItems.forEach { (viewId, actionId) ->
            view.findViewById<View>(viewId)?.setOnClickListener {
                try {
                    findNavController().navigate(actionId)
                } catch (e: Exception) {
                    android.util.Log.e("ProfileFragment", "Navigation error: ${e.message}")
                    Toast.makeText(requireContext(), "Navigation not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Logout functionality
        view.findViewById<View>(R.id.btn_logout)?.setOnClickListener {
            logout()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.userProfile.collect { userProfile ->
                userProfile?.let { user ->
                    updateUI(user)
                } ?: run {
                    // No profile data - show default or loading state
                    showDefaultProfile()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isLoading.collect { isLoading ->
                // Handle loading state if needed
                if (isLoading) {
                    android.util.Log.d("ProfileFragment", "Loading profile...")
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(requireContext(), "Profile Error: $it", Toast.LENGTH_LONG).show()
                    profileViewModel.clearError()
                }
            }
        }
    }
    
    private fun updateUI(user: dev.solora.data.FirebaseUser) {
        try {
            // Update avatar with initials
            val initials = "${user.name.take(1)}${user.surname.take(1)}"
            tvAvatar.text = initials.uppercase()
            
            // Update name and title
            tvName.text = "${user.name} ${user.surname}"
            tvTitle.text = when (user.role) {
                "admin" -> "Administrator"
                "sales_consultant" -> "Sales Consultant"
                "manager" -> "Manager"
                else -> "Team Member"
            }
            
            // Update contact information
            tvEmail.text = user.email
            tvPhone.text = user.phone ?: "Not provided"
            tvLocation.text = user.company ?: "Not specified"
            
            // Update bio
            tvBio.text = "Dedicated to guiding clients through sustainable solar solutions."
            
            android.util.Log.d("ProfileFragment", "Profile UI updated for user: ${user.name} ${user.surname}")
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error updating UI: ${e.message}", e)
            Toast.makeText(requireContext(), "Error displaying profile", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDefaultProfile() {
        tvAvatar.text = "??"
        tvName.text = "Loading..."
        tvTitle.text = ""
        tvEmail.text = ""
        tvPhone.text = ""
        tvLocation.text = ""
        tvBio.text = ""
    }
    
    private fun logout() {
        try {
            // Navigate to auth screen
            findNavController().navigate(R.id.action_start_to_auth)
            android.util.Log.d("ProfileFragment", "User logged out")
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Logout navigation error: ${e.message}")
            Toast.makeText(requireContext(), "Logout failed", Toast.LENGTH_SHORT).show()
        }
    }
}


