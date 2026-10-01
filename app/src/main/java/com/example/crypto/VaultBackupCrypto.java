package com.example.crypto;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

/**
 * Handles portable end-to-end encrypted backup file export and import using
 * PBKDF2 passphrase key derivation and AES-256-GCM.
 */
public final class VaultBackupCrypto {

    private static final String BACKUP_HEADER = "CIPHERVAULT_V1";

    private VaultBackupCrypto() {
    }

    /**
     * Exports raw JSON string of items encrypted with user's backup passphrase.
     */
    public static String exportEncryptedBackup(String rawDataJson, String passphrase) throws Exception {
        byte[] salt = PBKDF2KeyDerivation.generateSalt();
        SecretKey derivedKey = PBKDF2KeyDerivation.deriveKey(passphrase.toCharArray(), salt);
        String encryptedPayload = AESEncryptionEngine.encrypt(rawDataJson, derivedKey);

        JSONObject backupJson = new JSONObject();
        backupJson.put("header", BACKUP_HEADER);
        backupJson.put("version", 1);
        backupJson.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        backupJson.put("payload", encryptedPayload);
        backupJson.put("timestamp", System.currentTimeMillis());

        return backupJson.toString(2);
    }

    /**
     * Decrypts backup string using passphrase.
     */
    public static String importEncryptedBackup(String backupJsonStr, String passphrase) throws Exception {
        JSONObject backupJson = new JSONObject(backupJsonStr.trim());
        String header = backupJson.optString("header", "");
        if (!BACKUP_HEADER.equals(header)) {
            throw new IllegalArgumentException("Unsupported or invalid backup format.");
        }

        String saltBase64 = backupJson.getString("salt");
        String payload = backupJson.getString("payload");

        byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
        SecretKey derivedKey = PBKDF2KeyDerivation.deriveKey(passphrase.toCharArray(), salt);

        return AESEncryptionEngine.decrypt(payload, derivedKey);
    }
}
