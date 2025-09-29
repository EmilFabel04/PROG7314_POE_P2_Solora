package dev.solora.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dev.solora.R
import dev.solora.auth.AuthViewModel

class OnboardingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find and configure the Get Started button
        val getStartedButton = view.findViewById<View>(R.id.btn_get_started)
        
        if (getStartedButton != null) {
            // Ensure button is visible and clickable
            getStartedButton.visibility = View.VISIBLE
            getStartedButton.isClickable = true
            getStartedButton.isFocusable = true
            getStartedButton.isEnabled = true
            
            getStartedButton.setOnClickListener {
                // Save onboarding flag so that this screen is not shown again
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_seen", true).apply()

                // Navigate to the register screen
                findNavController().navigate(R.id.action_onboarding_to_register)
            }
        }
        
        // Note: "Already have account" link not present in this layout
        // Users can navigate back from register screen if needed
    }
}

class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Email/Password login
        val emailInput = view.findViewById<android.widget.EditText>(R.id.et_email)
        val passwordInput = view.findViewById<android.widget.EditText>(R.id.et_password)
        val submitButton = view.findViewById<LinearLayout>(R.id.btn_login)

        submitButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.login(email, password)
        }

        // Simple Google SSO setup
        setupGoogleSignIn(view)
        
        // Navigation
        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_to_register).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        view.findViewById<View>(R.id.txt_btn_sign_up).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun setupGoogleSignIn(view: View) {
        try {
            // Simple Google Sign-In configuration
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            view.findViewById<ImageButton>(R.id.btn_google_login).setOnClickListener {
                Log.d("LoginFragment", "Starting Google Sign-In...")
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Google Sign-In setup failed: ${e.message}")
            Toast.makeText(requireContext(), "Google Sign-In not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginFragment", "Google Sign-In successful: ${account.email}")
                
                if (account.idToken != null) {
                    authViewModel.loginWithGoogle(account.idToken!!)
                } else {
                    Log.e("LoginFragment", "No ID token received")
                    Toast.makeText(requireContext(), "Google authentication setup incomplete", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("LoginFragment", "Google Sign-In failed: ${e.statusCode}")
                when (e.statusCode) {
                    10 -> Toast.makeText(requireContext(), "Google configuration error. Please check Firebase setup.", Toast.LENGTH_LONG).show()
                    7 -> Toast.makeText(requireContext(), "Network error. Check internet connection.", Toast.LENGTH_LONG).show()
                    12501 -> { /* User cancelled - don't show error */ }
                    else -> Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Email/Password registration
        val nameInput = view.findViewById<android.widget.EditText>(R.id.et_name)
        val surnameInput = view.findViewById<android.widget.EditText>(R.id.et_surname)
        val emailInput = view.findViewById<android.widget.EditText>(R.id.et_email)
        val passwordInput = view.findViewById<android.widget.EditText>(R.id.et_password)
        val registerButton = view.findViewById<LinearLayout>(R.id.btn_register)

        registerButton.setOnClickListener {
            val name = nameInput.text?.toString()?.trim() ?: ""
            val surname = surnameInput.text?.toString()?.trim() ?: ""
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.register(name, surname, email, password)
        }

        // Simple Google SSO setup
        setupGoogleSignIn(view)

        // Navigation
        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_back_login).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.txt_btn_back_login).setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun setupGoogleSignIn(view: View) {
        try {
            // Simple Google Sign-In configuration
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            view.findViewById<ImageButton>(R.id.btn_google_register).setOnClickListener {
                Log.d("RegisterFragment", "Starting Google Sign-In...")
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Google Sign-In setup failed: ${e.message}")
            Toast.makeText(requireContext(), "Google Sign-In not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("RegisterFragment", "Google Sign-In successful: ${account.email}")
                
                if (account.idToken != null) {
                    authViewModel.registerWithGoogle(account.idToken!!)
                } else {
                    Log.e("RegisterFragment", "No ID token received")
                    Toast.makeText(requireContext(), "Google authentication setup incomplete", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("RegisterFragment", "Google Sign-In failed: ${e.statusCode}")
                when (e.statusCode) {
                    10 -> Toast.makeText(requireContext(), "Google configuration error. Please check Firebase setup.", Toast.LENGTH_LONG).show()
                    7 -> Toast.makeText(requireContext(), "Network error. Check internet connection.", Toast.LENGTH_LONG).show()
                    12501 -> { /* User cancelled - don't show error */ }
                    else -> Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}