package com.nitin.dotledger.utils

import android.content.Context
import android.net.Uri
import com.nitin.dotledger.data.entities.*
import com.opencsv.CSVReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object ImportUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    data class ImportResult(
        val accounts: List<Account>,
        val categories: List<Category>,
        val transactions: List<Transaction>
    )

    fun importFromCSV(context: Context, uri: Uri): ImportResult {
        val accounts = mutableListOf<Account>()
        val categories = mutableListOf<Category>()
        val transactions = mutableListOf<Transaction>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReader(InputStreamReader(inputStream))
            var currentSection = ""
            var skipHeader = false

            reader.readAll().forEach { line ->
                when {
                    line.isEmpty() -> return@forEach
                    line[0].startsWith("===") -> {
                        currentSection = line[0]
                        skipHeader = true
                    }
                    skipHeader -> {
                        skipHeader = false
                    }
                    currentSection.contains("ACCOUNTS") -> {
                        parseAccount(line)?.let { accounts.add(it) }
                    }
                    currentSection.contains("CATEGORIES") -> {
                        parseCategory(line)?.let { categories.add(it) }
                    }
                    currentSection.contains("TRANSACTIONS") -> {
                        parseTransaction(line)?.let { transactions.add(it) }
                    }
                }
            }

            reader.close()
        }

        return ImportResult(accounts, categories, transactions)
    }

    private fun parseAccount(line: Array<String>): Account? {
        return try {
            Account(
                id = line[0].toLong(),
                name = line[1],
                type = AccountType.valueOf(line[2]),
                balance = line[3].toDouble(),
                isActive = line[4].toBoolean(),
                createdAt = dateFormat.parse(line[5])?.time ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCategory(line: Array<String>): Category? {
        return try {
            Category(
                id = line[0].toLong(),
                name = line[1],
                type = CategoryType.valueOf(line[2]),
                isDefault = line[3].toBoolean(),
                colorCode = line[4],
                createdAt = dateFormat.parse(line[5])?.time ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTransaction(line: Array<String>): Transaction? {
        return try {
            Transaction(
                id = line[0].toLong(),
                accountId = line[1].toLong(),
                categoryId = line[2].toLongOrNull(),
                amount = line[3].toDouble(),
                type = TransactionType.valueOf(line[4]),
                date = dateFormat.parse(line[5])?.time ?: System.currentTimeMillis(),
                note = line[6],
                toAccountId = line[7].toLongOrNull(),
                createdAt = dateFormat.parse(line[8])?.time ?: System.currentTimeMillis(),
                modifiedAt = dateFormat.parse(line[9])?.time ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}