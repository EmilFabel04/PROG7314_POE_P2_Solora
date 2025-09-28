package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dev.solora.R
import dev.solora.profile.ProfileViewModel

class ChangePasswordFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var etCurrent: EditText
    private lateinit var etNew: EditText
    private lateinit var etConfirm: EditText
    private lateinit var btnUpdate: Button
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
    }
    
    private fun initializeViews(view: View) {
        etCurrent = view.findViewById(R.id.et_current)
        etNew = view.findViewById(R.id.et_new)
        etConfirm = view.findViewById(R.id.et_confirm)
        btnUpdate = view.findViewById(R.id.btn_update)
    }
    
    private fun setupClickListeners() {
        btnUpdate.setOnClickListener {
            changePassword()
        }
    }
    
    private fun changePassword() {
        val currentPassword = etCurrent.text.toString().trim()
        val newPassword = etNew.text.toString().trim()
        val confirmPassword = etConfirm.text.toString().trim()
        
        // Validation
        if (currentPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your current password", Toast.LENGTH_SHORT).show()
            etCurrent.requestFocus()
            return
        }
        
        if (newPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
            etNew.requestFocus()
            return
        }
        
        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            etNew.requestFocus()
            return
        }
        
        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            etConfirm.requestFocus()
            return
        }
        
        if (currentPassword == newPassword) {
            Toast.makeText(requireContext(), "New password must be different from current password", Toast.LENGTH_SHORT).show()
            etNew.requestFocus()
            return
        }
        
        // Disable button during update
        btnUpdate.isEnabled = false
        btnUpdate.text = "Updating..."
        
        // Update password using Firebase Auth
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.email != null) {
            // Re-authenticate user first
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Re-authentication successful, now update password
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            // Password updated successfully
                            Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener { exception ->
                            // Password update failed
                            Toast.makeText(requireContext(), "Failed to update password: ${exception.message}", Toast.LENGTH_LONG).show()
                            resetButton()
                        }
                }
                .addOnFailureListener { exception ->
                    // Re-authentication failed
                    Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    etCurrent.requestFocus()
                    resetButton()
                }
        } else {
            // User not authenticated
            Toast.makeText(requireContext(), "Please log in again to change your password", Toast.LENGTH_SHORT).show()
            resetButton()
        }
    }
    
    private fun resetButton() {
        btnUpdate.isEnabled = true
        btnUpdate.text = "Update password"
    }
}


