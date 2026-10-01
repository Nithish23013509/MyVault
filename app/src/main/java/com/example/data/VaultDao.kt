package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_items WHERE trash = 0 ORDER BY pinned DESC, favorite DESC, updatedAt DESC")
    fun getAllItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): VaultItemEntity?

    @Query("SELECT * FROM vault_items WHERE type = :type AND trash = 0 ORDER BY pinned DESC, favorite DESC, updatedAt DESC")
    fun getItemsByType(type: String): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity): Long

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()

    // Multi-field Records
    @Query("SELECT * FROM vault_records WHERE trash = 0 ORDER BY pinned DESC, favorite DESC, updatedAt DESC")
    fun getAllRecords(): Flow<List<VaultRecordEntity>>

    @Query("SELECT * FROM vault_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): VaultRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: VaultRecordEntity): Long

    @Update
    suspend fun updateRecord(record: VaultRecordEntity)

    @Query("DELETE FROM vault_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM vault_record_fields WHERE recordId = :recordId ORDER BY position ASC")
    fun getRecordFields(recordId: Long): Flow<List<VaultRecordFieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordField(field: VaultRecordFieldEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordFields(fields: List<VaultRecordFieldEntity>): List<Long>

    @Query("DELETE FROM vault_record_fields WHERE recordId = :recordId")
    suspend fun deleteRecordFields(recordId: Long)
}
