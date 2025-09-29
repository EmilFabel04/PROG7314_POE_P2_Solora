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

        android.util.Log.d("OnboardingFragment", "=== ZIMKITHA ONBOARDING FRAGMENT VIEW CREATED ===")
        
        // Find and configure the Get Started button
        val getStartedButton = view.findViewById<View>(R.id.btn_get_started)
        android.util.Log.d("OnboardingFragment", "Get Started button found: ${getStartedButton != null}")
        
        if (getStartedButton != null) {
            android.util.Log.d("OnboardingFragment", "Button visibility: ${getStartedButton.visibility}, enabled: ${getStartedButton.isEnabled}")
            android.util.Log.d("OnboardingFragment", "Button clickable: ${getStartedButton.isClickable}, focusable: ${getStartedButton.isFocusable}")
            
            // Ensure button is visible and clickable
            getStartedButton.visibility = android.view.View.VISIBLE
            getStartedButton.isClickable = true
            getStartedButton.isFocusable = true
            getStartedButton.isEnabled = true
            
            getStartedButton.setOnClickListener {
                android.util.Log.d("OnboardingFragment", "Get Started button clicked! Navigating to register...")

                // saving onboarding flag so that this screen is now shown again
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_seen", true).apply()

                // navigating to the register screen
                try {
                    findNavController().navigate(R.id.action_onboarding_to_register)
                    android.util.Log.d("OnboardingFragment", "Navigation to register successful!")
                } catch (e: Exception) {
                    android.util.Log.e("OnboardingFragment", "Navigation failed: ${e.message}")
                }
            }
        } else {
            android.util.Log.e("OnboardingFragment", "Get Started button NOT FOUND!")
        }
        
        android.util.Log.d("OnboardingFragment", "=== ONBOARDING SETUP COMPLETE ===")
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

        val emailInput = view.findViewById<android.widget.EditText>(R.id.et_email)
        val passwordInput = view.findViewById<android.widget.EditText>(R.id.et_password)
        val submitButton = view.findViewById<LinearLayout>(R.id.btn_login)

        submitButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Please fill in all fields",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            authViewModel.login(email, password)
        }

        // Google login setup with fallback client ID
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("LoginFragment", "Web client ID not found, trying Android client ID")
            getString(R.string.android_client_id)
        }
        
        Log.d("LoginFragment", "=== GOOGLE SIGN-IN SETUP ===")
        Log.d("LoginFragment", "Using Client ID: $webClientId")
        Log.d("LoginFragment", "Package: ${requireContext().packageName}")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        view.findViewById<ImageButton>(R.id.btn_google_login).setOnClickListener {
            Log.d("LoginFragment", "=== GOOGLE LOGIN BUTTON CLICKED ===")
            Log.d("LoginFragment", "Package name: ${requireContext().packageName}")
            Log.d("LoginFragment", "Google Services available: ${GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(requireContext()), gso)}")
            
            try {
                val signInIntent = googleSignInClient.signInIntent
                Log.d("LoginFragment", "Sign-in intent created successfully")
                startActivityForResult(signInIntent, RC_SIGN_IN)
            } catch (e: Exception) {
                Log.e("LoginFragment", "Failed to create sign-in intent: ${e.message}")
                Toast.makeText(requireContext(), "Google Sign-In setup error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_to_register).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        view.findViewById<View>(R.id.txt_btn_sign_up).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("LoginFragment", "=== onActivityResult called ===")
        Log.d("LoginFragment", "requestCode: $requestCode, resultCode: $resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                Log.d("LoginFragment", "Attempting to get Google account from intent...")
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginFragment", "Google account retrieved successfully")
                Log.d("LoginFragment", "Account email: ${account.email}")
                Log.d("LoginFragment", "Account display name: ${account.displayName}")
                Log.d("LoginFragment", "ID token available: ${account.idToken != null}")
                
                if (account.idToken != null) {
                    Log.d("LoginFragment", "Calling authViewModel.loginWithGoogle...")
                    authViewModel.loginWithGoogle(account.idToken!!)
                } else {
                    Log.e("LoginFragment", "ID token is null - this indicates SHA-1 or OAuth configuration issue")
                    Toast.makeText(requireContext(), "Google authentication failed: ID token not available", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("LoginFragment", "ApiException in onActivityResult: code=${e.statusCode}, message=${e.message}")
                Toast.makeText(requireContext(), "Google login failed: Code ${e.statusCode} - ${e.message}", Toast.LENGTH_LONG).show()
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
                android.widget.Toast.makeText(
                    requireContext(),
                    "Please fill in all fields",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Password must be at least 6 characters",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            authViewModel.register(name, surname, email, password)
        }

        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("RegisterFragment", "Web client ID not found, trying Android client ID")
            getString(R.string.android_client_id)
        }
        
        Log.d("RegisterFragment", "=== GOOGLE SIGN-IN SETUP ===")
        Log.d("RegisterFragment", "Using Client ID: $webClientId")
        Log.d("RegisterFragment", "Package: ${requireContext().packageName}")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val googleButton = view.findViewById<ImageButton>(R.id.btn_google_register)
        googleButton.setOnClickListener {
            Log.d("RegisterFragment", "=== GOOGLE REGISTER BUTTON CLICKED ===")
            Log.d("RegisterFragment", "Package name: ${requireContext().packageName}")
            Log.d("RegisterFragment", "Google Services available: ${GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(requireContext()), gso)}")
            
            try {
                val signInIntent = googleSignInClient.signInIntent
                Log.d("RegisterFragment", "Sign-in intent created successfully")
                startActivityForResult(signInIntent, RC_SIGN_IN)
            } catch (e: Exception) {
                Log.e("RegisterFragment", "Failed to create sign-in intent: ${e.message}")
                Toast.makeText(requireContext(), "Google Sign-In setup error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_back_login).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.txt_btn_back_login).setOnClickListener {
            Log.d("RegisterFragment", "Back to login clicked!")
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("RegisterFragment", "=== onActivityResult called ===")
        Log.d("RegisterFragment", "requestCode: $requestCode, resultCode: $resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                Log.d("RegisterFragment", "Attempting to get Google account from intent...")
                val account = task.getResult(ApiException::class.java)!!
                Log.d("RegisterFragment", "Google account retrieved successfully")
                Log.d("RegisterFragment", "Account email: ${account.email}")
                Log.d("RegisterFragment", "Account display name: ${account.displayName}")
                Log.d("RegisterFragment", "ID token available: ${account.idToken != null}")
                
                if (account.idToken != null) {
                    Log.d("RegisterFragment", "Calling authViewModel.registerWithGoogle...")
                    authViewModel.registerWithGoogle(account.idToken!!)
                } else {
                    Log.e("RegisterFragment", "ID token is null - this indicates SHA-1 or OAuth configuration issue")
                    Toast.makeText(requireContext(), "Google authentication failed: ID token not available", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("RegisterFragment", "ApiException in onActivityResult: code=${e.statusCode}, message=${e.message}")
                Toast.makeText(requireContext(), "Google register failed: Code ${e.statusCode} - ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}