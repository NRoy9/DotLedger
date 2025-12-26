package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object FormatterUtils {
    fun formatCurrency(amount: Double, format: NumberFormat, symbol: String): String {
        val symbols = DecimalFormatSymbols()

        val pattern = when (format) {
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
        return "$symbol${formatter.format(amount)}"
    }
}