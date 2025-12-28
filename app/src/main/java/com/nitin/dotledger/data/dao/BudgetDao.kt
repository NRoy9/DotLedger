package com.nitin.dotledger.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nitin.dotledger.data.entities.Budget
import com.nitin.dotledger.data.entities.BudgetPeriod

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): Budget?

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveBudgets(): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1 AND startDate <= :currentDate AND endDate >= :currentDate")
    suspend fun getActiveBudgetForCategory(categoryId: Long, currentDate: Long): Budget?

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND isActive = 1 AND startDate <= :currentDate AND endDate >= :currentDate")
    suspend fun getOverallBudget(currentDate: Long): Budget?

    @Query("SELECT * FROM budgets WHERE isActive = 1 AND startDate <= :currentDate AND endDate >= :currentDate")
    fun getCurrentBudgets(currentDate: Long): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1")
    fun getBudgetsForCategory(categoryId: Long): LiveData<List<Budget>>

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :budgetId")
    suspend fun deactivateBudget(budgetId: Long)
}