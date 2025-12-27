package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.TransactionType
import java.util.regex.Pattern

object SmsParser {

    // Common bank sender IDs
    private val BANK_SENDERS = setOf(
        "HDFCBK", "ICICIB", "SBIIN", "AXISBK", "KOTAKB", "PNBSMS",
        "SCBANK", "BOIIND", "CITIBANK", "INDBNK", "YESBNK", "IDFCFB", "FEDBNK"
    )

    // Keywords for transaction types
    private val DEBIT_KEYWORDS = setOf(
        "debited", "debit", "withdrawn", "spent", "paid", "purchase", "payment"
    )

    private val CREDIT_KEYWORDS = setOf(
        "credited", "credit", "received", "deposited", "refund", "cashback"
    )

    /**
     * Check if SMS is from a bank
     */
    fun isBankSms(sender: String): Boolean {
        return BANK_SENDERS.any { sender.contains(it, ignoreCase = true) }
    }

    /**
     * Parse transaction from SMS
     */
    fun parseTransaction(sender: String, message: String): ParsedTransaction? {
        if (!isBankSms(sender)) return null

        val amount = extractAmount(message) ?: return null
        val type = determineTransactionType(message) ?: return null
        val merchantOrNote = extractMerchantOrNote(message)
        val balance = extractBalance(message)

        return ParsedTransaction(
            amount = amount,
            type = type,
            note = merchantOrNote,
            currentBalance = balance
        )
    }

    /**
     * Extract amount from SMS
     */
    private fun extractAmount(message: String): Double? {
        // Pattern to match amounts like Rs.1000, INR 1000, 1,000.00
        val patterns = listOf(
            Pattern.compile("(?:Rs\\.?|INR)\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:debited|credited|paid|received|withdrawn|deposited)\\s+(?:Rs\\.?|INR)?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:amount|amt)\\s+(?:Rs\\.?|INR)?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                return amountStr.toDoubleOrNull()
            }
        }

        return null
    }

    /**
     * Determine transaction type (debit/credit)
     */
    private fun determineTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            DEBIT_KEYWORDS.any { lowerMessage.contains(it) } -> TransactionType.EXPENSE
            CREDIT_KEYWORDS.any { lowerMessage.contains(it) } -> TransactionType.INCOME
            else -> null
        }
    }

    /**
     * Extract merchant name or note
     */
    private fun extractMerchantOrNote(message: String): String {
        // Try to extract merchant name
        val patterns = listOf(
            Pattern.compile("at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|\\,)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("to\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|\\,)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("from\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|\\,)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        // If no merchant found, return first 50 chars of message as note
        return message.take(50)
    }

    /**
     * Extract current balance from SMS
     */
    private fun extractBalance(message: String): Double? {
        // Pattern to match balance like "Avl Bal: Rs.5000", "Balance: INR 5000.00"
        val pattern = Pattern.compile(
            "(?:balance|bal|avl\\s*bal)[:\\s]+(?:Rs\\.?|INR)?\\s*([0-9,]+\\.?[0-9]*)",
            Pattern.CASE_INSENSITIVE
        )

        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            val balanceStr = matcher.group(1)?.replace(",", "") ?: return null
            return balanceStr.toDoubleOrNull()
        }

        return null
    }

    /**
     * Extract account number (last 4 digits usually shown)
     */
    fun extractAccountNumber(message: String): String? {
        val pattern = Pattern.compile("(?:A/c|Account|Card)\\s*(?:no\\.?)?\\s*[xX*]{2,}([0-9]{4})", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(message)

        return if (matcher.find()) {
            "****${matcher.group(1)}"
        } else {
            null
        }
    }

    /**
     * Get transaction date from SMS timestamp (if available)
     */
    fun extractTransactionDate(message: String): Long? {
        // Most SMS show transaction date, but we'll use SMS received time as fallback
        // This can be enhanced to parse date strings from SMS
        return null // Will use System.currentTimeMillis() as default
    }
}

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val note: String,
    val currentBalance: Double? = null
)