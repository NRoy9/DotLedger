package com.nitin.dotledger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.dao.CategoryTotal
import com.nitin.dotledger.data.entities.*
import com.nitin.dotledger.data.repository.DotLedgerRepository
import kotlinx.coroutines.launch
import com.nitin.dotledger.data.entities.Budget
import com.nitin.dotledger.data.entities.BudgetWithSpending
import com.nitin.dotledger.data.models.TransactionFilter

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DotLedgerRepository

    val allAccounts: LiveData<List<Account>>
    val allCategories: LiveData<List<Category>>
    val allTransactions: LiveData<List<Transaction>>
    val totalBalance: LiveData<Double?>
    val appSettings: LiveData<AppSettings?>

    private val _currentFilter = MutableLiveData<TransactionFilter>()
    val currentFilter: LiveData<com.nitin.dotledger.data.models.TransactionFilter> = _currentFilter

    private val _filteredTransactions = MutableLiveData<List<Transaction>>()
    val filteredTransactions: LiveData<List<Transaction>> = _filteredTransactions

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DotLedgerRepository(
            database.accountDao(),
            database.categoryDao(),
            database.transactionDao(),
            database.settingsDao(),
            database.budgetDao(),
            database.recurringTransactionDao()
        )

        allAccounts = repository.allAccounts
        allCategories = repository.allCategories
        allTransactions = repository.allTransactions
        totalBalance = repository.totalBalance
        appSettings = database.settingsDao().getSettings()

        // Add these properties
        val allRecurring: LiveData<List<RecurringTransaction>> = repository.getAllRecurring()
        val activeRecurring: LiveData<List<RecurringTransaction>> = repository.getActiveRecurring()

    }

    // Settings operations
    fun updateSettings(settings: AppSettings) = viewModelScope.launch {
        repository.updateSettings(settings)
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

    // Budget operations
    val allActiveBudgets: LiveData<List<Budget>> = repository.allActiveBudgets

    fun insertBudget(budget: Budget) = viewModelScope.launch {
        repository.insertBudget(budget)
    }

    fun updateBudget(budget: Budget) = viewModelScope.launch {
        repository.updateBudget(budget)
    }

    fun deleteBudget(budget: Budget) = viewModelScope.launch {
        repository.deleteBudget(budget)
    }

    suspend fun getBudgetById(budgetId: Long): Budget? {
        return repository.getBudgetById(budgetId)
    }

    fun getCurrentBudgets(currentDate: Long): LiveData<List<Budget>> {
        return repository.getCurrentBudgets(currentDate)
    }

    suspend fun getBudgetWithSpending(budget: Budget): BudgetWithSpending {
        return repository.getBudgetWithSpending(budget)
    }

    suspend fun getAllBudgetsWithSpending(currentDate: Long): List<BudgetWithSpending> {
        return repository.getAllBudgetsWithSpending(currentDate)
    }

    suspend fun deactivateBudget(budgetId: Long) {
        repository.deactivateBudget(budgetId)
    }

    fun applyFilter(filter: com.nitin.dotledger.data.models.TransactionFilter) {
        _currentFilter.value = filter

        repository.searchAndFilterTransactions(filter).observeForever { transactions ->
            _filteredTransactions.value = transactions
        }
    }

    fun clearFilter() {
        _currentFilter.value = com.nitin.dotledger.data.models.TransactionFilter()
        _filteredTransactions.value = allTransactions.value
    }

    fun searchTransactions(query: String) {
        if (query.isEmpty()) {
            _filteredTransactions.value = allTransactions.value
        } else {
            repository.searchTransactions(query).observeForever { transactions ->
                _filteredTransactions.value = transactions
            }
        }
    }

    fun hasActiveFilter(): Boolean {
        return _currentFilter.value?.hasActiveFilters() ?: false
    }

    fun getFilterSummary(): String {
        val filter = _currentFilter.value ?: return "No filters"
        return com.nitin.dotledger.utils.FilterSummaryHelper.generateSummary(
            filter,
            allAccounts.value,
            allCategories.value
        )
    }

    fun getFilterBadge(): String {
        val filter = _currentFilter.value ?: return "All"
        return com.nitin.dotledger.utils.FilterSummaryHelper.getFilterBadge(filter)
    }

    fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) = viewModelScope.launch {
        repository.insertRecurringTransaction(recurringTransaction)
    }

    fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) = viewModelScope.launch {
        repository.updateRecurringTransaction(recurringTransaction)
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) = viewModelScope.launch {
        repository.deleteRecurringTransaction(recurringTransaction)
    }

    fun getRecurringTransactionById(id: Long): LiveData<RecurringTransaction?> {
        return repository.getRecurringTransactionByIdLive(id)
    }

    suspend fun getRecurringTransactionByIdSync(id: Long): RecurringTransaction? {
        return repository.getRecurringTransactionById(id)
    }

    fun toggleRecurringStatus(id: Long, isActive: Boolean) = viewModelScope.launch {
        repository.updateRecurringActiveStatus(id, isActive)
    }

}