package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dev.solora.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordFragment : Fragment() {
    
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnBack: android.widget.ImageView
    
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
    }
    
    private fun initializeViews(view: View) {
        etCurrentPassword = view.findViewById(R.id.et_current_password)
        etNewPassword = view.findViewById(R.id.et_new_password)
        etConfirmPassword = view.findViewById(R.id.et_confirm_password)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnBack = view.findViewById(R.id.btn_back)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnSave.setOnClickListener {
            changePassword()
        }
    }
    
    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        
        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Change password
        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Changing..."
                
                val user = auth.currentUser
                if (user != null) {
                    // Re-authenticate user first
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                        user.email ?: "", currentPassword
                    )
                    
                    user.reauthenticate(credential).await()
                    
                    // Update password
                    user.updatePassword(newPassword).await()
                    
                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    
                } else {
                    Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                
                when {
                    e.message?.contains("wrong-password") == true -> {
                        Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_LONG).show()
                    }
                    e.message?.contains("weak-password") == true -> {
                        Toast.makeText(requireContext(), "Password is too weak", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(requireContext(), "Failed to change password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Save"
            }
        }
    }
}