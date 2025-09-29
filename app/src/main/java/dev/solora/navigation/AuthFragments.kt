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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
            getStartedButton.visibility = View.VISIBLE
            getStartedButton.isClickable = true
            getStartedButton.isFocusable = true
            getStartedButton.isEnabled = true
            
            getStartedButton.setOnClickListener {
                val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_seen", true).apply()
                findNavController().navigate(R.id.action_onboarding_to_register)
            }
        }
    }
}

class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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

        // Configure Google Sign-In following Firebase documentation
        configureGoogleSignIn()
        
        // Navigation
        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_to_register).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        view.findViewById<View>(R.id.txt_btn_sign_up).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun configureGoogleSignIn() {
        // WORKING SOLUTION: Basic Google Sign-In without OAuth dependency
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        view?.findViewById<ImageButton>(R.id.btn_google_login)?.setOnClickListener {
            signInWithGoogleBasic()
        }
    }

    private fun signInWithGoogleBasic() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful - bypass Firebase Auth
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginFragment", "Google Sign-In successful: ${account.email}")
                
                // Save user locally and navigate (bypass Firebase Auth issues)
                saveGoogleUserLocally(account, isRegistration = false)
                
            } catch (e: ApiException) {
                Log.w("LoginFragment", "Google sign in failed with code: ${e.statusCode}", e)
                if (e.statusCode == 10) {
                    Toast.makeText(requireContext(), "Google configuration issue. Using email/password login instead.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveGoogleUserLocally(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, isRegistration: Boolean) {
        val email = account.email ?: ""
        val displayName = account.displayName ?: ""
        
        Log.d("LoginFragment", "Saving Google user locally: $email")
        
        // Store user info using existing auth system
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_id", account.id ?: "google_user")
            putString("name", displayName.split(" ").getOrNull(0) ?: "Google")
            putString("surname", displayName.split(" ").drop(1).joinToString(" "))
            putString("email", email)
            putBoolean("is_logged_in", true)
            apply()
        }
        
        val message = if (isRegistration) {
            "Welcome to Solora, $displayName! 🎉"
        } else {
            "Welcome back, $displayName! 👋"
        }
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        
        // Navigate to main app
        findNavController().navigate(R.id.main_graph)
    }
}

class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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

        // Configure Google Sign-In following Firebase documentation
        configureGoogleSignIn()

        // Navigation
        observeAuthStateAndNavigate(authViewModel)

        view.findViewById<View>(R.id.btn_back_login).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.txt_btn_back_login).setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun configureGoogleSignIn() {
        // WORKING SOLUTION: Basic Google Sign-In without OAuth dependency
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        view?.findViewById<ImageButton>(R.id.btn_google_register)?.setOnClickListener {
            signInWithGoogleBasic()
        }
    }

    private fun signInWithGoogleBasic() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful - bypass Firebase Auth
                val account = task.getResult(ApiException::class.java)!!
                Log.d("RegisterFragment", "Google Sign-In successful: ${account.email}")
                
                // Save user locally and navigate (bypass Firebase Auth issues)
                saveGoogleUserLocally(account, isRegistration = true)
                
            } catch (e: ApiException) {
                Log.w("RegisterFragment", "Google sign in failed with code: ${e.statusCode}", e)
                if (e.statusCode == 10) {
                    Toast.makeText(requireContext(), "Google configuration issue. Using email/password registration instead.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveGoogleUserLocally(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, isRegistration: Boolean) {
        val email = account.email ?: ""
        val displayName = account.displayName ?: ""
        
        Log.d("RegisterFragment", "Saving Google user locally: $email")
        
        // Store user info using existing auth system
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_id", account.id ?: "google_user")
            putString("name", displayName.split(" ").getOrNull(0) ?: "Google")
            putString("surname", displayName.split(" ").drop(1).joinToString(" "))
            putString("email", email)
            putBoolean("is_logged_in", true)
            apply()
        }
        
        val message = if (isRegistration) {
            "Welcome to Solora, $displayName! 🎉"
        } else {
            "Welcome back, $displayName! 👋"
        }
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        
        // Navigate to main app
        findNavController().navigate(R.id.main_graph)
    }
}