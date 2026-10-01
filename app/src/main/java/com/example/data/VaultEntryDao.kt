package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [VaultEntry].
 * Provides asynchronous queries and transaction operations for local storage.
 */
@Dao
interface VaultEntryDao {

    @Query("SELECT * FROM vault_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): VaultEntry?

    @Query("SELECT * FROM vault_entries WHERE category = :category ORDER BY timestamp DESC")
    fun getEntriesByCategory(category: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchEntries(query: String): Flow<List<VaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<VaultEntry>): List<Long>

    @Update
    suspend fun updateEntry(entry: VaultEntry)

    @Delete
    suspend fun deleteEntry(entry: VaultEntry)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_entries WHERE category = :category")
    suspend fun deleteByCategory(category: String): Int

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM vault_entries")
    suspend fun getCount(): Int
}
