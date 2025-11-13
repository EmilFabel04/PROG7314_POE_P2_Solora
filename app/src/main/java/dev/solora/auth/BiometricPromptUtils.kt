package dev.solora.auth

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.solora.R

object BiometricPromptUtils {

    fun createBiometricPrompt(
        activity: FragmentActivity,
        processSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                processSuccess(result)
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }
    fun createPromptInfo(activity: FragmentActivity): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(activity.getString(R.string.biometric_prompt_title))
            setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            setDescription(activity.getString(R.string.biometric_prompt_description))
            setConfirmationRequired(false)
            setNegativeButtonText(activity.getString(R.string.biometric_prompt_negative_text))
            
        }.build()
    }
}
