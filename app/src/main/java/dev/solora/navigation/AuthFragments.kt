package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.asLiveData
import dev.solora.R
import dev.solora.auth.AuthState
import dev.solora.auth.AuthViewModel

class OnboardingFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_login).setOnClickListener { findNavController().navigate(R.id.action_onboarding_to_login) }
        view.findViewById<View>(R.id.btn_register).setOnClickListener { findNavController().navigate(R.id.action_onboarding_to_register) }
    }
}

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val emailInput = view.findViewById<android.widget.EditText>(R.id.et_email)
        val passwordInput = view.findViewById<android.widget.EditText>(R.id.et_password)
        val submitButton = view.findViewById<android.widget.Button>(R.id.btn_submit)
        
        // Observe auth state
        authViewModel.authState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    submitButton.isEnabled = false
                    submitButton.text = "Logging in..."
                }
                is AuthState.Success -> {
                    submitButton.isEnabled = true
                    submitButton.text = "Login"
                    findNavController().navigate(R.id.action_login_to_main)
                    authViewModel.clearAuthState()
                }
                is AuthState.Error -> {
                    submitButton.isEnabled = true
                    submitButton.text = "Login"
                    // Show error message
                    android.widget.Toast.makeText(requireContext(), state.message, android.widget.Toast.LENGTH_LONG).show()
                    authViewModel.clearAuthState()
                }
                is AuthState.Idle -> {
                    submitButton.isEnabled = true
                    submitButton.text = "Login"
                }
            }
        }
        
        submitButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""
            
            if (email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please fill in all fields", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            authViewModel.login(email, password)
        }
        
        view.findViewById<View>(R.id.btn_to_register).setOnClickListener { 
            findNavController().navigate(R.id.action_login_to_register) 
        }
    }
}

class RegisterFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val nameInput = view.findViewById<android.widget.EditText>(R.id.et_name)
        val emailInput = view.findViewById<android.widget.EditText>(R.id.et_email)
        val passwordInput = view.findViewById<android.widget.EditText>(R.id.et_password)
        val registerButton = view.findViewById<android.widget.Button>(R.id.btn_register)
        
        // Observe auth state
        authViewModel.authState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    registerButton.isEnabled = false
                    registerButton.text = "Creating Account..."
                }
                is AuthState.Success -> {
                    registerButton.isEnabled = true
                    registerButton.text = "Create Account"
                    findNavController().navigate(R.id.action_register_to_main)
                    authViewModel.clearAuthState()
                }
                is AuthState.Error -> {
                    registerButton.isEnabled = true
                    registerButton.text = "Create Account"
                    // Show error message
                    android.widget.Toast.makeText(requireContext(), state.message, android.widget.Toast.LENGTH_LONG).show()
                    authViewModel.clearAuthState()
                }
                is AuthState.Idle -> {
                    registerButton.isEnabled = true
                    registerButton.text = "Create Account"
                }
            }
        }
        
        registerButton.setOnClickListener {
            val name = nameInput.text?.toString()?.trim() ?: ""
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please fill in all fields", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                android.widget.Toast.makeText(requireContext(), "Password must be at least 6 characters", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            authViewModel.register(name, email, password)
        }
        
        view.findViewById<View>(R.id.btn_back_login).setOnClickListener { findNavController().popBackStack() }
    }
}


