package com.nitin.dotledger.data.models

import com.nitin.dotledger.data.entities.TransactionType

data class TransactionFilter(
    val searchQuery: String = "",
    val types: Set<TransactionType> = emptySet(),
    val accountIds: Set<Long> = emptySet(),
    val categoryIds: Set<Long> = emptySet(),
    val dateRange: DateRange? = null,
    val amountRange: AmountRange? = null,
    val sortBy: SortOption = SortOption.DATE_DESC
) {
    fun isEmpty(): Boolean {
        return searchQuery.isEmpty() &&
                types.isEmpty() &&
                accountIds.isEmpty() &&
                categoryIds.isEmpty() &&
                dateRange == null &&
                amountRange == null
    }

    fun hasActiveFilters(): Boolean = !isEmpty()
}

data class DateRange(
    val startDate: Long,
    val endDate: Long
) {
    companion object {
        fun today(): DateRange {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            return DateRange(start, end)
        }

        fun thisWeek(): DateRange {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_YEAR, 6)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            return DateRange(start, end)
        }

        fun thisMonth(): DateRange {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            calendar.set(
                java.util.Calendar.DAY_OF_MONTH,
                calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            )
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            return DateRange(start, end)
        }

        fun last30Days(): DateRange {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            return DateRange(start, end)
        }

        fun custom(startDate: Long, endDate: Long): DateRange {
            return DateRange(startDate, endDate)
        }
    }
}

data class AmountRange(
    val minAmount: Double = 0.0,
    val maxAmount: Double = Double.MAX_VALUE
) {
    fun contains(amount: Double): Boolean {
        return amount >= minAmount && amount <= maxAmount
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    AMOUNT_DESC("Amount (Highest First)"),
    AMOUNT_ASC("Amount (Lowest First)"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)")
}

enum class QuickFilter(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM("Custom Range")
}