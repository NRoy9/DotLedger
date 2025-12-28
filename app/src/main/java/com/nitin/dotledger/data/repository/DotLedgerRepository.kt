package com.nitin.dotledger.data.repository

import androidx.lifecycle.LiveData
import com.nitin.dotledger.data.dao.AccountDao
import com.nitin.dotledger.data.dao.CategoryDao
import com.nitin.dotledger.data.dao.CategoryTotal
import com.nitin.dotledger.data.dao.SettingsDao
import com.nitin.dotledger.data.dao.TransactionDao
import com.nitin.dotledger.data.entities.*
import com.nitin.dotledger.data.entities.Budget
import com.nitin.dotledger.data.entities.BudgetWithSpending
import com.nitin.dotledger.data.dao.BudgetDao

class DotLedgerRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao,
    private val budgetDao: BudgetDao
) {
    // Settings operations
    val appSettings: LiveData<AppSettings?> = settingsDao.getSettings()

    suspend fun saveSettings(settings: AppSettings) = settingsDao.saveSettings(settings)

    suspend fun getSettingsSync(): AppSettings? = settingsDao.getSettingsSync()

    // Account operations
    val allAccounts: LiveData<List<Account>> = accountDao.getAllAccounts()
    val totalBalance: LiveData<Double?> = accountDao.getTotalBalance()

    suspend fun insertAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    suspend fun getAccountById(accountId: Long) = accountDao.getAccountById(accountId)
    fun getAccountByIdLive(accountId: Long) = accountDao.getAccountByIdLive(accountId)
    suspend fun updateAccountBalance(accountId: Long, balance: Double) =
        accountDao.updateBalance(accountId, balance)

    // Category operations
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)
    fun getCategoriesByType(type: CategoryType) = categoryDao.getCategoriesByType(type)
    suspend fun getCategoryById(categoryId: Long) = categoryDao.getCategoryById(categoryId)
    suspend fun getCategoryCount(type: CategoryType) = categoryDao.getCategoryCount(type)

    // Transaction operations
    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insert(transaction)

        // Update account balance for income/expense
        when (transaction.type) {
            TransactionType.INCOME -> {
                val account = accountDao.getAccountById(transaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance + transaction.amount)
                }
            }
            TransactionType.EXPENSE -> {
                val account = accountDao.getAccountById(transaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance - transaction.amount)
                }
            }
            TransactionType.TRANSFER -> {
                // Update both accounts
                val fromAccount = accountDao.getAccountById(transaction.accountId)
                val toAccount = transaction.toAccountId?.let { accountDao.getAccountById(it) }

                fromAccount?.let {
                    accountDao.updateBalance(it.id, it.balance - transaction.amount)
                }
                toAccount?.let {
                    accountDao.updateBalance(it.id, it.balance + transaction.amount)
                }
            }
        }

        return id
    }

    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        // Revert old transaction effect
        when (oldTransaction.type) {
            TransactionType.INCOME -> {
                val account = accountDao.getAccountById(oldTransaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance - oldTransaction.amount)
                }
            }
            TransactionType.EXPENSE -> {
                val account = accountDao.getAccountById(oldTransaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance + oldTransaction.amount)
                }
            }
            TransactionType.TRANSFER -> {
                val fromAccount = accountDao.getAccountById(oldTransaction.accountId)
                val toAccount = oldTransaction.toAccountId?.let { accountDao.getAccountById(it) }

                fromAccount?.let {
                    accountDao.updateBalance(it.id, it.balance + oldTransaction.amount)
                }
                toAccount?.let {
                    accountDao.updateBalance(it.id, it.balance - oldTransaction.amount)
                }
            }
        }

        // Apply new transaction effect
        when (newTransaction.type) {
            TransactionType.INCOME -> {
                val account = accountDao.getAccountById(newTransaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance + newTransaction.amount)
                }
            }
            TransactionType.EXPENSE -> {
                val account = accountDao.getAccountById(newTransaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance - newTransaction.amount)
                }
            }
            TransactionType.TRANSFER -> {
                val fromAccount = accountDao.getAccountById(newTransaction.accountId)
                val toAccount = newTransaction.toAccountId?.let { accountDao.getAccountById(it) }

                fromAccount?.let {
                    accountDao.updateBalance(it.id, it.balance - newTransaction.amount)
                }
                toAccount?.let {
                    accountDao.updateBalance(it.id, it.balance + newTransaction.amount)
                }
            }
        }

        transactionDao.update(newTransaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // Revert transaction effect on account balance
        when (transaction.type) {
            TransactionType.INCOME -> {
                val account = accountDao.getAccountById(transaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance - transaction.amount)
                }
            }
            TransactionType.EXPENSE -> {
                val account = accountDao.getAccountById(transaction.accountId)
                account?.let {
                    accountDao.updateBalance(it.id, it.balance + transaction.amount)
                }
            }
            TransactionType.TRANSFER -> {
                val fromAccount = accountDao.getAccountById(transaction.accountId)
                val toAccount = transaction.toAccountId?.let { accountDao.getAccountById(it) }

                fromAccount?.let {
                    accountDao.updateBalance(it.id, it.balance + transaction.amount)
                }
                toAccount?.let {
                    accountDao.updateBalance(it.id, it.balance - transaction.amount)
                }
            }
        }

        transactionDao.delete(transaction)
    }

    suspend fun getTransactionById(transactionId: Long) =
        transactionDao.getTransactionById(transactionId)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long) =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByAccount(accountId: Long) =
        transactionDao.getTransactionsByAccount(accountId)

    fun getTotalByType(type: TransactionType) =
        transactionDao.getTotalByType(type)

    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long) =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)

    fun getTransactionsByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long) =
        transactionDao.getTransactionsByTypeAndDateRange(type, startDate, endDate)

    fun getCreditCardExpenses(accountId: Long) =
        transactionDao.getCreditCardExpenses(accountId)

    suspend fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal> =
        transactionDao.getCategoryTotals(type, startDate, endDate)

    suspend fun updateSettings(settings: AppSettings) = settingsDao.saveSettings(settings)

    // Budget operations
    val allActiveBudgets: LiveData<List<Budget>> = budgetDao.getAllActiveBudgets()

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insert(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    suspend fun getBudgetById(budgetId: Long): Budget? = budgetDao.getBudgetById(budgetId)

    fun getCurrentBudgets(currentDate: Long): LiveData<List<Budget>> =
        budgetDao.getCurrentBudgets(currentDate)

    suspend fun getActiveBudgetForCategory(categoryId: Long, currentDate: Long): Budget? =
        budgetDao.getActiveBudgetForCategory(categoryId, currentDate)

    suspend fun getOverallBudget(currentDate: Long): Budget? =
        budgetDao.getOverallBudget(currentDate)

    suspend fun deactivateBudget(budgetId: Long) = budgetDao.deactivateBudget(budgetId)

    // Get budget with spending information
    suspend fun getBudgetWithSpending(budget: Budget): BudgetWithSpending {
        val category = budget.categoryId?.let { categoryDao.getCategoryById(it) }

        // Calculate spent amount for the budget period
        val spent = if (budget.categoryId != null) {
            // Category-specific budget
            transactionDao.getTransactionsByTypeAndDateRange(
                TransactionType.EXPENSE,
                budget.startDate,
                budget.endDate
            ).value?.filter { it.categoryId == budget.categoryId }?.sumOf { it.amount } ?: 0.0
        } else {
            // Overall budget
            transactionDao.getTotalByTypeAndDateRange(
                TransactionType.EXPENSE,
                budget.startDate,
                budget.endDate
            ) ?: 0.0
        }

        val remaining = budget.amount - spent
        val percentageUsed = if (budget.amount > 0) {
            ((spent / budget.amount) * 100).toFloat()
        } else {
            0f
        }

        return BudgetWithSpending(
            budget = budget,
            category = category,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentageUsed
        )
    }

    suspend fun getAllBudgetsWithSpending(currentDate: Long): List<BudgetWithSpending> {
        val budgets = budgetDao.getCurrentBudgets(currentDate).value ?: emptyList()
        return budgets.map { getBudgetWithSpending(it) }
    }

}