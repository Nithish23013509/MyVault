package com.example.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val collectionId: String? = null,
    val tags: String = "",
    val categoryTag: String = "",
    val encryptedPayload: String, // Locally encrypted using Android KeyStore master key
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val sensitive: Boolean = true,
    val archived: Boolean = false,
    val trash: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val isFavorite: Boolean get() = favorite

    @get:Ignore
    val encryptedContent: String get() = encryptedPayload

    @get:Ignore
    val category: String get() = type

    @get:Ignore
    val timestamp: Long get() = updatedAt
}

@Entity(tableName = "vault_records")
data class VaultRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val collectionId: String? = null,
    val tags: String = "",
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val sensitive: Boolean = true,
    val archived: Boolean = false,
    val trash: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_record_fields",
    indices = [Index(value = ["recordId"])]
)
data class VaultRecordFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val fieldName: String,
    val fieldType: String,
    val encryptedValue: String,
    val sensitive: Boolean = true,
    val position: Int = 0
)
