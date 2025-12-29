package com.nitin.dotledger.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g., "Monthly Rent", "Weekly Groceries"
    val accountId: Long,
    val categoryId: Long?,
    val amount: Double,
    val type: TransactionType,
    val frequency: RecurringFrequency,
    val startDate: Long, // First occurrence date
    val endDate: Long? = null, // Optional end date, null = indefinite
    val nextOccurrence: Long, // Next scheduled date
    val lastExecuted: Long? = null, // Last time it was executed
    val note: String = "",
    val toAccountId: Long? = null, // For transfers
    val isActive: Boolean = true,
    val dayOfMonth: Int? = null, // For MONTHLY (1-31)
    val dayOfWeek: Int? = null, // For WEEKLY (1-7, where 1=Monday)
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

enum class RecurringFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly");

    fun getNextOccurrence(currentDate: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentDate

        when (this) {
            DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            BIWEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 2)
            MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            QUARTERLY -> calendar.add(java.util.Calendar.MONTH, 3)
            YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }

        return calendar.timeInMillis
    }
}