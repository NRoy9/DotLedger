package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.models.TransactionFilter
import java.text.SimpleDateFormat
import java.util.*

object FilterSummaryHelper {

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    /**
     * Generate a human-readable summary of active filters
     */
    fun generateSummary(
        filter: TransactionFilter,
        accounts: List<Account>? = null,
        categories: List<Category>? = null
    ): String {
        if (!filter.hasActiveFilters()) {
            return "No filters applied"
        }

        val parts = mutableListOf<String>()

        // Search query
        if (filter.searchQuery.isNotEmpty()) {
            parts.add("\"${filter.searchQuery}\"")
        }

        // Transaction types
        if (filter.types.isNotEmpty()) {
            val typeNames = filter.types.map { it.name.lowercase().capitalize() }
            parts.add(typeNames.joinToString("/"))
        }

        // Accounts
        if (filter.accountIds.isNotEmpty() && accounts != null) {
            val accountNames = accounts
                .filter { filter.accountIds.contains(it.id) }
                .map { it.name }

            if (accountNames.size <= 2) {
                parts.add(accountNames.joinToString(", "))
            } else {
                parts.add("${accountNames.size} accounts")
            }
        } else if (filter.accountIds.isNotEmpty()) {
            parts.add("${filter.accountIds.size} accounts")
        }

        // Categories
        if (filter.categoryIds.isNotEmpty() && categories != null) {
            val categoryNames = categories
                .filter { filter.categoryIds.contains(it.id) }
                .map { it.name }

            if (categoryNames.size <= 2) {
                parts.add(categoryNames.joinToString(", "))
            } else {
                parts.add("${categoryNames.size} categories")
            }
        } else if (filter.categoryIds.isNotEmpty()) {
            parts.add("${filter.categoryIds.size} categories")
        }

        // Date range
        filter.dateRange?.let { range ->
            val start = dateFormat.format(Date(range.startDate))
            val end = dateFormat.format(Date(range.endDate))
            parts.add("$start - $end")
        }

        // Amount range
        filter.amountRange?.let { range ->
            if (range.minAmount > 0 && range.maxAmount < Double.MAX_VALUE) {
                parts.add("₹${range.minAmount.toInt()}-${range.maxAmount.toInt()}")
            } else if (range.minAmount > 0) {
                parts.add(">₹${range.minAmount.toInt()}")
            } else if (range.maxAmount < Double.MAX_VALUE) {
                parts.add("<₹${range.maxAmount.toInt()}")
            }
        }

        return parts.joinToString(" • ")
    }

    /**
     * Count total active filter criteria
     */
    fun countActiveFilters(filter: TransactionFilter): Int {
        var count = 0

        if (filter.searchQuery.isNotEmpty()) count++
        if (filter.types.isNotEmpty()) count++
        if (filter.accountIds.isNotEmpty()) count++
        if (filter.categoryIds.isNotEmpty()) count++
        if (filter.dateRange != null) count++
        if (filter.amountRange != null) count++

        return count
    }

    /**
     * Get a brief description of filter
     */
    fun getFilterBadge(filter: TransactionFilter): String {
        val count = countActiveFilters(filter)
        return when {
            count == 0 -> "All"
            count == 1 -> "1 filter"
            else -> "$count filters"
        }
    }
}