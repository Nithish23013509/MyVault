package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.crypto.PBKDF2KeyDerivation

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ciphervault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_CLIPBOARD_TIMEOUT = "clipboard_timeout"
        private const val KEY_HAS_SETUP = "has_completed_setup"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_REVEAL_AUTH_REQUIRED = "reveal_auth_required"
        private const val KEY_SENSITIVE_DATA_MASKED = "sensitive_data_masked"
        private const val KEY_CUSTOM_COLLECTIONS = "custom_collections"
        private const val KEY_AUTO_CLOUD_SYNC = "auto_cloud_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_cloud_sync_time"
        private const val KEY_ENCRYPTED_PIN = "encrypted_master_pin"
    }

    var autoCloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CLOUD_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CLOUD_SYNC, value).apply()

    var lastCloudSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var revealAuthRequired: Boolean
        get() = prefs.getBoolean(KEY_REVEAL_AUTH_REQUIRED, true)
        set(value) = prefs.edit().putBoolean(KEY_REVEAL_AUTH_REQUIRED, value).apply()

    var sensitiveDataMasked: Boolean
        get() = prefs.getBoolean(KEY_SENSITIVE_DATA_MASKED, true)
        set(value) = prefs.edit().putBoolean(KEY_SENSITIVE_DATA_MASKED, value).apply()

    var customCollectionsJson: String
        get() = prefs.getString(KEY_CUSTOM_COLLECTIONS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_COLLECTIONS, value).apply()

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_HAS_SETUP, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SETUP, value).apply()

    var pinLength: Int
        get() = prefs.getInt(KEY_PIN_LENGTH, 4)
        set(value) = prefs.edit().putInt(KEY_PIN_LENGTH, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    /**
     * Auto lock timeout in seconds (0 = immediate upon exit, 30 = 30s, 60 = 1min, 300 = 5min, -1 = never)
     * Default: 30 seconds
     */
    var autoLockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    /**
     * Clipboard clear timeout in seconds (15, 30, 60, or 0 for never)
     * Default: 30 seconds
     */
    var clipboardClearTimeoutSeconds: Int
        get() = prefs.getInt(KEY_CLIPBOARD_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_CLIPBOARD_TIMEOUT, value).apply()

    fun setMasterPin(pin: String) {
        val salt = PBKDF2KeyDerivation.generateSalt()
        val hash = PBKDF2KeyDerivation.hashCredential(pin, salt)
        val encryptedPin = try {
            com.example.crypto.AESEncryptionManager.getInstance().encrypt(pin)
        } catch (e: Exception) {
            null
        }
        val editor = prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putBoolean(KEY_HAS_SETUP, true)
        if (encryptedPin != null) {
            editor.putString(KEY_ENCRYPTED_PIN, encryptedPin)
        }
        editor.apply()
    }

    fun getMasterPinDecrypted(): String? {
        val enc = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return null
        return try {
            com.example.crypto.AESEncryptionManager.getInstance().decrypt(enc)
        } catch (e: Exception) {
            null
        }
    }

    fun verifyMasterPin(pin: String): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        return PBKDF2KeyDerivation.verifyCredential(pin, storedHash, salt)
    }

    fun resetSecurity() {
        prefs.edit().clear().apply()
    }
}
