package com.example.data

enum class VaultItemType(val displayName: String) {
    LINK("Link"),
    PHONE("Phone"),
    EMAIL("Email"),
    TEXT("Text"),
    NUMBER("Number"),
    CUSTOM("Custom"),
    RECORD("Multi-Field Record"),
    PASSWORD("Password"),
    DOCUMENT_ID("Document ID"),
    NOTE("Secure Note"),
    MAIL("Email Account");

    companion object {
        fun fromString(value: String): VaultItemType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                when (value.uppercase()) {
                    "RECORD", "MULTI_FIELD", "MULTI_FIELD_RECORD" -> RECORD
                    "DOCUMENT_ID" -> DOCUMENT_ID
                    "MAIL" -> MAIL
                    "CARD" -> NUMBER
                    else -> PASSWORD
                }
            }
        }
    }
}

/**
 * Collection model representing a grouped vault category (e.g., Personal, College, Work, Projects).
 */
data class VaultCollection(
    val id: String,
    val name: String,
    val icon: String = "📁",
    val colorHex: String = "#10B981",
    val description: String = ""
)

/**
 * Generic vault item representing an encrypted credential, secret, link, note, or custom field.
 */
data class VaultItem(
    val id: Long = 0,
    val type: VaultItemType = VaultItemType.PASSWORD,
    val title: String = "",
    val value: String = "",
    val collectionId: String? = null,
    val tags: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val sensitive: Boolean = true,
    val archived: Boolean = false,
    val trash: Boolean = false,
    val deletedAt: Long? = null,
    val categoryTag: String = "Personal",
    val isFavorite: Boolean = false,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val documentNumber: String = "",
    val documentType: String = "",
    val documentExpiry: String = "",
    val issuedBy: String = "",
    val emailAddress: String = "",
    val emailProvider: String = "",
    val recoveryEmail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveFavorite: Boolean get() = favorite || isFavorite
    val effectiveValue: String
        get() = value.ifEmpty {
            password.ifEmpty {
                url.ifEmpty {
                    documentNumber.ifEmpty {
                        emailAddress
                    }
                }
            }
        }
    val effectiveTags: String get() = tags.ifEmpty { username.ifEmpty { categoryTag } }
}

/**
 * Multi-field record representing grouped structured fields.
 */
data class VaultRecord(
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

/**
 * Individual field inside a multi-field [VaultRecord].
 */
data class VaultRecordField(
    val id: Long = 0,
    val recordId: Long = 0,
    val fieldName: String,
    val fieldType: String = "text",
    val value: String = "",
    val sensitive: Boolean = true,
    val position: Int = 0
)
