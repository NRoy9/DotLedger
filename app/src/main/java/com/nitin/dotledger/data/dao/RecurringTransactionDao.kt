package com.nitin.dotledger.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nitin.dotledger.data.entities.RecurringTransaction

@Dao
interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransaction): Long

    @Update
    suspend fun update(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun delete(recurringTransaction: RecurringTransaction)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransaction?

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getByIdLive(id: Long): LiveData<RecurringTransaction?>

    @Query("SELECT * FROM recurring_transactions ORDER BY name ASC")
    fun getAllRecurring(): LiveData<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveRecurring(): LiveData<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 0 ORDER BY name ASC")
    fun getInactiveRecurring(): LiveData<List<RecurringTransaction>>

    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE isActive = 1 
        AND nextOccurrence <= :currentDate
        ORDER BY nextOccurrence ASC
    """)
    suspend fun getDueRecurring(currentDate: Long): List<RecurringTransaction>

    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE isActive = 1 
        AND nextOccurrence <= :endDate
        AND (endDate IS NULL OR endDate >= :startDate)
        ORDER BY nextOccurrence ASC
    """)
    fun getRecurringInRange(startDate: Long, endDate: Long): LiveData<List<RecurringTransaction>>

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Query("UPDATE recurring_transactions SET nextOccurrence = :nextOccurrence, lastExecuted = :lastExecuted WHERE id = :id")
    suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long, lastExecuted: Long)

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE isActive = 1")
    fun getActiveCount(): LiveData<Int>
}