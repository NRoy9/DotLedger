package com.nitin.dotledger.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.utils.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Check if SMS reading is enabled
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val settings = database.settingsDao().getSettingsSync()

                if (settings?.enableSmsReading != true) {
                    Log.d(TAG, "SMS reading is disabled")
                    return@launch
                }

                // Extract SMS messages
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                for (smsMessage in messages) {
                    processSms(context, smsMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private suspend fun processSms(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.displayOriginatingAddress ?: return
        val message = smsMessage.messageBody ?: return

        Log.d(TAG, "Processing SMS from: $sender")
        Log.d(TAG, "Message: $message")

        // Check if it's a bank SMS
        if (!SmsParser.isBankSms(sender)) {
            Log.d(TAG, "Not a bank SMS, ignoring")
            return
        }

        // Parse transaction
        val parsedTransaction = SmsParser.parseTransaction(sender, message)
        if (parsedTransaction == null) {
            Log.d(TAG, "Could not parse transaction from SMS")
            return
        }

        // Get database
        val database = AppDatabase.getDatabase(context)

        // Try to match account by last 4 digits
        val accountNumber = SmsParser.extractAccountNumber(message)
        val accounts = database.accountDao().getAllAccounts().value ?: emptyList()

        // Use first account if can't match
        val account = if (accountNumber != null) {
            accounts.firstOrNull { it.name.contains(accountNumber.takeLast(4)) }
        } else {
            null
        } ?: accounts.firstOrNull()

        if (account == null) {
            Log.d(TAG, "No account found to assign transaction")
            return
        }

        // Find or create "SMS Auto" category
        val categoryDao = database.categoryDao()
        val categoryType = when (parsedTransaction.type) {
            com.nitin.dotledger.data.entities.TransactionType.EXPENSE ->
                com.nitin.dotledger.data.entities.CategoryType.EXPENSE
            com.nitin.dotledger.data.entities.TransactionType.INCOME ->
                com.nitin.dotledger.data.entities.CategoryType.INCOME
            else -> com.nitin.dotledger.data.entities.CategoryType.EXPENSE
        }

        val categories = categoryDao.getCategoriesByType(categoryType).value ?: emptyList()

        var category = categories.firstOrNull { it.name == "SMS Auto" }
        if (category == null) {
            // Create SMS Auto category
            val newCategory = com.nitin.dotledger.data.entities.Category(
                name = "SMS Auto",
                type = categoryType,
                colorCode = "#FFA500",
                isDefault = false
            )
            val categoryId = categoryDao.insert(newCategory)
            category = newCategory.copy(id = categoryId)
        }

        // Create transaction
        val transaction = Transaction(
            accountId = account.id,
            categoryId = category.id,
            amount = parsedTransaction.amount,
            type = parsedTransaction.type,
            date = System.currentTimeMillis(),
            note = "Auto: ${parsedTransaction.note}",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        // Insert transaction (this will also update account balance)
        val transactionDao = database.transactionDao()
        transactionDao.insert(transaction)

        // Update account balance if available in SMS
        parsedTransaction.currentBalance?.let { balance ->
            database.accountDao().updateBalance(account.id, balance)
        }

        Log.d(TAG, "Transaction created successfully: ${parsedTransaction.amount}")

        // Show notification (optional)
        showNotification(
            context,
            parsedTransaction.amount,
            parsedTransaction.type,
            account.name
        )
    }

    private fun showNotification(
        context: Context,
        amount: Double,
        type: com.nitin.dotledger.data.entities.TransactionType,
        accountName: String
    ) {
        // Optional: Show a notification that transaction was auto-created
        // Implementation can be added here if needed
        Log.d(TAG, "Would show notification: $type of ₹$amount in $accountName")
    }
}