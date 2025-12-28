package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

object CurrencyFormatter {

    fun format(amount: Double, settings: AppSettings?): String {
        if (settings == null) {
            return "₹${formatNumber(amount, NumberFormat.INDIAN)}"
        }

        return "${settings.currencySymbol}${formatNumber(amount, settings.numberFormat)}"
    }

    private fun formatNumber(amount: Double, format: NumberFormat): String {
        val symbols = DecimalFormatSymbols()

        return when (format) {
            NumberFormat.INDIAN -> {
                // Indian format: 12,34,567.00
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'

                val formatter = DecimalFormat("#,##,##0.00", symbols)
                formatter.format(amount)
            }
            NumberFormat.INTERNATIONAL -> {
                // International format: 1,234,567.00
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'

                val formatter = DecimalFormat("#,###.00", symbols)
                formatter.format(amount)
            }
            NumberFormat.EUROPEAN -> {
                // European format: 1.234.567,00
                symbols.groupingSeparator = '.'
                symbols.decimalSeparator = ','

                val formatter = DecimalFormat("#,###.00", symbols)
                formatter.format(amount)
            }
        }
    }
}