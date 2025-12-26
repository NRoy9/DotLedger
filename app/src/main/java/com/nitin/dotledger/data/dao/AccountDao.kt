package com.nitin.dotledger.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nitin.dotledger.data.entities.Account

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllAccounts(): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdLive(accountId: Long): LiveData<Account?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1")
    fun getTotalBalance(): LiveData<Double?>

    @Query("UPDATE accounts SET balance = :balance WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, balance: Double)
}