package com.example

import com.example.crypto.AESEncryptionEngine
import com.example.crypto.PBKDF2KeyDerivation
import com.example.crypto.PasswordGenerator
import com.example.crypto.PasswordStrengthCalculator
import com.example.crypto.VaultBackupCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testAesEncryptionDecryption() {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val secretMessage = "TopSecretPassword123!@#"
        val encryptedBase64 = AESEncryptionEngine.encrypt(secretMessage, secretKey)

        assertNotNull(encryptedBase64)
        assertNotEquals(secretMessage, encryptedBase64)

        val decrypted = AESEncryptionEngine.decrypt(encryptedBase64, secretKey)
        assertEquals(secretMessage, decrypted)
    }

    @Test
    fun testPbkdf2PinDerivationAndVerification() {
        val pin = "8492"
        val salt = PBKDF2KeyDerivation.generateSalt()
        val hash = PBKDF2KeyDerivation.hashCredential(pin, salt)

        assertNotNull(hash)
        assertTrue(PBKDF2KeyDerivation.verifyCredential("8492", hash, salt))
        assertTrue(!PBKDF2KeyDerivation.verifyCredential("0000", hash, salt))
    }

    @Test
    fun testPasswordGenerator() {
        val password = PasswordGenerator.generate(18, true, true, true, true, true)
        assertEquals(18, password.length)

        val eval = PasswordStrengthCalculator.evaluate(password)
        assertTrue(eval.score >= 3)
    }

    @Test
    fun testBiometricAuthManager() {
        val manager = com.example.security.BiometricAuthManager.getInstance()
        assertNotNull(manager)
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val status = manager.checkBiometricStatus(context)
        assertNotNull(status)
    }

    @Test
    fun testVaultEntryEntity() {
        val entry = com.example.data.VaultEntry(
            id = 1L,
            title = "Personal Server",
            encryptedContent = "U2FsdGVkX1+vupppZksvRf5pq5g5XjFR",
            category = com.example.data.VaultEntry.CATEGORY_PASSWORD,
            timestamp = 1696118400000L
        )

        assertEquals("Personal Server", entry.title)
        assertEquals("U2FsdGVkX1+vupppZksvRf5pq5g5XjFR", entry.encryptedContent)
        assertEquals("password", entry.category)
        assertEquals(1696118400000L, entry.timestamp)
    }

    @Test
    fun testAppDatabaseAndVaultEntryDao() = kotlinx.coroutines.runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val dao = db.vaultEntryDao()
        val entry = com.example.data.VaultEntry(
            title = "ProtonMail",
            encryptedContent = "CiphertextSecret123",
            category = com.example.data.VaultEntry.CATEGORY_PASSWORD,
            timestamp = System.currentTimeMillis()
        )

        val id = dao.insertEntry(entry)
        assertTrue(id > 0)

        val retrieved = dao.getEntryById(id)
        assertNotNull(retrieved)
        assertEquals("ProtonMail", retrieved?.title)
        assertEquals("CiphertextSecret123", retrieved?.encryptedContent)
        assertEquals("password", retrieved?.category)

        val count = dao.getCount()
        assertEquals(1, count)

        db.close()
    }

    @Test
    fun testAESEncryptionManager() {
        val manager = com.example.crypto.AESEncryptionManager.getInstance()
        assertNotNull(manager)

        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val sensitiveData = "SuperSecretBankLoginPassword#2026!"
        val encryptedBase64 = manager.encrypt(sensitiveData, secretKey)
        assertNotNull(encryptedBase64)
        assertNotEquals(sensitiveData, encryptedBase64)

        val decrypted = manager.decrypt(encryptedBase64, secretKey)
        assertEquals(sensitiveData, decrypted)
    }

    @Test
    fun testVaultViewModelVaultEntryOperations() = kotlinx.coroutines.runBlocking {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.VaultViewModel(app)
        assertNotNull(viewModel.vaultEntryDao)
        assertNotNull(viewModel.vaultEntries)
        assertNotNull(viewModel.vaultEntriesUiState)

        val entry = com.example.data.VaultEntry(
            title = "Test Note",
            encryptedContent = "EncryptedTextPayload999",
            category = com.example.data.VaultEntry.CATEGORY_NOTE,
            timestamp = System.currentTimeMillis()
        )
        viewModel.saveVaultEntry(entry)
    }

    @Test
    fun testVaultBackupEncryption() {
        val rawVaultData = """[{"title":"Google","username":"user@gmail.com","password":"secret"}]"""
        val passphrase = "MasterBackupPassphrase99!"

        val backupEncrypted = VaultBackupCrypto.exportEncryptedBackup(rawVaultData, passphrase)
        assertNotNull(backupEncrypted)
        assertTrue(backupEncrypted.contains("CIPHERVAULT_V1"))

        val restored = VaultBackupCrypto.importEncryptedBackup(backupEncrypted, passphrase)
        assertEquals(rawVaultData, restored)
    }
}
