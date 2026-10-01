package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.crypto.AESEncryptionEngine
import com.example.crypto.PBKDF2KeyDerivation
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKey

/**
 * Handles True Zero-Knowledge Cloud Sync with Cloud Firestore.
 * Sensitive data fields (value, password, notes, tags) are encrypted with a client-side
 * PBKDF2-derived user vault key before upload. The cloud server receives ONLY encrypted ciphertexts
 * and never sees plaintext credentials.
 */
class FirestoreVaultRepository private constructor(private val context: Context) {

    private var firestore: FirebaseFirestore? = null

    init {
        initFirestore()
    }

    private fun initFirestore() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization notice: ${e.localizedMessage}")
        }
    }

    private fun getDb(): FirebaseFirestore? {
        if (firestore == null) {
            initFirestore()
        }
        return firestore
    }

    /**
     * Retrieves or generates the user's random cloud salt from their user document in Firestore.
     */
    suspend fun getOrCreateCloudSalt(userId: String): String = withContext(Dispatchers.IO) {
        val db = getDb() ?: throw IllegalStateException("Firestore not initialized")
        val userDocRef = db.collection("users").document(userId)
        val snapshot = userDocRef.get().await()

        val existingSalt = snapshot.getString("cloudSalt")
        if (!existingSalt.isNullOrBlank()) {
            return@withContext existingSalt
        }

        // Generate fresh cryptographically secure random 16-byte salt
        val newSaltBytes = PBKDF2KeyDerivation.generateSalt()
        val newSaltBase64 = Base64.encodeToString(newSaltBytes, Base64.NO_WRAP)

        val metadata = hashMapOf(
            "cloudSalt" to newSaltBase64,
            "version" to 1,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        userDocRef.set(metadata, SetOptions.merge()).await()
        newSaltBase64
    }

    /**
     * Derives the Zero-Knowledge Cloud AES SecretKey from the user's Master PIN and cloud salt.
     */
    fun deriveCloudKey(passphraseOrPin: String, cloudSaltBase64: String): SecretKey {
        val saltBytes = Base64.decode(cloudSaltBase64, Base64.NO_WRAP)
        return PBKDF2KeyDerivation.deriveKey(passphraseOrPin.toCharArray(), saltBytes)
    }

    /**
     * Persists an item to Cloud Firestore with end-to-end Zero-Knowledge encryption.
     * Sensitive fields (value, notes, tags) are encrypted client-side using [cloudKey].
     */
    suspend fun saveItem(userId: String, item: VaultItem, cloudKey: SecretKey): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = getDb() ?: return@withContext Result.failure(Exception("Firestore not initialized"))

            // Pack all sensitive fields into client-side encrypted JSON payload
            val payloadObj = JSONObject().apply {
                put("value", item.value.ifEmpty { item.effectiveValue })
                put("notes", item.notes)
                put("tags", item.tags.ifEmpty { item.categoryTag })
                put("collectionId", item.collectionId ?: "")
                put("pinned", item.pinned)
                put("sensitive", item.sensitive)
                put("archived", item.archived)
                put("trash", item.trash)
                put("categoryTag", item.categoryTag)
                put("username", item.username)
                put("password", item.password)
                put("url", item.url)
                put("documentNumber", item.documentNumber)
                put("documentType", item.documentType)
                put("documentExpiry", item.documentExpiry)
                put("issuedBy", item.issuedBy)
                put("emailAddress", item.emailAddress)
                put("emailProvider", item.emailProvider)
                put("recoveryEmail", item.recoveryEmail)
            }

            // Zero-Knowledge Encrypt using user's cloud key
            val encryptedPayload = AESEncryptionEngine.encrypt(payloadObj.toString(), cloudKey)

            // Strictly upload metadata + encrypted payload. NO PLAINTEXT SENSITIVE FIELDS.
            val cloudData = hashMapOf(
                "id" to item.id,
                "type" to item.type.name,
                "title" to item.title,
                "encryptedPayload" to encryptedPayload,
                "isFavorite" to item.favorite,
                "createdAt" to item.createdAt,
                "updatedAt" to item.updatedAt,
                "lastSyncedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(userId)
                .collection("vault_items")
                .document(item.id.toString())
                .set(cloudData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving item to Firestore: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Deletes an item from Cloud Firestore.
     */
    suspend fun deleteItem(userId: String, itemId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = getDb() ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            db.collection("users")
                .document(userId)
                .collection("vault_items")
                .document(itemId.toString())
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item from Firestore: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Fetches all cloud-synced items for a user and decrypts their payloads using [cloudKey].
     */
    suspend fun fetchItems(userId: String, cloudKey: SecretKey): Result<List<VaultItem>> = withContext(Dispatchers.IO) {
        try {
            val db = getDb() ?: return@withContext Result.failure(Exception("Firestore not initialized"))
            val snapshot = db.collection("users")
                .document(userId)
                .collection("vault_items")
                .get()
                .await()

            val list = mutableListOf<VaultItem>()
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: continue
                val typeStr = doc.getString("type") ?: VaultItemType.PASSWORD.name
                val type = VaultItemType.fromString(typeStr)
                val title = doc.getString("title") ?: "Encrypted Item"
                val encryptedPayload = doc.getString("encryptedPayload") ?: ""
                val isFavorite = doc.getBoolean("isFavorite") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                if (encryptedPayload.isNotBlank()) {
                    try {
                        val decryptedJson = AESEncryptionEngine.decrypt(encryptedPayload, cloudKey)
                        val payloadObj = JSONObject(decryptedJson)

                        val value = payloadObj.optString("value", "")
                        val notes = payloadObj.optString("notes", "")
                        val tags = payloadObj.optString("tags", "")
                        val collectionId = if (payloadObj.has("collectionId") && payloadObj.getString("collectionId").isNotBlank()) payloadObj.getString("collectionId") else null
                        val pinned = payloadObj.optBoolean("pinned", false)
                        val sensitive = payloadObj.optBoolean("sensitive", true)
                        val archived = payloadObj.optBoolean("archived", false)
                        val trash = payloadObj.optBoolean("trash", false)
                        val categoryTag = payloadObj.optString("categoryTag", "Personal")
                        val username = payloadObj.optString("username", "")
                        val password = payloadObj.optString("password", "")
                        val url = payloadObj.optString("url", "")
                        val documentNumber = payloadObj.optString("documentNumber", "")
                        val documentType = payloadObj.optString("documentType", "")
                        val documentExpiry = payloadObj.optString("documentExpiry", "")
                        val issuedBy = payloadObj.optString("issuedBy", "")
                        val emailAddress = payloadObj.optString("emailAddress", "")
                        val emailProvider = payloadObj.optString("emailProvider", "")
                        val recoveryEmail = payloadObj.optString("recoveryEmail", "")

                        list.add(
                            VaultItem(
                                id = id,
                                type = type,
                                title = title,
                                value = value,
                                collectionId = collectionId,
                                tags = tags,
                                notes = notes,
                                favorite = isFavorite,
                                pinned = pinned,
                                sensitive = sensitive,
                                archived = archived,
                                trash = trash,
                                categoryTag = categoryTag,
                                isFavorite = isFavorite,
                                username = username,
                                password = password,
                                url = url,
                                documentNumber = documentNumber,
                                documentType = documentType,
                                documentExpiry = documentExpiry,
                                issuedBy = issuedBy,
                                emailAddress = emailAddress,
                                emailProvider = emailProvider,
                                recoveryEmail = recoveryEmail,
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not decrypt item $id with current cloud key: ${e.localizedMessage}")
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching items from Firestore: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Batch syncs all local items to Cloud Firestore with Zero-Knowledge encryption.
     */
    suspend fun syncAllItemsToCloud(userId: String, items: List<VaultItem>, cloudKey: SecretKey): Result<Int> = withContext(Dispatchers.IO) {
        var syncedCount = 0
        for (item in items) {
            val res = saveItem(userId, item, cloudKey)
            if (res.isSuccess) syncedCount++
        }
        Result.success(syncedCount)
    }

    /**
     * Persists custom collection definitions to the user's cloud document.
     */
    suspend fun saveCustomCollections(userId: String, collectionsJson: String) = withContext(Dispatchers.IO) {
        try {
            val db = getDb() ?: return@withContext
            db.collection("users").document(userId)
                .set(mapOf("customCollections" to collectionsJson, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving custom collections to cloud: ${e.localizedMessage}")
        }
    }

    /**
     * Fetches custom collection definitions from the user's cloud document.
     */
    suspend fun fetchCustomCollections(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val db = getDb() ?: return@withContext null
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.getString("customCollections")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Subscribes to real-time changes in Firestore for automatic cloud sync.
     */
    fun listenToItems(
        userId: String,
        cloudKey: SecretKey,
        onItemsUpdated: (List<VaultItem>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): com.google.firebase.firestore.ListenerRegistration? {
        val db = getDb() ?: return null
        return db.collection("users")
            .document(userId)
            .collection("vault_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Cloud sync listener error: ${error.localizedMessage}")
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val items = mutableListOf<VaultItem>()
                for (doc in snapshot.documents) {
                    val id = doc.getLong("id") ?: continue
                    val typeStr = doc.getString("type") ?: VaultItemType.PASSWORD.name
                    val type = VaultItemType.fromString(typeStr)
                    val title = doc.getString("title") ?: "Encrypted Item"
                    val encryptedPayload = doc.getString("encryptedPayload") ?: ""
                    val isFavorite = doc.getBoolean("isFavorite") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    if (encryptedPayload.isNotBlank()) {
                        try {
                            val decryptedJson = AESEncryptionEngine.decrypt(encryptedPayload, cloudKey)
                            val payloadObj = JSONObject(decryptedJson)

                            val value = payloadObj.optString("value", "")
                            val notes = payloadObj.optString("notes", "")
                            val tags = payloadObj.optString("tags", "")
                            val collectionId = if (payloadObj.has("collectionId") && payloadObj.getString("collectionId").isNotBlank()) payloadObj.getString("collectionId") else null
                            val pinned = payloadObj.optBoolean("pinned", false)
                            val sensitive = payloadObj.optBoolean("sensitive", true)
                            val archived = payloadObj.optBoolean("archived", false)
                            val trash = payloadObj.optBoolean("trash", false)
                            val categoryTag = payloadObj.optString("categoryTag", "Personal")
                            val username = payloadObj.optString("username", "")
                            val password = payloadObj.optString("password", "")
                            val url = payloadObj.optString("url", "")
                            val documentNumber = payloadObj.optString("documentNumber", "")
                            val documentType = payloadObj.optString("documentType", "")
                            val documentExpiry = payloadObj.optString("documentExpiry", "")
                            val issuedBy = payloadObj.optString("issuedBy", "")
                            val emailAddress = payloadObj.optString("emailAddress", "")
                            val emailProvider = payloadObj.optString("emailProvider", "")
                            val recoveryEmail = payloadObj.optString("recoveryEmail", "")

                            items.add(
                                VaultItem(
                                    id = id,
                                    type = type,
                                    title = title,
                                    value = value,
                                    collectionId = collectionId,
                                    tags = tags,
                                    notes = notes,
                                    favorite = isFavorite,
                                    pinned = pinned,
                                    sensitive = sensitive,
                                    archived = archived,
                                    trash = trash,
                                    categoryTag = categoryTag,
                                    isFavorite = isFavorite,
                                    username = username,
                                    password = password,
                                    url = url,
                                    documentNumber = documentNumber,
                                    documentType = documentType,
                                    documentExpiry = documentExpiry,
                                    issuedBy = issuedBy,
                                    emailAddress = emailAddress,
                                    emailProvider = emailProvider,
                                    recoveryEmail = recoveryEmail,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Decryption error in listener for item $id: ${e.localizedMessage}")
                        }
                    }
                }
                onItemsUpdated(items)
            }
    }

    companion object {
        private const val TAG = "FirestoreVaultRepo"

        @Volatile
        private var instance: FirestoreVaultRepository? = null

        fun getInstance(context: Context): FirestoreVaultRepository {
            return instance ?: synchronized(this) {
                instance ?: FirestoreVaultRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
