package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dev.solora.R
import dev.solora.profile.ProfileViewModel
import dev.solora.settings.SettingsViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    // UI Elements
    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvTitle: TextView
    private lateinit var switchNotifications: Switch
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners(view)
        observeViewModel()
        
        // Load user profile
        profileViewModel.loadUserProfile()
    }
    
    private fun initializeViews(view: View) {
        tvAvatar = view.findViewById(R.id.tv_avatar)
        tvName = view.findViewById(R.id.tv_name)
        tvTitle = view.findViewById(R.id.tv_title)
        switchNotifications = view.findViewById(R.id.switch_notifications)
    }
    
    private fun setupClickListeners(view: View) {
        // Edit Profile
        view.findViewById<View>(R.id.btn_edit_profile)?.setOnClickListener {
            showEditProfileDialog()
        }
        
        // Change Password
        view.findViewById<View>(R.id.btn_change_password)?.setOnClickListener {
            showChangePasswordDialog()
        }
        
        // Authentication
        view.findViewById<View>(R.id.btn_authentication)?.setOnClickListener {
            showAuthenticationDialog()
        }
        
        // Language
        view.findViewById<View>(R.id.btn_language)?.setOnClickListener {
            showLanguageDialog()
        }
        
        // Notifications Toggle
        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            // Toggle the switch
            switchNotifications.isChecked = !switchNotifications.isChecked
        }
        
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            handleNotificationToggle(isChecked)
        }
        
        // Logout
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
                    showDefaultProfile()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isLoading.collect { isLoading ->
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
                "sales_consultant" -> "Sales Representative"
                "manager" -> "Manager"
                else -> "Team Member"
            }
            
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
    }
    
    private fun showAuthenticationDialog() {
        Toast.makeText(requireContext(), "Authentication settings coming soon!", Toast.LENGTH_LONG).show()
    }
    
    private fun showLanguageDialog() {
        Toast.makeText(requireContext(), "Language settings coming soon!", Toast.LENGTH_LONG).show()
    }
    
    private fun handleNotificationToggle(isEnabled: Boolean) {
        if (isEnabled) {
            Toast.makeText(requireContext(), "Push notifications enabled", Toast.LENGTH_SHORT).show()
            android.util.Log.d("ProfileFragment", "Notifications enabled")
        } else {
            Toast.makeText(requireContext(), "Push notifications disabled", Toast.LENGTH_SHORT).show()
            android.util.Log.d("ProfileFragment", "Notifications disabled")
        }
    }
    
    private fun logout() {
        try {
            Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show()
            
            // Sign out from Firebase Auth
            auth.signOut()
            
            android.util.Log.d("ProfileFragment", "User signed out from Firebase Auth")
            
            // Navigate to auth screen
            findNavController().navigate(R.id.action_start_to_auth)
            
            android.util.Log.d("ProfileFragment", "User logged out and navigated to auth screen")
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Logout error: ${e.message}", e)
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        
        // Get current user data
        val currentUser = profileViewModel.userProfile.value
        
        // Initialize form fields
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_name)
        val etSurname = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_surname)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_email)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_phone)
        val etCompany = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company)
        val etCompanyName = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company_name)
        val etCompanyAddress = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company_address)
        val etCompanyPhone = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company_phone)
        val etCompanyEmail = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company_email)
        val etCompanyWebsite = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_company_website)
        
        // Populate fields with current user data from user_settings (consultant info)
        val currentSettings = settingsViewModel.settings.value
        currentSettings?.let { settings ->
            // Use consultant information from settings as the main profile data
            etName.setText(settings.companySettings.consultantName)
            etSurname.setText("") // Surname not stored in settings, leave empty
            etEmail.setText(settings.companySettings.consultantEmail)
            etPhone.setText(settings.companySettings.consultantPhone)
            etCompany.setText(settings.companySettings.companyName)
            
            // Company information
            etCompanyName.setText(settings.companySettings.companyName)
            etCompanyAddress.setText(settings.companySettings.companyAddress)
            etCompanyPhone.setText(settings.companySettings.companyPhone)
            etCompanyEmail.setText(settings.companySettings.companyEmail)
            etCompanyWebsite.setText(settings.companySettings.companyWebsite)
        }
        
        // Fallback to user profile data if settings are empty
        currentUser?.let { user ->
            if (currentSettings?.companySettings?.consultantName.isNullOrEmpty()) {
                etName.setText(user.name)
            }
            if (currentSettings?.companySettings?.consultantEmail.isNullOrEmpty()) {
                etEmail.setText(user.email)
            }
            if (currentSettings?.companySettings?.consultantPhone.isNullOrEmpty()) {
                etPhone.setText(user.phone ?: "")
            }
            if (currentSettings?.companySettings?.companyName.isNullOrEmpty()) {
                etCompany.setText(user.company ?: "")
                etCompanyName.setText(user.company ?: "")
            }
        }
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up button click listeners
        dialogView.findViewById<ImageButton>(R.id.btn_close_edit_profile).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_cancel_edit_profile).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_save_edit_profile).setOnClickListener {
            saveProfileFromDialog(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite, dialog)
        }
        
        dialog.show()
    }
    
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        
        // Initialize form fields
        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up button click listeners
        dialogView.findViewById<ImageButton>(R.id.btn_close_change_password).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_cancel_change_password).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_save_change_password).setOnClickListener {
            changePasswordFromDialog(etCurrentPassword, etNewPassword, etConfirmPassword, dialog)
        }
        
        dialog.show()
    }
    
    private fun saveProfileFromDialog(
        etName: TextInputEditText,
        etSurname: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        etCompany: TextInputEditText,
        etCompanyName: TextInputEditText,
        etCompanyAddress: TextInputEditText,
        etCompanyPhone: TextInputEditText,
        etCompanyEmail: TextInputEditText,
        etCompanyWebsite: TextInputEditText,
        dialog: AlertDialog
    ) {
        try {
            val name = etName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim().takeIf { it.isNotEmpty() }
            val company = etCompany.text.toString().trim().takeIf { it.isNotEmpty() }
            
            // Company information
            val companyName = etCompanyName.text.toString().trim().takeIf { it.isNotEmpty() }
            val companyAddress = etCompanyAddress.text.toString().trim().takeIf { it.isNotEmpty() }
            val companyPhone = etCompanyPhone.text.toString().trim().takeIf { it.isNotEmpty() }
            val companyEmail = etCompanyEmail.text.toString().trim().takeIf { it.isNotEmpty() }
            val companyWebsite = etCompanyWebsite.text.toString().trim().takeIf { it.isNotEmpty() }
            
            // Basic validation
            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in name and email fields", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validate company email if provided
            companyEmail?.let { companyEmailValue ->
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(companyEmailValue).matches()) {
                    Toast.makeText(requireContext(), "Please enter a valid company email address", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // Update company settings with all the information
            val currentSettings = settingsViewModel.settings.value
            if (currentSettings != null) {
                val updatedCompanySettings = currentSettings.companySettings.copy(
                    // Consultant information (stored as the user's profile in settings)
                    consultantName = name,
                    consultantEmail = email,
                    consultantPhone = phone ?: currentSettings.companySettings.consultantPhone,
                    
                    // Company information
                    companyName = companyName ?: currentSettings.companySettings.companyName,
                    companyAddress = companyAddress ?: currentSettings.companySettings.companyAddress,
                    companyPhone = companyPhone ?: currentSettings.companySettings.companyPhone,
                    companyEmail = companyEmail ?: currentSettings.companySettings.companyEmail,
                    companyWebsite = companyWebsite ?: currentSettings.companySettings.companyWebsite
                )
                settingsViewModel.updateCompanySettings(updatedCompanySettings)
            }
            
            // Also update the user profile as a fallback
            profileViewModel.updateUserProfile(
                name = name,
                surname = surname.ifEmpty { name }, // Use name as surname if empty
                email = email,
                phone = phone,
                company = companyName ?: company
            )
            
            Toast.makeText(requireContext(), "Profile and company information updated successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error saving profile: ${e.message}", e)
            Toast.makeText(requireContext(), "Error saving profile", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun changePasswordFromDialog(
        etCurrentPassword: TextInputEditText,
        etNewPassword: TextInputEditText,
        etConfirmPassword: TextInputEditText,
        dialog: AlertDialog
    ) {
        try {
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            
            // Basic validation
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all password fields", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "New password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "New passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Change password using Firebase Auth
            val user = auth.currentUser
            if (user != null) {
                // For now, just show success message
                // In a real implementation, you would re-authenticate and update password
                Toast.makeText(requireContext(), "Password change feature coming soon!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error changing password: ${e.message}", e)
            Toast.makeText(requireContext(), "Error changing password", Toast.LENGTH_SHORT).show()
        }
    }
}