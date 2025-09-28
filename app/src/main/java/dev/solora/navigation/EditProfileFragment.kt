package dev.solora.navigation

import android.content.Context
import android.content.SharedPreferences
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
import dev.solora.R
import dev.solora.profile.ProfileViewModel

class EditProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var sharedPrefs: SharedPreferences
    
    private lateinit var etName: EditText
    private lateinit var etTitle: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText
    private lateinit var etBio: EditText
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPrefs = requireContext().getSharedPreferences("solora_profile", Context.MODE_PRIVATE)
        
        initializeViews(view)
        loadCurrentProfile()
        setupClickListeners()
    }
    
    private fun initializeViews(view: View) {
        etName = view.findViewById(R.id.et_name)
        etTitle = view.findViewById(R.id.et_title)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etLocation = view.findViewById(R.id.et_location)
        etBio = view.findViewById(R.id.et_bio)
    }
    
    private fun loadCurrentProfile() {
        // Load existing profile data
        etName.setText(sharedPrefs.getString("profile_name", ""))
        etTitle.setText(sharedPrefs.getString("profile_title", "Solar Consultant"))
        etEmail.setText(sharedPrefs.getString("profile_email", ""))
        etPhone.setText(sharedPrefs.getString("profile_phone", ""))
        etLocation.setText(sharedPrefs.getString("profile_location", "Cape Town, South Africa"))
        etBio.setText(sharedPrefs.getString("profile_bio", ""))
    }
    
    private fun setupClickListeners() {
        view?.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener { 
            findNavController().popBackStack() 
        }
        
        view?.findViewById<Button>(R.id.btn_save)?.setOnClickListener { 
            saveProfile()
        }
    }
    
    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val title = etTitle.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val bio = etBio.text.toString().trim()
        
        // Basic validation
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
            etName.requestFocus()
            return
        }
        
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            etEmail.requestFocus()
            return
        }
        
        // Save to SharedPreferences
        sharedPrefs.edit().apply {
            putString("profile_name", name)
            putString("profile_title", title)
            putString("profile_email", email)
            putString("profile_phone", phone)
            putString("profile_location", location)
            putString("profile_bio", bio)
            putLong("profile_updated", System.currentTimeMillis())
            apply()
        }
        
        Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }
}


