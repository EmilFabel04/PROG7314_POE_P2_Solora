package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.profile.ProfileViewModel
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    
    // UI Elements
    private lateinit var etName: TextInputEditText
    private lateinit var etSurname: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etCompany: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        observeViewModel()
        loadCurrentProfile()
    }
    
    private fun initializeViews(view: View) {
        etName = view.findViewById(R.id.et_name)
        etSurname = view.findViewById(R.id.et_surname)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etCompany = view.findViewById(R.id.et_company)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnSave.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.userProfile.collect { userProfile ->
                userProfile?.let { user ->
                    populateFields(user)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.isLoading.collect { isLoading ->
                btnSave.isEnabled = !isLoading
                if (isLoading) {
                    btnSave.text = "Saving..."
                } else {
                    btnSave.text = "Save"
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                    profileViewModel.clearError()
                }
            }
        }
    }
    
    private fun loadCurrentProfile() {
        profileViewModel.refreshProfile()
    }
    
    private fun populateFields(user: dev.solora.data.FirebaseUser) {
        try {
            etName.setText(user.name)
            etSurname.setText(user.surname)
            etEmail.setText(user.email)
            etPhone.setText(user.phone ?: "")
            etCompany.setText(user.company ?: "")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading profile data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveProfile() {
        try {
            val name = etName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim().takeIf { it.isNotEmpty() }
            val company = etCompany.text.toString().trim().takeIf { it.isNotEmpty() }
            
            // Basic validation
            if (name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Save profile
            profileViewModel.updateUserProfile(
                name = name,
                surname = surname,
                email = email,
                phone = phone,
                company = company
            )
            
            // Navigate back after successful save
            lifecycleScope.launch {
                // Wait for the update to complete
                profileViewModel.userProfile.collect {
                    if (it != null && !profileViewModel.isLoading.value) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                        return@collect
                    }
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving profile", Toast.LENGTH_SHORT).show()
        }
    }
}


