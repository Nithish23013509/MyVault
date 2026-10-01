package com.example.data

import android.util.Log
import com.example.crypto.AESEncryptionEngine
import com.example.crypto.KeyStoreHelper
import com.example.crypto.VaultBackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.crypto.SecretKey

class VaultRepository(private val vaultDao: VaultDao) {

    private val masterKey: SecretKey
        get() = KeyStoreHelper.getOrCreateMasterKey()

    val allItems: Flow<List<VaultItem>> = vaultDao.getAllItems().map { entities ->
        entities.mapNotNull { entity ->
            decryptEntity(entity)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getItemById(id: Long): VaultItem? = withContext(Dispatchers.IO) {
        val entity = vaultDao.getItemById(id) ?: return@withContext null
        decryptEntity(entity)
    }

    suspend fun saveItem(item: VaultItem): Long = withContext(Dispatchers.IO) {
        val entity = encryptItem(item)
        if (item.id == 0L) {
            vaultDao.insertItem(entity)
        } else {
            vaultDao.updateItem(entity)
            item.id
        }
    }

    suspend fun deleteItem(item: VaultItem) = withContext(Dispatchers.IO) {
        vaultDao.deleteById(item.id)
    }

    suspend fun toggleFavorite(item: VaultItem) = withContext(Dispatchers.IO) {
        val updated = item.copy(favorite = !item.favorite, updatedAt = System.currentTimeMillis())
        vaultDao.updateItem(encryptItem(updated))
    }

    suspend fun exportEncryptedBackup(passphrase: String, currentItems: List<VaultItem>): String = withContext(Dispatchers.IO) {
        val rawJsonArray = JSONArray()
        for (item in currentItems) {
            val obj = JSONObject().apply {
                put("type", item.type.name)
                put("title", item.title)
                put("value", item.value)
                put("tags", item.tags)
                put("notes", item.notes)
                put("collectionId", item.collectionId ?: "")
                put("favorite", item.favorite)
                put("pinned", item.pinned)
                put("sensitive", item.sensitive)
                put("archived", item.archived)
                put("trash", item.trash)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            }
            rawJsonArray.put(obj)
        }
        VaultBackupCrypto.exportEncryptedBackup(rawJsonArray.toString(), passphrase)
    }

    suspend fun importEncryptedBackup(encryptedBackupStr: String, passphrase: String): Int =
        withContext(Dispatchers.IO) {
            val decryptedJsonStr = VaultBackupCrypto.importEncryptedBackup(encryptedBackupStr, passphrase)
            val jsonArray = JSONArray(decryptedJsonStr)
            var count = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val value = when {
                    obj.has("value") -> obj.optString("value", "")
                    obj.has("password") -> obj.optString("password", "")
                    obj.has("url") -> obj.optString("url", "")
                    obj.has("documentNumber") -> obj.optString("documentNumber", "")
                    else -> ""
                }
                val tags = when {
                    obj.has("tags") -> obj.optString("tags", "")
                    obj.has("username") -> obj.optString("username", "")
                    else -> ""
                }
                val item = VaultItem(
                    id = 0,
                    type = VaultItemType.fromString(obj.optString("type", "PASSWORD")),
                    title = obj.optString("title", "Imported Item"),
                    value = value,
                    collectionId = if (obj.has("collectionId") && obj.getString("collectionId").isNotBlank()) obj.getString("collectionId") else null,
                    tags = tags,
                    notes = obj.optString("notes", ""),
                    favorite = obj.optBoolean("favorite", obj.optBoolean("isFavorite", false)),
                    pinned = obj.optBoolean("pinned", false),
                    sensitive = obj.optBoolean("sensitive", true),
                    archived = obj.optBoolean("archived", false),
                    trash = obj.optBoolean("trash", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis()
                )
                saveItem(item)
                count++
            }
            count
        }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        vaultDao.deleteAll()
    }

    private fun encryptItem(item: VaultItem): VaultItemEntity {
        val payloadObj = JSONObject().apply {
            put("value", item.value.ifEmpty { item.effectiveValue })
            put("notes", item.notes)
            put("tags", item.tags.ifEmpty { item.categoryTag })
            put("collectionId", item.collectionId ?: "")
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

        val encryptedPayload = AESEncryptionEngine.encrypt(payloadObj.toString(), masterKey)

        return VaultItemEntity(
            id = item.id,
            type = item.type.name,
            title = item.title,
            collectionId = item.collectionId,
            tags = item.tags,
            categoryTag = item.categoryTag,
            encryptedPayload = encryptedPayload,
            favorite = item.favorite || item.isFavorite,
            pinned = item.pinned,
            sensitive = item.sensitive,
            archived = item.archived,
            trash = item.trash,
            deletedAt = item.deletedAt,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
    }

    private fun decryptEntity(entity: VaultItemEntity): VaultItem? {
        return try {
            val decryptedJson = AESEncryptionEngine.decrypt(entity.encryptedPayload, masterKey)
            val payloadObj = JSONObject(decryptedJson)

            val value = when {
                payloadObj.has("value") -> payloadObj.optString("value", "")
                payloadObj.has("password") -> payloadObj.optString("password", "")
                payloadObj.has("url") -> payloadObj.optString("url", "")
                payloadObj.has("documentNumber") -> payloadObj.optString("documentNumber", "")
                else -> ""
            }
            val notes = payloadObj.optString("notes", "")
            val tags = when {
                payloadObj.has("tags") && payloadObj.getString("tags").isNotBlank() -> payloadObj.getString("tags")
                payloadObj.has("username") -> payloadObj.optString("username", "")
                else -> entity.tags
            }
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
            val categoryTag = payloadObj.optString("categoryTag", entity.categoryTag)

            VaultItem(
                id = entity.id,
                type = VaultItemType.fromString(entity.type),
                title = entity.title,
                value = value,
                collectionId = entity.collectionId,
                tags = tags,
                notes = notes,
                favorite = entity.favorite,
                pinned = entity.pinned,
                sensitive = entity.sensitive,
                archived = entity.archived,
                trash = entity.trash,
                deletedAt = entity.deletedAt,
                categoryTag = categoryTag,
                isFavorite = entity.favorite,
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
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error decrypting entity ${entity.id}", e)
            null
        }
    }
}
