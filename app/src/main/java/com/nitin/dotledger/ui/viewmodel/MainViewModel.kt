package com.nitin.dotledger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.dao.CategoryTotal
import com.nitin.dotledger.data.entities.*
import com.nitin.dotledger.data.repository.DotLedgerRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DotLedgerRepository

    val allAccounts: LiveData<List<Account>>
    val allCategories: LiveData<List<Category>>
    val allTransactions: LiveData<List<Transaction>>
    val totalBalance: LiveData<Double?>
    val appSettings: LiveData<AppSettings?>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DotLedgerRepository(
            database.accountDao(),
            database.categoryDao(),
            database.transactionDao(),
            database.settingsDao()
        )

        allAccounts = repository.allAccounts
        allCategories = repository.allCategories
        allTransactions = repository.allTransactions
        totalBalance = repository.totalBalance
        appSettings = repository.appSettings
    }

    // Settings operations
    fun updateSettings(settings: AppSettings) = viewModelScope.launch {
        repository.saveSettings(settings)
    }

    suspend fun getSettingsSync(): AppSettings? {
        return repository.getSettingsSync()
    }

    // Account operations
    fun insertAccount(account: Account) = viewModelScope.launch {
        repository.insertAccount(account)
    }

    fun updateAccount(account: Account) = viewModelScope.launch {
        repository.updateAccount(account)
    }

    fun deleteAccount(account: Account) = viewModelScope.launch {
        repository.deleteAccount(account)
    }

    fun getAccountById(accountId: Long): LiveData<Account?> {
        return repository.getAccountByIdLive(accountId)
    }

    fun updateAccountBalance(accountId: Long, balance: Double) = viewModelScope.launch {
        repository.updateAccountBalance(accountId, balance)
    }

    // Category operations
    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        repository.updateCategory(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>> {
        return repository.getCategoriesByType(type)
    }

    // Transaction operations
    fun insertTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) = viewModelScope.launch {
        repository.updateTransaction(oldTransaction, newTransaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    suspend fun getTransactionById(transactionId: Long): Transaction? {
        return repository.getTransactionById(transactionId)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionsByAccount(accountId: Long): LiveData<List<Transaction>> {
        return repository.getTransactionsByAccount(accountId)
    }

    fun getTotalByType(type: TransactionType): LiveData<Double?> {
        return repository.getTotalByType(type)
    }

    fun getCreditCardExpenses(accountId: Long): LiveData<List<Transaction>> {
        return repository.getCreditCardExpenses(accountId)
    }

    suspend fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal> {
        return repository.getCategoryTotals(type, startDate, endDate)
    }

    // Helper function to calculate total with formula
    suspend fun calculateTotal(): Double {
        val accounts = allAccounts.value ?: emptyList()
        val accountBalance = accounts.sumOf { it.balance }

        val incomeTotal = repository.getTotalByTypeAndDateRange(
            TransactionType.INCOME,
            0,
            System.currentTimeMillis()
        ) ?: 0.0

        val expenseTotal = repository.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            0,
            System.currentTimeMillis()
        ) ?: 0.0

        return accountBalance + incomeTotal - expenseTotal
    }
}