package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.TransactionType
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val accountName: String?,
    val merchantName: String?,
    val date: Long = System.currentTimeMillis()
)

object SmsParser {

    // Common bank identifiers
    private val bankIdentifiers = listOf(
        "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "YES BANK",
        "PNB", "BOI", "CANARA", "UNION BANK", "IDBI", "BOB",
        "INDUSIND", "PAYTM", "PHONEPE", "GOOGLEPAY", "AMAZONPAY"
    )

    // Transaction type keywords
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "purchase",
        "withdrawn", "withdrawal", "payment", "used"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited",
        "deposit", "refund", "cashback", "salary"
    )

    /**
     * Checks if the SMS is from a bank
     */
    fun isBankSms(sender: String, message: String): Boolean {
        val upperSender = sender.uppercase()
        val upperMessage = message.uppercase()

        // Check if sender or message contains bank identifiers
        return bankIdentifiers.any {
            upperSender.contains(it) || upperMessage.contains(it)
        }
    }

    /**
     * Parses SMS to extract transaction details
     */
    fun parseTransaction(message: String): ParsedTransaction? {
        try {
            val amount = extractAmount(message) ?: return null
            val type = determineTransactionType(message) ?: return null
            val accountName = extractAccountName(message)
            val merchantName = extractMerchantName(message)

            return ParsedTransaction(
                amount = amount,
                type = type,
                accountName = accountName,
                merchantName = merchantName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Extracts amount from SMS
     * Looks for patterns like: Rs.1000, Rs 1000, INR 1000, 1000.00, etc.
     */
    private fun extractAmount(message: String): Double? {
        val patterns = listOf(
            // Rs.1,234.56 or Rs 1,234.56
            Pattern.compile("(?:rs\\.?|inr)\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            // 1,234.56 (standalone numbers with commas/decimals)
            Pattern.compile("\\b([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)\\b"),
            // 1234.56 (without commas)
            Pattern.compile("\\b([0-9]+\\.[0-9]{2})\\b")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }

        return null
    }

    /**
     * Determines if transaction is debit or credit
     */
    private fun determineTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        val hasDebitKeyword = debitKeywords.any { lowerMessage.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerMessage.contains(it) }

        return when {
            hasDebitKeyword && !hasCreditKeyword -> TransactionType.EXPENSE
            hasCreditKeyword && !hasDebitKeyword -> TransactionType.INCOME
            else -> {
                // If both or neither, try to determine from context
                if (lowerMessage.contains("credited") || lowerMessage.contains("received")) {
                    TransactionType.INCOME
                } else if (lowerMessage.contains("debited") || lowerMessage.contains("paid")) {
                    TransactionType.EXPENSE
                } else {
                    null
                }
            }
        }
    }

    /**
     * Extracts account name from SMS
     * Looks for patterns like: A/C XX1234, Card XX1234, etc.
     */
    private fun extractAccountName(message: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:a/c|account|card)\\s*(?:no\\.?|number)?\\s*[xX*]*([0-9]{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:ending|ending with)\\s*([0-9]{4})", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val accountNumber = matcher.group(1)
                return "Account ending $accountNumber"
            }
        }

        // Try to extract bank name
        for (bank in bankIdentifiers) {
            if (message.uppercase().contains(bank)) {
                return bank
            }
        }

        return null
    }

    /**
     * Extracts merchant/payee name from SMS
     */
    private fun extractMerchantName(message: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:at|to|from)\\s+([A-Za-z0-9\\s&-]+?)(?:on|\\.|for|Rs)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:merchant|payee)\\s*:?\\s*([A-Za-z0-9\\s&-]+?)(?:\\.|for|on)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 2 && merchant.length < 50) {
                    return merchant
                }
            }
        }

        return null
    }
}