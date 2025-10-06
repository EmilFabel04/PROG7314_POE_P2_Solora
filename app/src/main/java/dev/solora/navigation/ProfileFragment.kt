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
import dev.solora.auth.AuthViewModel
import dev.solora.api.FirebaseFunctionsApi
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val firebaseApi = FirebaseFunctionsApi()
    
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
        
        // Observe settings to ensure they're loaded and update profile display
        viewLifecycleOwner.lifecycleScope.launch {
            settingsViewModel.settings.collect { settings ->
                android.util.Log.d("ProfileFragment", "Settings observed: consultantName=${settings.companySettings.consultantName}")
                // Update profile display with consultant name from settings
                updateProfileWithSettings(settings.companySettings)
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
    
    private fun updateProfileWithSettings(companySettings: dev.solora.settings.CompanySettings) {
        try {
            // Update profile display with consultant name from settings
            val consultantName = companySettings.consultantName
            if (consultantName.isNotEmpty()) {
                // Update name display with consultant name
                tvName.text = consultantName
                
                // Update avatar with consultant name initials
                val nameParts = consultantName.trim().split(" ")
                val initials = if (nameParts.size >= 2) {
                    "${nameParts[0].take(1)}${nameParts[1].take(1)}"
                } else {
                    consultantName.take(2)
                }
                tvAvatar.text = initials.uppercase()
                
                android.util.Log.d("ProfileFragment", "Profile updated with consultant name: $consultantName")
            } else {
                android.util.Log.d("ProfileFragment", "No consultant name set in settings")
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error updating profile with settings: ${e.message}", e)
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
        // Show confirmation dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You will need to sign in again to access your account.")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        // Show loading message
        dev.solora.utils.ToastUtils.showOrangeToast(
            requireContext(), 
            "Logging out..."
        )
        
        android.util.Log.d("ProfileFragment", "Starting logout process")
        
        // Clear any cached data
        clearUserData()
        
        // Use AuthViewModel logout method (clears DataStore and Firebase Auth)
        authViewModel.logout()
        
        // Observe the auth state to know when logout is complete
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is dev.solora.auth.AuthState.Success -> {
                        android.util.Log.d("ProfileFragment", "Logout successful: ${state.message}")
                        
                        // Clear ViewModels data
                        profileViewModel.clearUserData()
                        settingsViewModel.clearSettings()
                        
                        // Show success message
                        dev.solora.utils.ToastUtils.showOrangeToast(
                            requireContext(), 
                            "Successfully logged out"
                        )
                        
                        // Navigate to login screen with proper cleanup
                        findNavController().navigate(R.id.action_logout_to_login)
                        
                        android.util.Log.d("ProfileFragment", "User logged out and navigated to login screen")
                        
                        // Clear the auth state to prevent further processing
                        authViewModel.clearAuthState()
                    }
                    is dev.solora.auth.AuthState.Error -> {
                        android.util.Log.e("ProfileFragment", "Logout failed: ${state.message}")
                        Toast.makeText(
                            requireContext(), 
                            "Logout failed: ${state.message}", 
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Clear the auth state
                        authViewModel.clearAuthState()
                    }
                    else -> {
                        // Loading or Idle state - do nothing, wait for Success or Error
                    }
                }
            }
        }
    }
    
    private fun clearUserData() {
        try {
            // Clear any local preferences or cached data
            val sharedPrefs = requireContext().getSharedPreferences("solora_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            
            android.util.Log.d("ProfileFragment", "Local user data cleared")
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error clearing user data: ${e.message}", e)
        }
    }
    
    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        
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
        
        // Load user settings data and populate form
        loadUserSettingsAndPopulateForm(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
        
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
    
    private fun loadUserSettingsAndPopulateForm(
        etName: TextInputEditText,
        etSurname: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        etCompany: TextInputEditText,
        etCompanyName: TextInputEditText,
        etCompanyAddress: TextInputEditText,
        etCompanyPhone: TextInputEditText,
        etCompanyEmail: TextInputEditText,
        etCompanyWebsite: TextInputEditText
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d("ProfileFragment", "Starting to load user settings...")
                
                // Try to get settings from SettingsViewModel first
                val currentSettings = settingsViewModel.settings.value
                android.util.Log.d("ProfileFragment", "SettingsViewModel value: $currentSettings")
                
                if (currentSettings != null && currentSettings.companySettings.consultantName.isNotEmpty()) {
                    android.util.Log.d("ProfileFragment", "Using SettingsViewModel data: ${currentSettings.companySettings.consultantName}")
                    // Populate with settings data
                    populateFormFromSettings(currentSettings, etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                } else {
                    android.util.Log.d("ProfileFragment", "SettingsViewModel empty, trying REST API...")
                    
                    // Fallback: Use REST API to get user settings
                    val firebaseApi = FirebaseFunctionsApi()
                    val result = firebaseApi.getSettings()
                    
                    android.util.Log.d("ProfileFragment", "REST API result: $result")
                    
                    if (result.isSuccess) {
                        val settingsData = result.getOrNull()
                        android.util.Log.d("ProfileFragment", "API settings data: $settingsData")
                        
                        if (settingsData != null) {
                            // Create CompanySettings from the API response
                            val companySettings = dev.solora.settings.CompanySettings(
                                companyName = settingsData["companyName"] as? String ?: "",
                                companyAddress = settingsData["companyAddress"] as? String ?: "",
                                companyPhone = settingsData["companyPhone"] as? String ?: "",
                                companyEmail = settingsData["companyEmail"] as? String ?: "",
                                companyWebsite = settingsData["companyWebsite"] as? String ?: "",
                                consultantName = settingsData["consultantName"] as? String ?: "",
                                consultantPhone = settingsData["consultantPhone"] as? String ?: "",
                                consultantEmail = settingsData["consultantEmail"] as? String ?: "",
                                consultantLicense = settingsData["consultantLicense"] as? String ?: ""
                            )
                            
                            android.util.Log.d("ProfileFragment", "Created CompanySettings: consultantName=${companySettings.consultantName}, consultantEmail=${companySettings.consultantEmail}")
                            
                            // Create AppSettings with the company settings
                            val appSettings = dev.solora.settings.AppSettings(companySettings = companySettings)
                            populateFormFromSettings(appSettings, etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                        } else {
                            android.util.Log.d("ProfileFragment", "API returned null data, trying getUserProfile API")
                            // Try getUserProfile API endpoint
                            val userProfileResult = firebaseApi.getUserProfile()
                            if (userProfileResult.isSuccess) {
                                val userProfileData = userProfileResult.getOrNull()
                                if (userProfileData != null) {
                                    android.util.Log.d("ProfileFragment", "Got user profile data from API: $userProfileData")
                                    populateFormFromApiUserProfile(userProfileData, etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                                } else {
                                    android.util.Log.d("ProfileFragment", "No user profile data from API, using ViewModel fallback")
                                    populateFormFromUserProfile(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                                }
                            } else {
                                android.util.Log.d("ProfileFragment", "getUserProfile API failed: ${userProfileResult.exceptionOrNull()?.message}, using ViewModel fallback")
                                populateFormFromUserProfile(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                            }
                        }
                    } else {
                        android.util.Log.d("ProfileFragment", "getSettings API call failed: ${result.exceptionOrNull()?.message}, trying getUserProfile API")
                        // Try getUserProfile API as fallback
                        val userProfileResult = firebaseApi.getUserProfile()
                        if (userProfileResult.isSuccess) {
                            val userProfileData = userProfileResult.getOrNull()
                            if (userProfileData != null) {
                                android.util.Log.d("ProfileFragment", "Got user profile data from API fallback: $userProfileData")
                                populateFormFromApiUserProfile(userProfileData, etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                            } else {
                                android.util.Log.d("ProfileFragment", "No user profile data from API fallback, using ViewModel fallback")
                                populateFormFromUserProfile(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                            }
                        } else {
                            android.util.Log.d("ProfileFragment", "getUserProfile API fallback failed: ${userProfileResult.exceptionOrNull()?.message}, using ViewModel fallback")
                            populateFormFromUserProfile(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Error loading user settings: ${e.message}", e)
                // Fallback to user profile data
                populateFormFromUserProfile(etName, etSurname, etEmail, etPhone, etCompany, etCompanyName, etCompanyAddress, etCompanyPhone, etCompanyEmail, etCompanyWebsite)
            }
        }
    }
    
    private fun populateFormFromSettings(
        settings: dev.solora.settings.AppSettings,
        etName: TextInputEditText,
        etSurname: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        etCompany: TextInputEditText,
        etCompanyName: TextInputEditText,
        etCompanyAddress: TextInputEditText,
        etCompanyPhone: TextInputEditText,
        etCompanyEmail: TextInputEditText,
        etCompanyWebsite: TextInputEditText
    ) {
        android.util.Log.d("ProfileFragment", "populateFormFromSettings called with consultantName: ${settings.companySettings.consultantName}")
        
        // Populate consultant information from settings
        val consultantName = settings.companySettings.consultantName
        val consultantEmail = settings.companySettings.consultantEmail
        val consultantPhone = settings.companySettings.consultantPhone
        val companyName = settings.companySettings.companyName
        
        android.util.Log.d("ProfileFragment", "Setting form fields - Name: '$consultantName', Email: '$consultantEmail', Phone: '$consultantPhone', Company: '$companyName'")
        
        etName.setText(consultantName)
        etSurname.setText("") // Surname not stored in settings
        etEmail.setText(consultantEmail)
        etPhone.setText(consultantPhone)
        etCompany.setText(companyName)
        
        // Populate company information
        etCompanyName.setText(settings.companySettings.companyName)
        etCompanyAddress.setText(settings.companySettings.companyAddress)
        etCompanyPhone.setText(settings.companySettings.companyPhone)
        etCompanyEmail.setText(settings.companySettings.companyEmail)
        etCompanyWebsite.setText(settings.companySettings.companyWebsite)
        
        android.util.Log.d("ProfileFragment", "Form fields set successfully")
    }
    
    private fun populateFormFromUserProfile(
        etName: TextInputEditText,
        etSurname: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        etCompany: TextInputEditText,
        etCompanyName: TextInputEditText,
        etCompanyAddress: TextInputEditText,
        etCompanyPhone: TextInputEditText,
        etCompanyEmail: TextInputEditText,
        etCompanyWebsite: TextInputEditText
    ) {
        android.util.Log.d("ProfileFragment", "populateFormFromUserProfile called - fallback to user profile data")
        
        // Fallback to user profile data
        val currentUser = profileViewModel.userProfile.value
        android.util.Log.d("ProfileFragment", "Current user profile: $currentUser")
        
        currentUser?.let { user ->
            android.util.Log.d("ProfileFragment", "Setting form fields from user profile - Name: '${user.name}', Email: '${user.email}', Phone: '${user.phone}', Company: '${user.company}'")
            
            etName.setText(user.name)
            etSurname.setText(user.surname)
            etEmail.setText(user.email)
            etPhone.setText(user.phone ?: "")
            etCompany.setText(user.company ?: "")
            etCompanyName.setText(user.company ?: "")
            // Leave other company fields empty
        }
        
        android.util.Log.d("ProfileFragment", "Form populated from user profile fallback")
    }
    
    private fun populateFormFromApiUserProfile(
        userProfileData: Map<String, Any>,
        etName: TextInputEditText,
        etSurname: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        etCompany: TextInputEditText,
        etCompanyName: TextInputEditText,
        etCompanyAddress: TextInputEditText,
        etCompanyPhone: TextInputEditText,
        etCompanyEmail: TextInputEditText,
        etCompanyWebsite: TextInputEditText
    ) {
        android.util.Log.d("ProfileFragment", "populateFormFromApiUserProfile called with data: $userProfileData")
        
        // Extract user profile data from API response
        val name = userProfileData["name"] as? String ?: ""
        val surname = userProfileData["surname"] as? String ?: ""
        val email = userProfileData["email"] as? String ?: ""
        val phone = userProfileData["phone"] as? String ?: ""
        val company = userProfileData["company"] as? String ?: ""
        
        // Extract company information if available
        val companyName = userProfileData["companyName"] as? String ?: ""
        val companyAddress = userProfileData["companyAddress"] as? String ?: ""
        val companyPhone = userProfileData["companyPhone"] as? String ?: ""
        val companyEmail = userProfileData["companyEmail"] as? String ?: ""
        val companyWebsite = userProfileData["companyWebsite"] as? String ?: ""
        
        android.util.Log.d("ProfileFragment", "Setting form fields from API user profile - Name: '$name', Email: '$email', Phone: '$phone', Company: '$company'")
        
        // Populate form fields
        etName.setText(name)
        etSurname.setText(surname)
        etEmail.setText(email)
        etPhone.setText(phone)
        etCompany.setText(company)
        etCompanyName.setText(companyName)
        etCompanyAddress.setText(companyAddress)
        etCompanyPhone.setText(companyPhone)
        etCompanyEmail.setText(companyEmail)
        etCompanyWebsite.setText(companyWebsite)
        
        android.util.Log.d("ProfileFragment", "Form populated from API user profile data")
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
            
            // Also update the user profile via API
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val userProfile = dev.solora.data.FirebaseUser(
                        id = null,
                        name = name,
                        surname = surname.ifEmpty { name },
                        email = email,
                        phone = phone,
                        company = companyName ?: company,
                        createdAt = null,
                        updatedAt = null
                    )
                    
                    val apiResult = firebaseApi.updateUserProfile(userProfile)
                    if (apiResult.isSuccess) {
                        android.util.Log.d("ProfileFragment", "User profile updated via API successfully")
                    } else {
                        android.util.Log.w("ProfileFragment", "API user profile update failed: ${apiResult.exceptionOrNull()?.message}")
                        // Fallback to ViewModel
                        profileViewModel.updateUserProfile(
                            name = name,
                            surname = surname.ifEmpty { name },
                            email = email,
                            phone = phone,
                            company = companyName ?: company
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileFragment", "Error updating user profile via API: ${e.message}", e)
                    // Fallback to ViewModel
                    profileViewModel.updateUserProfile(
                        name = name,
                        surname = surname.ifEmpty { name },
                        email = email,
                        phone = phone,
                        company = companyName ?: company
                    )
                }
            }
            
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