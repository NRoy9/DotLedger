package com.nitin.dotledger.utils

import com.nitin.dotledger.data.entities.TransactionType
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val merchantName: String?,
    val accountInfo: String?,
    val date: Long = System.currentTimeMillis(),
    val confidence: Float // 0.0 to 1.0
)

object SmsParser {

    // Common keywords for transaction identification
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "purchase", "withdrawn",
        "payment", "transaction", "used", "bought", "charged"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "refund",
        "cashback", "reward", "salary", "transfer in"
    )

    private val BANK_KEYWORDS = listOf(
        "bank", "hdfc", "icici", "sbi", "axis", "kotak", "paytm",
        "phonepe", "gpay", "googlepay", "amazon", "flipkart"
    )

    // Regex patterns for amount extraction
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:amount|amt)\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([0-9,]+(?:\\.[0-9]{2})?)\\s*(?:rs|inr|₹)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:debited|credited|paid|received)\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    )

    // Regex patterns for merchant/account extraction
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?:at|to|from)\\s+([A-Z][A-Z0-9\\s&-]+?)(?:\\s+on|\\s+dated|\\.|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:merchant|vendor)\\s*:?\\s*([A-Z][A-Z0-9\\s&-]+?)(?:\\s+on|\\s+dated|\\.|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:A/C|account|card)\\s*(?:ending|no\\.?)\\s*([X0-9]{4,})", Pattern.CASE_INSENSITIVE)
    )

    // Date patterns
    private val DATE_PATTERNS = listOf(
        Pattern.compile("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{2,4})"),
        Pattern.compile("(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{2,4})", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Parse SMS message and extract transaction details
     */
    fun parseSms(message: String, sender: String): ParsedTransaction? {
        val normalizedMsg = message.toLowerCase(Locale.getDefault())

        // Check if it's a banking/transaction SMS
        if (!isBankingSms(normalizedMsg, sender)) {
            return null
        }

        // Extract transaction type
        val type = extractTransactionType(normalizedMsg) ?: return null

        // Extract amount
        val amount = extractAmount(message) ?: return null

        // Extract merchant/account info
        val merchantName = extractMerchant(message)
        val accountInfo = extractAccountInfo(message)

        // Extract date
        val date = extractDate(message) ?: System.currentTimeMillis()

        // Calculate confidence score
        val confidence = calculateConfidence(normalizedMsg, amount, type, merchantName)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchantName = merchantName,
            accountInfo = accountInfo,
            date = date,
            confidence = confidence
        )
    }

    /**
     * Check if SMS is from a bank or payment service
     */
    private fun isBankingSms(message: String, sender: String): Boolean {
        // Check sender
        val senderLower = sender.toLowerCase(Locale.getDefault())
        if (BANK_KEYWORDS.any { senderLower.contains(it) }) {
            return true
        }

        // Check message content
        if (BANK_KEYWORDS.any { message.contains(it) }) {
            return true
        }

        // Check for common banking SMS patterns
        return message.contains("a/c") ||
                message.contains("account") ||
                message.contains("card") ||
                message.contains("upi")
    }

    /**
     * Determine transaction type (DEBIT/CREDIT)
     */
    private fun extractTransactionType(message: String): TransactionType? {
        val hasDebit = DEBIT_KEYWORDS.any { message.contains(it) }
        val hasCredit = CREDIT_KEYWORDS.any { message.contains(it) }

        return when {
            hasDebit && !hasCredit -> TransactionType.EXPENSE
            hasCredit && !hasDebit -> TransactionType.INCOME
            else -> null // Ambiguous
        }
    }

    /**
     * Extract amount from SMS
     */
    private fun extractAmount(message: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                try {
                    val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                    return amountStr.toDouble()
                } catch (e: NumberFormatException) {
                    continue
                }
            }
        }
        return null
    }

    /**
     * Extract merchant/vendor name
     */
    private fun extractMerchant(message: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 2) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        return null
    }

    /**
     * Extract account information
     */
    private fun extractAccountInfo(message: String): String? {
        val accountPattern = Pattern.compile("(?:A/C|account|card)\\s*(?:ending|no\\.?)\\s*([X0-9]{4,})", Pattern.CASE_INSENSITIVE)
        val matcher = accountPattern.matcher(message)

        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }

        return null
    }

    /**
     * Extract transaction date
     */
    private fun extractDate(message: String): Long? {
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                try {
                    val dateStr = matcher.group(0)
                    val format = if (dateStr!!.contains("-") || dateStr.contains("/")) {
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    } else {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    }
                    return format.parse(dateStr)?.time
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    /**
     * Clean merchant name
     */
    private fun cleanMerchantName(merchant: String): String {
        var cleaned = merchant.trim()

        // Remove common suffixes
        val suffixesToRemove = listOf("PVT", "LTD", "LIMITED", "INC", "CORP", "CO")
        for (suffix in suffixesToRemove) {
            cleaned = cleaned.replace(suffix, "", ignoreCase = true)
        }

        // Capitalize properly
        cleaned = cleaned.split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }

        return cleaned.trim()
    }

    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(
        message: String,
        amount: Double?,
        type: TransactionType?,
        merchant: String?
    ): Float {
        var score = 0.0f

        // Amount extracted
        if (amount != null && amount > 0) {
            score += 0.4f
        }

        // Transaction type identified
        if (type != null) {
            score += 0.3f
        }

        // Merchant identified
        if (!merchant.isNullOrBlank()) {
            score += 0.2f
        }

        // Has banking keywords
        if (BANK_KEYWORDS.any { message.contains(it) }) {
            score += 0.1f
        }

        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Format parsed transaction for display
     */
    fun formatTransactionSummary(parsed: ParsedTransaction): String {
        val typeStr = if (parsed.type == TransactionType.EXPENSE) "Expense" else "Income"
        val amountStr = "₹${String.format("%.2f", parsed.amount)}"

        return buildString {
            append("$typeStr of $amountStr")
            if (!parsed.merchantName.isNullOrBlank()) {
                append("\nat ${parsed.merchantName}")
            }
            if (!parsed.accountInfo.isNullOrBlank()) {
                append("\nAccount: ${parsed.accountInfo}")
            }
            append("\nConfidence: ${(parsed.confidence * 100).toInt()}%")
        }
    }
}