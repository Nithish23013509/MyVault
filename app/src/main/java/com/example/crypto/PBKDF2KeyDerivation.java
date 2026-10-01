package com.example.crypto;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Key derivation and verification using PBKDF2WithHmacSHA256 with 100,000 iterations.
 */
public final class PBKDF2KeyDerivation {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 150_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PBKDF2KeyDerivation() {
        // Utility class
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Derives a 256-bit AES SecretKey from a passphrase and salt.
     */
    public static SecretKey deriveKey(char[] passphrase, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Hashes a PIN or Password with a salt for secure local credential verification.
     */
    public static String hashCredential(String credential, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(credential.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    /**
     * Constant-time verification of credential hash to prevent timing attacks.
     */
    public static boolean verifyCredential(String inputCredential, String storedHashBase64, byte[] salt) {
        try {
            String computedHash = hashCredential(inputCredential, salt);
            byte[] a = Base64.decode(computedHash, Base64.NO_WRAP);
            byte[] b = Base64.decode(storedHashBase64, Base64.NO_WRAP);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            return false;
        }
    }
}
