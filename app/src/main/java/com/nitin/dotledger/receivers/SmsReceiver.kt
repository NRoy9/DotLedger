package com.nitin.dotledger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.utils.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody

                // Check if it's a bank SMS
                if (SmsParser.isBankSms(sender, messageBody)) {
                    processBankSms(context, sender, messageBody)
                }
            }
        }
    }

    private fun processBankSms(context: Context, sender: String, message: String) {
        val parsedTransaction = SmsParser.parseTransaction(message)

        if (parsedTransaction != null) {
            // Show notification to user
            showTransactionNotification(context, parsedTransaction)

            // Optionally auto-save to database
            // autoSaveTransaction(context, parsedTransaction)
        }
    }

    private fun showTransactionNotification(context: Context, parsed: com.nitin.dotledger.utils.ParsedTransaction) {
        val typeText = if (parsed.type == com.nitin.dotledger.data.entities.TransactionType.INCOME) "Credit" else "Debit"
        val message = "$typeText of ${parsed.amount} ${parsed.merchantName?.let { "at $it" } ?: ""}"

        Toast.makeText(context, "Transaction detected: $message", Toast.LENGTH_LONG).show()
    }

    private fun autoSaveTransaction(context: Context, parsed: com.nitin.dotledger.utils.ParsedTransaction) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val accountDao = database.accountDao()

                // Get first available account or create a default one
                val accounts = accountDao.getAllAccounts()
                // Note: This is LiveData, you might need to handle this differently

                // For now, we'll just show a toast
                // In production, you'd want to show a dialog for user to confirm
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}