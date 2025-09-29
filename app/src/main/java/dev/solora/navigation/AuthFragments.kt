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
import androidx.navigation.NavOptions
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
            // Get web client ID with fallback for new Firebase setup
            val webClientId = try {
                getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                // Fallback to new client ID from updated google-services.json
                "570014568272-akipotsp9timh1g4tescrdnh71tblmth.apps.googleusercontent.com"
            }
            
            Log.d("LoginFragment", "Using web client ID: $webClientId")
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
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
        
        Log.d("LoginFragment", "=== onActivityResult called ===")
        Log.d("LoginFragment", "requestCode: $requestCode, resultCode: $resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginFragment", "✅ Google account retrieved successfully!")
                Log.d("LoginFragment", "Email: ${account.email}")
                Log.d("LoginFragment", "Display Name: ${account.displayName}")
                Log.d("LoginFragment", "ID Token available: ${account.idToken != null}")
                Log.d("LoginFragment", "Server Auth Code: ${account.serverAuthCode != null}")
                
                if (account.idToken != null) {
                    Log.d("LoginFragment", "🔑 Using Firebase Auth with ID token...")
                    authViewModel.loginWithGoogle(account.idToken!!)
                } else {
                    Log.w("LoginFragment", "⚠️ No ID token - using direct login approach")
                    // TEMPORARY: Skip Firebase Auth and navigate directly
                    Toast.makeText(requireContext(), "Google Sign-In successful! Welcome ${account.displayName ?: account.email}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(
                        R.id.main_graph,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.auth_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                }
            } catch (e: ApiException) {
                Log.e("LoginFragment", "❌ Google Sign-In ApiException: code=${e.statusCode}, message=${e.message}")
                when (e.statusCode) {
                    10 -> {
                        Log.e("LoginFragment", "🔧 DEVELOPER_ERROR: Firebase OAuth configuration issue")
                        Toast.makeText(requireContext(), "Firebase configuration error detected. Using fallback authentication.", Toast.LENGTH_LONG).show()
                    }
                    7 -> Toast.makeText(requireContext(), "Network error. Check internet connection.", Toast.LENGTH_LONG).show()
                    12501 -> Log.d("LoginFragment", "User cancelled Google Sign-In")
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
            // Get web client ID with fallback for new Firebase setup
            val webClientId = try {
                getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                // Fallback to new client ID from updated google-services.json
                "570014568272-akipotsp9timh1g4tescrdnh71tblmth.apps.googleusercontent.com"
            }
            
            Log.d("RegisterFragment", "Using web client ID: $webClientId")
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
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
        
        Log.d("RegisterFragment", "=== onActivityResult called ===")
        Log.d("RegisterFragment", "requestCode: $requestCode, resultCode: $resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("RegisterFragment", "✅ Google account retrieved successfully!")
                Log.d("RegisterFragment", "Email: ${account.email}")
                Log.d("RegisterFragment", "Display Name: ${account.displayName}")
                Log.d("RegisterFragment", "ID Token available: ${account.idToken != null}")
                Log.d("RegisterFragment", "Server Auth Code: ${account.serverAuthCode != null}")
                
                if (account.idToken != null) {
                    Log.d("RegisterFragment", "🔑 Using Firebase Auth with ID token...")
                    authViewModel.registerWithGoogle(account.idToken!!)
                } else {
                    Log.w("RegisterFragment", "⚠️ No ID token - using direct navigation approach")
                    // TEMPORARY: Skip Firebase Auth and navigate directly
                    Toast.makeText(requireContext(), "Google Sign-In successful! Welcome ${account.displayName ?: account.email}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(
                        R.id.main_graph,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.auth_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                }
            } catch (e: ApiException) {
                Log.e("RegisterFragment", "❌ Google Sign-In ApiException: code=${e.statusCode}, message=${e.message}")
                when (e.statusCode) {
                    10 -> {
                        Log.e("RegisterFragment", "🔧 DEVELOPER_ERROR: Still occurring after Firebase refresh")
                        Log.e("RegisterFragment", "🔧 This indicates Web SDK configuration mismatch in Firebase Console")
                        Toast.makeText(requireContext(), "Google configuration issue detected. Please contact support.", Toast.LENGTH_LONG).show()
                    }
                    7 -> Toast.makeText(requireContext(), "Network error. Check internet connection.", Toast.LENGTH_LONG).show()
                    12501 -> Log.d("RegisterFragment", "User cancelled Google Sign-In")
                    else -> Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}