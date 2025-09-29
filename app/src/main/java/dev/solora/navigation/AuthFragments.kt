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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var credentialManager: CredentialManager

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

        // Configure Google Sign-In following Firebase documentation (Credential Manager)
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
        credentialManager = CredentialManager.create(requireContext())
        val webClientId = getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        view?.findViewById<ImageButton>(R.id.btn_google_login)?.setOnClickListener {
            viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {})
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                try {
                    val result = credentialManager.getCredential(requireContext(), request)
                    handleCredential(result.credential)
                } catch (e: GetCredentialException) {
                    Log.e("LoginFragment", "Credential retrieval failed: ${e.message}")
                    Toast.makeText(requireContext(), "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleCredential(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            authViewModel.authenticateWithGoogle(idToken, isRegistration = false)
        } else {
            Log.w("LoginFragment", "Credential is not Google ID token")
            Toast.makeText(requireContext(), "Not a Google ID credential", Toast.LENGTH_SHORT).show()
        }
    }
}

class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

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

        // Configure Google Sign-In following Firebase documentation (Credential Manager)
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
        credentialManager = CredentialManager.create(requireContext())
        val webClientId = getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        view?.findViewById<ImageButton>(R.id.btn_google_register)?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                try {
                    val result = credentialManager.getCredential(requireContext(), request)
                    handleCredential(result.credential)
                } catch (e: GetCredentialException) {
                    Log.e("RegisterFragment", "Credential retrieval failed: ${e.message}")
                    Toast.makeText(requireContext(), "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleCredential(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            authViewModel.authenticateWithGoogle(idToken, isRegistration = true)
        } else {
            Log.w("RegisterFragment", "Credential is not Google ID token")
            Toast.makeText(requireContext(), "Not a Google ID credential", Toast.LENGTH_SHORT).show()
        }
    }
}