package com.example.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Robust Java cryptographic engine utilizing AES-256 in GCM mode (Galois/Counter Mode).
 * Provides authenticated encryption ensuring both confidentiality and data integrity.
 */
public final class AESEncryptionEngine {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // Standard 96-bit IV for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // Standard 128-bit authentication tag

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AESEncryptionEngine() {
        // Utility class
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Output format: Base64(IV [12 bytes] + Ciphertext + Tag [16 bytes]).
     */
    public static String encrypt(String plaintext, SecretKey secretKey) throws Exception {
        if (plaintext == null) {
            return null;
        }

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        byte[] iv;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            iv = cipher.getIV();
        } catch (Exception e) {
            iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        }

        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] cipherTextWithTag = cipher.doFinal(plaintextBytes);

        // Prepend IV to ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherTextWithTag.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherTextWithTag);

        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
    }

    /**
     * Decrypts Base64 payload (IV + Ciphertext + Tag) using AES-256-GCM.
     */
    public static String decrypt(String encryptedBase64, SecretKey secretKey) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return "";
        }

        byte[] decodedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP);
        if (decodedBytes.length < GCM_IV_LENGTH_BYTES + 16) {
            throw new IllegalArgumentException("Invalid encrypted payload length.");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byteBuffer.get(iv);

        byte[] cipherTextWithTag = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherTextWithTag);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plainBytes = cipher.doFinal(cipherTextWithTag);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * Creates a SecretKeySpec from raw 256-bit key bytes.
     */
    public static SecretKey createKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }
}
