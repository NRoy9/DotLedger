package com.nitin.dotledger.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // Always use ID 1 for singleton settings
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val numberFormat: NumberFormat = NumberFormat.INDIAN,
    val defaultTransactionType: TransactionType = TransactionType.EXPENSE,
    val isDarkTheme: Boolean = true
)

enum class NumberFormat {
    INDIAN,      // 12,34,567.00
    INTERNATIONAL, // 1,234,567.00
    EUROPEAN     // 1.234.567,00
}

data class Currency(
    val code: String,
    val symbol: String,
    val name: String
)

object CurrencyList {
    val currencies = listOf(
        Currency("INR", "₹", "Indian Rupee"),
        Currency("USD", "$", "US Dollar"),
        Currency("EUR", "€", "Euro"),
        Currency("GBP", "£", "British Pound"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("AUD", "A$", "Australian Dollar"),
        Currency("CAD", "C$", "Canadian Dollar"),
        Currency("CHF", "CHF", "Swiss Franc"),
        Currency("CNY", "¥", "Chinese Yuan"),
        Currency("SGD", "S$", "Singapore Dollar"),
        Currency("AED", "د.إ", "UAE Dirham"),
        Currency("SAR", "﷼", "Saudi Riyal")
    )
}