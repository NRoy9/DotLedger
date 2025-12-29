package com.nitin.dotledger.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalByType(type: TransactionType): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double?

    @Query("SELECT * FROM transactions WHERE type = :type AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE type = :type AND date >= :startDate AND date <= :endDate GROUP BY categoryId")
    suspend fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = 'EXPENSE' ORDER BY date DESC")
    fun getCreditCardExpenses(accountId: Long): LiveData<List<Transaction>>

    // Search and filter methods
    // Enhanced search and filter methods
    @Query("""
    SELECT * FROM transactions 
    WHERE (:searchQuery = '' OR note LIKE '%' || :searchQuery || '%')
    AND (:hasTypes = 0 OR type IN (:types))
    AND (:hasAccounts = 0 OR accountId IN (:accountIds))
    AND (:hasCategories = 0 OR categoryId IN (:categoryIds) OR (:includeNull = 1 AND categoryId IS NULL))
    AND date >= :startDate AND date <= :endDate
    AND amount >= :minAmount AND amount <= :maxAmount
    ORDER BY 
        CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
        CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
        CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
        CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC
""")
    fun searchAndFilterTransactions(
        searchQuery: String,
        types: List<String>,
        hasTypes: Int,
        accountIds: List<Long>,
        hasAccounts: Int,
        categoryIds: List<Long>,
        hasCategories: Int,
        includeNull: Int,
        startDate: Long,
        endDate: Long,
        minAmount: Double,
        maxAmount: Double,
        sortBy: String
    ): LiveData<List<Transaction>>

    @Query("""
    SELECT * FROM transactions 
    WHERE note LIKE '%' || :query || '%'
    ORDER BY date DESC
    LIMIT :limit
""")
    fun searchTransactionsByNote(query: String, limit: Int = 50): LiveData<List<Transaction>>
}

data class CategoryTotal(
    val categoryId: Long?,
    val total: Double
)