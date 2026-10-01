package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Primary Room database configuration for the application.
 * Uses explicit migrations to protect user data and avoids destructive migrations.
 */
@Database(
    entities = [
        VaultEntry::class,
        VaultItemEntity::class,
        VaultRecordEntity::class,
        VaultRecordFieldEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun vaultDao(): VaultDao

    companion object {
        private const val DATABASE_NAME = "ciphervault_app.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add generic columns to vault_items
                db.execSQL("ALTER TABLE vault_items ADD COLUMN collectionId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN sensitive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN trash INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                // Create vault_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vault_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        collectionId TEXT,
                        tags TEXT NOT NULL,
                        favorite INTEGER NOT NULL,
                        pinned INTEGER NOT NULL,
                        sensitive INTEGER NOT NULL,
                        archived INTEGER NOT NULL,
                        trash INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create vault_record_fields table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vault_record_fields (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recordId INTEGER NOT NULL,
                        fieldName TEXT NOT NULL,
                        fieldType TEXT NOT NULL,
                        encryptedValue TEXT NOT NULL,
                        sensitive INTEGER NOT NULL,
                        position INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_record_fields_recordId ON vault_record_fields (recordId)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
