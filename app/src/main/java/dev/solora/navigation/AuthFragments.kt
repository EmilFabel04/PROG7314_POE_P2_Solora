package dev.solora.navigation

// Force Android Studio sync - Google SSO enabled
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
// Google Play Services imports
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
    
    // Modern Activity Result API for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            Log.d("LoginFragment", "Google login successful, account: ${account.email}")
            
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                throw Exception("ID token is null or empty. Make sure SHA-1 fingerprint is configured in Firebase Console.")
            }
            
            Log.d("LoginFragment", "ID token received successfully")
            authViewModel.loginWithGoogle(idToken)
        } catch (e: ApiException) {
            Log.e("LoginFragment", "Google login failed with code: ${e.statusCode}, message: ${e.message}")
            Toast.makeText(requireContext(), "Google login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("LoginFragment", "Unexpected Google login error: ${e.message}")
            Toast.makeText(requireContext(), "Google login error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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

        // Google login setup
        try {
            val webClientId = getString(R.string.default_web_client_id)
            Log.d("LoginFragment", "Using web client ID: $webClientId")
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            Log.d("LoginFragment", "Google Sign-In client initialized successfully")

            view.findViewById<ImageButton>(R.id.btn_google_login).setOnClickListener {
                Log.d("LoginFragment", "Google login button clicked")
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Failed to initialize Google Sign-In: ${e.message}")
            Toast.makeText(requireContext(), "Google Sign-In setup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_to_register).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        view.findViewById<View>(R.id.txt_btn_sign_up).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

}

class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    // Modern Activity Result API for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            Log.d("RegisterFragment", "Google register successful, account: ${account.email}")
            
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                throw Exception("ID token is null or empty. Make sure SHA-1 fingerprint is configured in Firebase Console.")
            }
            
            Log.d("RegisterFragment", "ID token received successfully")
            authViewModel.registerWithGoogle(idToken)
        } catch (e: ApiException) {
            Log.e("RegisterFragment", "Google register failed with code: ${e.statusCode}, message: ${e.message}")
            Toast.makeText(requireContext(), "Google register failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Unexpected Google register error: ${e.message}")
            Toast.makeText(requireContext(), "Google register error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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

        // Google Sign-In setup
        try {
            val webClientId = getString(R.string.default_web_client_id)
            Log.d("RegisterFragment", "Using web client ID: $webClientId")
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            Log.d("RegisterFragment", "Google Sign-In client initialized successfully")

            val googleButton = view.findViewById<ImageButton>(R.id.btn_google_register)
            googleButton.setOnClickListener {
                Log.d("RegisterFragment", "Google register button clicked")
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Failed to initialize Google Sign-In: ${e.message}")
            Toast.makeText(requireContext(), "Google Sign-In setup failed: ${e.message}", Toast.LENGTH_LONG).show()
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

}
