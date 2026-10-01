package com.example.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Manages master cryptographic keys in the hardware-backed AndroidKeyStore.
 */
public final class KeyStoreHelper {

    private static final String TAG = "KeyStoreHelper";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String MASTER_KEY_ALIAS = "CipherVault_Master_AES_Key";

    private KeyStoreHelper() {
        // Utility class
    }

    /**
     * Retrieves the existing AES-256 master key or generates a new one
     * backed by Android KeyStore.
     */
    public static synchronized SecretKey getOrCreateMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(MASTER_KEY_ALIAS, null);
            if (entry != null && entry.getSecretKey() != null) {
                return entry.getSecretKey();
            }
        }

        // Generate new AES-256 Key
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true);

        keyGenerator.init(builder.build());
        SecretKey secretKey = keyGenerator.generateKey();
        Log.i(TAG, "Generated fresh master key in AndroidKeyStore.");
        return secretKey;
    }

    /**
     * Checks if master key exists in AndroidKeyStore.
     */
    public static boolean hasMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            return keyStore.containsAlias(MASTER_KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking master key alias", e);
            return false;
        }
    }

    /**
     * Clears master key on factory vault reset.
     */
    public static void deleteMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS);
                Log.i(TAG, "Deleted master key from AndroidKeyStore.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting master key", e);
        }
    }
}
