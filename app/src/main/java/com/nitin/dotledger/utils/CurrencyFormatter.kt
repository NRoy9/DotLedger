package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object CurrencyFormatter {

    /**
     * Format amount based on app settings
     */
    fun format(amount: Double, settings: AppSettings): String {
        val symbols = DecimalFormatSymbols()

        val pattern = when (settings.numberFormat) {
            NumberFormat.INDIAN -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                "##,##,###.00"
            }
            NumberFormat.INTERNATIONAL -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                "###,###,###.00"
            }
            NumberFormat.EUROPEAN -> {
                symbols.groupingSeparator = '.'
                symbols.decimalSeparator = ','
                "###.###.###,00"
            }
        }

        val formatter = DecimalFormat(pattern, symbols)
        val formattedAmount = formatter.format(amount)

        return "${settings.currencySymbol}$formattedAmount"
    }

    /**
     * Format amount with default INR settings (for backwards compatibility)
     */
    fun formatINR(amount: Double): String {
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val formatter = DecimalFormat("##,##,###.00", symbols)
        return "₹${formatter.format(amount)}"
    }

    /**
     * Get currency symbol from settings
     */
    fun getSymbol(settings: AppSettings): String {
        return settings.currencySymbol
    }

    /**
     * Format amount without currency symbol
     */
    fun formatWithoutSymbol(amount: Double, settings: AppSettings): String {
        val symbols = DecimalFormatSymbols()

        val pattern = when (settings.numberFormat) {
            NumberFormat.INDIAN -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                "##,##,###.00"
            }
            NumberFormat.INTERNATIONAL -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                "###,###,###.00"
            }
            NumberFormat.EUROPEAN -> {
                symbols.groupingSeparator = '.'
                symbols.decimalSeparator = ','
                "###.###.###,00"
            }
        }

        val formatter = DecimalFormat(pattern, symbols)
        return formatter.format(amount)
    }

    /**
     * Parse formatted string back to double
     */
    fun parse(formattedAmount: String, settings: AppSettings): Double? {
        return try {
            val cleanString = formattedAmount
                .replace(settings.currencySymbol, "")
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")

            // Handle decimal separator
            val decimalSeparator = when (settings.numberFormat) {
                NumberFormat.EUROPEAN -> ","
                else -> "."
            }

            cleanString.replace(decimalSeparator, ".").toDouble()
        } catch (e: Exception) {
            null
        }
    }
}