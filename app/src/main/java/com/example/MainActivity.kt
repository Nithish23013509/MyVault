package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricAuthManager
import com.example.ui.VaultViewModel
import com.example.ui.screens.LockScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.VaultHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultBgDark

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = VaultBgDark
                ) {
                    val isLoginPassed by viewModel.isLoginPassed.collectAsStateWithLifecycle()
                    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
                    val authError by viewModel.authError.collectAsStateWithLifecycle()
                    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

                    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
                    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
                    val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsStateWithLifecycle()
                    val configuredPinLength by viewModel.configuredPinLength.collectAsStateWithLifecycle()
                    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

                    // Request runtime notification permission on Android 13+ (API 33+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { /* notification permission result handled */ }

                        LaunchedEffect(Unit) {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Auto prompt biometrics on launch if available and already setup
                    LaunchedEffect(isLoginPassed, isSetupCompleted, isUnlocked) {
                        if (isLoginPassed && isSetupCompleted && !isUnlocked && isBiometricAvailable) {
                            promptBiometric()
                        }
                    }

                    if (!isLoginPassed) {
                        LoginScreen(
                            isLoading = isAuthLoading,
                            statusMessage = cloudSyncStatus,
                            authError = authError,
                            onGoogleSignIn = { activity ->
                                viewModel.signInWithGoogle(activity)
                            },
                            onEmailSignIn = { email, pass ->
                                viewModel.signInWithEmail(email, pass)
                            },
                            onEmailSignUp = { email, pass ->
                                viewModel.signUpWithEmail(email, pass)
                            },
                            onContinueLocal = {
                                viewModel.continueLocalMode()
                            }
                        )
                    } else if (!isUnlocked) {
                        LockScreen(
                            isSetupMode = !isSetupCompleted,
                            isBiometricAvailable = isBiometricAvailable,
                            configuredPinLength = configuredPinLength,
                            onPinEntered = { pin ->
                                viewModel.unlockWithPin(pin)
                            },
                            onSetupPin = { pin ->
                                viewModel.setupMasterPin(pin)
                            },
                            onTriggerBiometric = {
                                promptBiometric()
                            },
                            onQuickUnlock = {
                                viewModel.quickDemoUnlock()
                            },
                            errorMessage = statusMessage
                        )
                    } else {
                        VaultHomeScreen(
                            viewModel = viewModel,
                            onLockRequested = {
                                viewModel.lockVault()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    private fun promptBiometric() {
        BiometricAuthManager.getInstance().authenticate(
            this,
            "MyVault Protection",
            "Verify fingerprint or face to access your protected vault",
            "Use Master PIN",
            object : BiometricAuthManager.BiometricAuthCallback {
                override fun onAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                    // Handled gracefully; user can still use PIN
                }

                override fun onAuthenticationFailed() {
                    // User can retry or use PIN
                }

                override fun onUserCancelled() {
                    // User canceled or selected PIN option
                }
            }
        )
    }
}
