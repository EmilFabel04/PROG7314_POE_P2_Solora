package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.solora.R
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
        view.findViewById<View>(R.id.btn_submit).setOnClickListener {
            // Simplified email/password extraction omitted
            authViewModel.login("test@solora.co.za", "password")
            findNavController().navigate(R.id.action_login_to_main)
        }
        view.findViewById<View>(R.id.btn_to_register).setOnClickListener { findNavController().navigate(R.id.action_login_to_register) }
    }
}

class RegisterFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_register).setOnClickListener {
            authViewModel.register("User", "user@solora.co.za", "password")
            findNavController().navigate(R.id.action_register_to_main)
        }
        view.findViewById<View>(R.id.btn_back_login).setOnClickListener { findNavController().popBackStack() }
    }
}


