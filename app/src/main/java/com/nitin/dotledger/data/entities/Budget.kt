package com.nitin.dotledger.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?, // null for overall budget
    val amount: Double,
    val period: BudgetPeriod, // MONTHLY, YEARLY
    val startDate: Long, // Start of budget period
    val endDate: Long, // End of budget period
    val isActive: Boolean = true,
    val alertPercentage: Int = 80, // Alert when 80% of budget is used
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

enum class BudgetPeriod {
    MONTHLY,
    YEARLY
}

// Data class for budget with spending info
data class BudgetWithSpending(
    val budget: Budget,
    val category: Category?,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Float
) {
    val isOverBudget: Boolean
        get() = spent > budget.amount

    val shouldAlert: Boolean
        get() = percentageUsed >= budget.alertPercentage
}