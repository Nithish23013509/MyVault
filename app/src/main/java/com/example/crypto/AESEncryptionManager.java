package com.example.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AESEncryptionManager handles authenticated AES-256-GCM encryption and decryption
 * of sensitive strings using hardware-backed SecretKeys stored in the Android KeyStore.
 */
public class AESEncryptionManager {

    private static final String TAG = "AESEncryptionManager";
    private static final String ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String DEFAULT_KEY_ALIAS = "CipherVault_Keystore_AES_Key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag
    private static final int AES_KEY_SIZE_BITS = 256;

    private static volatile AESEncryptionManager sInstance;
    private final SecureRandom mSecureRandom;
    private final String mDefaultAlias;

    public AESEncryptionManager() {
        this(DEFAULT_KEY_ALIAS);
    }

    public AESEncryptionManager(@NonNull String defaultAlias) {
        this.mDefaultAlias = defaultAlias;
        this.mSecureRandom = new SecureRandom();
    }

    public static AESEncryptionManager getInstance() {
        if (sInstance == null) {
            synchronized (AESEncryptionManager.class) {
                if (sInstance == null) {
                    sInstance = new AESEncryptionManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Retrieves an existing SecretKey from the Android KeyStore, or creates a new
     * 256-bit AES key if one does not exist yet under the default alias.
     */
    @NonNull
    public SecretKey getOrCreateSecretKey() throws Exception {
        return getOrCreateSecretKey(mDefaultAlias);
    }

    /**
     * Retrieves or generates a SecretKey under the specified alias.
     */
    @NonNull
    public synchronized SecretKey getOrCreateSecretKey(@NonNull String keyAlias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (keyStore.containsAlias(keyAlias)) {
            KeyStore.Entry entry = keyStore.getEntry(keyAlias, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                if (secretKey != null) {
                    return secretKey;
                }
            }
        }

        // Generate a new hardware-backed AES-256 SecretKey in Android KeyStore
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER);

        KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(keyGenSpec);
        SecretKey generatedKey = keyGenerator.generateKey();
        Log.i(TAG, "Generated fresh AES-256 SecretKey in Android KeyStore under alias: " + keyAlias);
        return generatedKey;
    }

    /**
     * Encrypts a sensitive plaintext string using the default Android KeyStore SecretKey.
     *
     * @param plaintext Sensitive data to protect
     * @return Base64-encoded ciphertext payload prefixed with the 12-byte IV
     */
    @Nullable
    public String encrypt(@Nullable String plaintext) throws Exception {
        return encrypt(plaintext, mDefaultAlias);
    }

    /**
     * Encrypts a sensitive plaintext string using a SecretKey stored under a specific alias.
     */
    @Nullable
    public String encrypt(@Nullable String plaintext, @NonNull String keyAlias) throws Exception {
        if (plaintext == null) {
            return null;
        }
        SecretKey secretKey = getOrCreateSecretKey(keyAlias);
        return encrypt(plaintext, secretKey);
    }

    /**
     * Encrypts plaintext using the provided SecretKey with AES/GCM/NoPadding.
     */
    @Nullable
    public String encrypt(@Nullable String plaintext, @NonNull SecretKey secretKey) throws Exception {
        if (plaintext == null) {
            return null;
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            iv = cipher.getIV();
        } catch (Exception e) {
            iv = new byte[GCM_IV_LENGTH_BYTES];
            mSecureRandom.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        }

        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertextWithTag = cipher.doFinal(plaintextBytes);

        // Prepend IV to ciphertext (IV + Ciphertext + Tag)
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertextWithTag.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertextWithTag);

        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
    }

    /**
     * Decrypts an encrypted Base64 payload using the default Android KeyStore SecretKey.
     *
     * @param encryptedBase64 Base64 string produced by encrypt()
     * @return Decrypted plaintext string
     */
    @NonNull
    public String decrypt(@Nullable String encryptedBase64) throws Exception {
        return decrypt(encryptedBase64, mDefaultAlias);
    }

    /**
     * Decrypts an encrypted Base64 payload using the SecretKey under the specified alias.
     */
    @NonNull
    public String decrypt(@Nullable String encryptedBase64, @NonNull String keyAlias) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return "";
        }
        SecretKey secretKey = getOrCreateSecretKey(keyAlias);
        return decrypt(encryptedBase64, secretKey);
    }

    /**
     * Decrypts an encrypted Base64 payload using the provided SecretKey.
     */
    @NonNull
    public String decrypt(@Nullable String encryptedBase64, @NonNull SecretKey secretKey) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return "";
        }

        byte[] combined = Base64.decode(encryptedBase64, Base64.NO_WRAP);
        if (combined.length < GCM_IV_LENGTH_BYTES + 16) {
            throw new IllegalArgumentException("Payload too short to contain valid IV and GCM authentication tag.");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byteBuffer.get(iv);

        byte[] ciphertextWithTag = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertextWithTag);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedBytes = cipher.doFinal(ciphertextWithTag);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Checks if a key alias exists in Android KeyStore.
     */
    public boolean hasSecretKey(@NonNull String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(keyAlias);
        } catch (Exception e) {
            Log.e(TAG, "Failed checking key alias in Android KeyStore", e);
            return false;
        }
    }

    /**
     * Deletes the SecretKey from Android KeyStore.
     */
    public boolean deleteSecretKey(@NonNull String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias);
                Log.i(TAG, "Deleted key alias from Android KeyStore: " + keyAlias);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed deleting key alias from Android KeyStore", e);
            return false;
        }
    }

    /**
     * Deletes the default SecretKey from Android KeyStore.
     */
    public boolean deleteDefaultSecretKey() {
        return deleteSecretKey(mDefaultAlias);
    }
}
