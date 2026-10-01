package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for vault entries containing metadata and encrypted payload.
 *
 * @param id Unique primary key for the database record.
 * @param title Descriptive title or label for the vault entry.
 * @param encryptedContent Authenticated AES-256-GCM encrypted ciphertext payload.
 * @param category Category classification (e.g. "password", "link", "note").
 * @param timestamp Epoch milliseconds timestamp of when the entry was created or updated.
 */
@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "encrypted_content")
    val encryptedContent: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_PASSWORD = "password"
        const val CATEGORY_LINK = "link"
        const val CATEGORY_NOTE = "note"
    }
}
