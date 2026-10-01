package com.example.security

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricAuthManager.getInstance().isBiometricAvailable(context)
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "CipherVault Unlock",
        subtitle: String = "Confirm biometric credential to decrypt vault",
        negativeButtonText: String = "Use Master PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BiometricAuthManager.getInstance().authenticate(
            activity,
            title,
            subtitle,
            negativeButtonText,
            object : BiometricAuthManager.BiometricAuthCallback {
                override fun onAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                    onError(errorMessage)
                }

                override fun onAuthenticationFailed() {
                    onError("Biometric authentication failed. Try again.")
                }

                override fun onUserCancelled() {
                    // Handled gracefully, user opted for PIN or dismissed
                }
            }
        )
    }
}
