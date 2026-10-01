package com.example.security;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * BiometricAuthManager provides biometric authentication services integrating
 * Android BiometricPrompt to securely authenticate the user before unlocking the vault.
 */
public class BiometricAuthManager {

    public enum BiometricStatus {
        READY,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED
    }

    public interface BiometricAuthCallback {
        void onAuthenticationSuccess(@NonNull BiometricPrompt.AuthenticationResult result);
        void onAuthenticationError(int errorCode, @NonNull String errorMessage);
        void onAuthenticationFailed();
        void onUserCancelled();
    }

    private static volatile BiometricAuthManager sInstance;
    private BiometricPrompt mCurrentPrompt;

    private BiometricAuthManager() {
    }

    public static BiometricAuthManager getInstance() {
        if (sInstance == null) {
            synchronized (BiometricAuthManager.class) {
                if (sInstance == null) {
                    sInstance = new BiometricAuthManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Checks detailed biometric readiness on the device.
     */
    @NonNull
    public BiometricStatus checkBiometricStatus(@NonNull Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return BiometricStatus.READY;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return BiometricStatus.NO_HARDWARE;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return BiometricStatus.HARDWARE_UNAVAILABLE;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return BiometricStatus.NONE_ENROLLED;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                return BiometricStatus.SECURITY_UPDATE_REQUIRED;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            default:
                return BiometricStatus.UNSUPPORTED;
        }
    }

    /**
     * Simple boolean check for whether biometric unlock is available and enrolled.
     */
    public boolean isBiometricAvailable(@NonNull Context context) {
        return checkBiometricStatus(context) == BiometricStatus.READY;
    }

    /**
     * Triggers the Android BiometricPrompt dialog to authenticate the user before unlocking the vault.
     *
     * @param activity           FragmentActivity context hosting the prompt
     * @param title              Title shown in prompt dialog
     * @param subtitle           Subtitle or guidance text
     * @param negativeButtonText Fallback button text (e.g., "Use Master PIN")
     * @param callback           Result callback listener
     */
    public void authenticate(
            @NonNull FragmentActivity activity,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        authenticateInternal(activity, null, title, subtitle, negativeButtonText, callback);
    }

    /**
     * Triggers biometric authentication tied to a cryptographic Cipher object (e.g. from KeyStore).
     */
    public void authenticateWithCrypto(
            @NonNull FragmentActivity activity,
            @NonNull BiometricPrompt.CryptoObject cryptoObject,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        authenticateInternal(activity, cryptoObject, title, subtitle, negativeButtonText, callback);
    }

    private void authenticateInternal(
            @NonNull FragmentActivity activity,
            @Nullable BiometricPrompt.CryptoObject cryptoObject,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt.AuthenticationCallback internalCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onAuthenticationSuccess(result);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    callback.onUserCancelled();
                } else {
                    callback.onAuthenticationError(errorCode, errString.toString());
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onAuthenticationFailed();
            }
        };

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(false)
                .build();

        mCurrentPrompt = new BiometricPrompt(activity, executor, internalCallback);

        if (cryptoObject != null) {
            mCurrentPrompt.authenticate(promptInfo, cryptoObject);
        } else {
            mCurrentPrompt.authenticate(promptInfo);
        }
    }

    /**
     * Cancels any active authentication prompt.
     */
    public void cancelAuthentication() {
        if (mCurrentPrompt != null) {
            try {
                mCurrentPrompt.cancelAuthentication();
            } catch (Exception ignored) {
            }
            mCurrentPrompt = null;
        }
    }
}
