package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Firebase Authentication with Google Sign-in via Android Credential Manager.
 */
class FirebaseAuthManager private constructor(private val appContext: Context) {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    init {
        initFirebase(appContext)
    }

    private fun initFirebase(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            _currentUser.value = firebaseAuth?.currentUser

            firebaseAuth?.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization warning: ${e.localizedMessage}")
        }
    }

    fun isUserSignedIn(): Boolean = _currentUser.value != null

    /**
     * Initiates Google Sign-In using Credential Manager and authenticates with Firebase Auth.
     */
    suspend fun signInWithGoogle(
        activity: Activity,
        serverClientId: String = GOOGLE_WEB_CLIENT_ID
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val auth = firebaseAuth ?: FirebaseAuth.getInstance()
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                _currentUser.value = user
                _isLoading.value = false
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Firebase user is null after sign-in"))
                }
            } else {
                _isLoading.value = false
                val err = "Unexpected credential type received"
                _authError.value = err
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            _isLoading.value = false
            Log.i(TAG, "Google Sign-In canceled by user")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            _isLoading.value = false
            val msg = "No Google account configured on device/emulator"
            _authError.value = msg
            Log.i(TAG, "$msg: ${e.localizedMessage}")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            _isLoading.value = false
            val msg = "Google Sign-In: ${e.localizedMessage}"
            _authError.value = msg
            Log.w(TAG, "$msg: ${e.localizedMessage}")
            Result.failure(e)
        } catch (e: Exception) {
            _isLoading.value = false
            val msg = "Authentication failed: ${e.localizedMessage}"
            _authError.value = msg
            Log.e(TAG, msg, e)
            Result.failure(e)
        }
    }

    /**
     * Signs in using email and password with Firebase Auth.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null
        try {
            val auth = firebaseAuth ?: FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            _currentUser.value = user
            _isLoading.value = false
            if (user != null) Result.success(user) else Result.failure(Exception("User is null after sign in"))
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.localizedMessage
            Result.failure(e)
        }
    }

    /**
     * Creates a new Firebase Auth account using email and password.
     */
    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null
        try {
            val auth = firebaseAuth ?: FirebaseAuth.getInstance()
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            _currentUser.value = user
            _isLoading.value = false
            if (user != null) Result.success(user) else Result.failure(Exception("User is null after account creation"))
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.localizedMessage
            Result.failure(e)
        }
    }

    /**
     * Signs in anonymously so automatic cloud sync works immediately out of the box.
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null
        try {
            val auth = firebaseAuth ?: FirebaseAuth.getInstance()
            val result = auth.signInAnonymously().await()
            val user = result.user
            _currentUser.value = user
            _isLoading.value = false
            if (user != null) Result.success(user) else Result.failure(Exception("Anonymous user is null"))
        } catch (e: Exception) {
            _isLoading.value = false
            Log.w(TAG, "Anonymous sign-in note: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Signs out of Firebase and clears local credential state.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
            _currentUser.value = null
            _authError.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out: ${e.localizedMessage}")
        }
    }

    fun clearError() {
        _authError.value = null
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
        const val GOOGLE_WEB_CLIENT_ID = "518363031734-hto1tgjrj1ceus11bme8agsf94sb2bdd.apps.googleusercontent.com"

        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
