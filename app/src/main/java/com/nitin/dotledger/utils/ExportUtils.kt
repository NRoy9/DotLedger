package com.nitin.dotledger.utils

import android.content.Context
import android.net.Uri
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.Transaction
import com.opencsv.CSVWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToCSV(
        context: Context,
        uri: Uri,
        accounts: List<Account>,
        categories: List<Category>,
        transactions: List<Transaction>
    ) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))

            // Export format info
            writer.writeNext(arrayOf("DotLedger Export", "Version 1.0"))
            writer.writeNext(arrayOf("Export Date", dateFormat.format(Date())))
            writer.writeNext(arrayOf()) // Empty line

            // Export Accounts
            writer.writeNext(arrayOf("=== ACCOUNTS ==="))
            writer.writeNext(arrayOf("ID", "Name", "Type", "Balance", "Active", "Created"))

            accounts.forEach { account ->
                writer.writeNext(arrayOf(
                    account.id.toString(),
                    account.name,
                    account.type.name,
                    account.balance.toString(),
                    account.isActive.toString(),
                    dateFormat.format(Date(account.createdAt))
                ))
            }
            writer.writeNext(arrayOf()) // Empty line

            // Export Categories
            writer.writeNext(arrayOf("=== CATEGORIES ==="))
            writer.writeNext(arrayOf("ID", "Name", "Type", "IsDefault", "ColorCode", "Created"))

            categories.forEach { category ->
                writer.writeNext(arrayOf(
                    category.id.toString(),
                    category.name,
                    category.type.name,
                    category.isDefault.toString(),
                    category.colorCode,
                    dateFormat.format(Date(category.createdAt))
                ))
            }
            writer.writeNext(arrayOf()) // Empty line

            // Export Transactions
            writer.writeNext(arrayOf("=== TRANSACTIONS ==="))
            writer.writeNext(arrayOf(
                "ID", "AccountID", "CategoryID", "Amount", "Type",
                "Date", "Note", "ToAccountID", "Created", "Modified"
            ))

            transactions.forEach { transaction ->
                writer.writeNext(arrayOf(
                    transaction.id.toString(),
                    transaction.accountId.toString(),
                    transaction.categoryId?.toString() ?: "",
                    transaction.amount.toString(),
                    transaction.type.name,
                    dateFormat.format(Date(transaction.date)),
                    transaction.note,
                    transaction.toAccountId?.toString() ?: "",
                    dateFormat.format(Date(transaction.createdAt)),
                    dateFormat.format(Date(transaction.modifiedAt))
                ))
            }

            writer.close()
        }
    }

    fun exportSummaryReport(
        context: Context,
        uri: Uri,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        startDate: Long,
        endDate: Long
    ) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))

            val filteredTransactions = transactions.filter {
                it.date >= startDate && it.date <= endDate
            }

            // Header
            writer.writeNext(arrayOf("DotLedger Summary Report"))
            writer.writeNext(arrayOf(
                "Period",
                "${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}"
            ))
            writer.writeNext(arrayOf())

            // Summary
            val totalIncome = filteredTransactions
                .filter { it.type.name == "INCOME" }
                .sumOf { it.amount }

            val totalExpense = filteredTransactions
                .filter { it.type.name == "EXPENSE" }
                .sumOf { it.amount }

            val totalBalance = accounts.sumOf { it.balance }

            writer.writeNext(arrayOf("=== SUMMARY ==="))
            writer.writeNext(arrayOf("Total Income", totalIncome.toString()))
            writer.writeNext(arrayOf("Total Expense", totalExpense.toString()))
            writer.writeNext(arrayOf("Net Flow", (totalIncome - totalExpense).toString()))
            writer.writeNext(arrayOf("Current Balance", totalBalance.toString()))
            writer.writeNext(arrayOf())

            // Account Balances
            writer.writeNext(arrayOf("=== ACCOUNT BALANCES ==="))
            writer.writeNext(arrayOf("Account", "Type", "Balance"))
            accounts.forEach { account ->
                writer.writeNext(arrayOf(
                    account.name,
                    account.type.name,
                    account.balance.toString()
                ))
            }
            writer.writeNext(arrayOf())

            // Category Breakdown
            writer.writeNext(arrayOf("=== EXPENSE BY CATEGORY ==="))
            writer.writeNext(arrayOf("Category", "Amount"))

            val expensesByCategory = filteredTransactions
                .filter { it.type.name == "EXPENSE" && it.categoryId != null }
                .groupBy { it.categoryId }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .entries
                .sortedByDescending { it.value }

            expensesByCategory.forEach { (categoryId, total) ->
                val category = categories.find { it.id == categoryId }
                writer.writeNext(arrayOf(
                    category?.name ?: "Unknown",
                    total.toString()
                ))
            }
            writer.writeNext(arrayOf())

            writer.writeNext(arrayOf("=== INCOME BY CATEGORY ==="))
            writer.writeNext(arrayOf("Category", "Amount"))

            val incomesByCategory = filteredTransactions
                .filter { it.type.name == "INCOME" && it.categoryId != null }
                .groupBy { it.categoryId }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .entries
                .sortedByDescending { it.value }

            incomesByCategory.forEach { (categoryId, total) ->
                val category = categories.find { it.id == categoryId }
                writer.writeNext(arrayOf(
                    category?.name ?: "Unknown",
                    total.toString()
                ))
            }

            writer.close()
        }
    }
}