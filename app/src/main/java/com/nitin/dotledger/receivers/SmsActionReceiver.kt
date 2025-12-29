package com.nitin.dotledger.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.entities.TransactionType
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.ACTION_APPROVE
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.ACTION_DISMISS
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_ACCOUNT_INFO
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_AMOUNT
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_DATE
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_MERCHANT
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_SMS_BODY
import com.nitin.dotledger.receivers.SmsBroadcastReceiver.Companion.EXTRA_TYPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_APPROVE -> {
                handleApprove(context, intent)
            }
            ACTION_DISMISS -> {
                handleDismiss(context, notificationId)
            }
        }

        // Cancel notification
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }

    private fun handleApprove(context: Context, intent: Intent) {
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: return
        val merchant = intent.getStringExtra(EXTRA_MERCHANT)
        val accountInfo = intent.getStringExtra(EXTRA_ACCOUNT_INFO)
        val date = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())
        val smsBody = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""

        val type = try {
            TransactionType.valueOf(typeStr)
        } catch (e: Exception) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val accountDao = database.accountDao()
                val categoryDao = database.categoryDao()
                val transactionDao = database.transactionDao()

                // Get first available account (or create default if none exists)
                val accounts = accountDao.getAllAccounts().value
                val account = if (!accounts.isNullOrEmpty()) {
                    // Try to match account by accountInfo
                    if (!accountInfo.isNullOrBlank()) {
                        accounts.find { it.name.contains(accountInfo, ignoreCase = true) }
                            ?: accounts.first()
                    } else {
                        accounts.first()
                    }
                } else {
                    // No accounts - show error
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Please add an account first",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Get appropriate category
                val categoryType = if (type == TransactionType.EXPENSE) {
                    com.nitin.dotledger.data.entities.CategoryType.EXPENSE
                } else {
                    com.nitin.dotledger.data.entities.CategoryType.INCOME
                }

                val categories = categoryDao.getCategoriesByType(categoryType).value
                val category = if (!categories.isNullOrEmpty()) {
                    // Try to match category by merchant name
                    if (!merchant.isNullOrBlank()) {
                        // Simple matching - can be improved
                        categories.find {
                            merchant.contains(it.name, ignoreCase = true) ||
                                    it.name.contains(merchant, ignoreCase = true)
                        } ?: categories.last() // Default to "Others"
                    } else {
                        categories.last()
                    }
                } else {
                    null
                }

                // Create transaction
                val note = buildString {
                    if (!merchant.isNullOrBlank()) {
                        append(merchant)
                    }
                    if (!accountInfo.isNullOrBlank()) {
                        if (isNotEmpty()) append(" - ")
                        append("A/C: $accountInfo")
                    }
                    if (isEmpty()) {
                        append("Auto-added from SMS")
                    }
                }

                val transaction = Transaction(
                    accountId = account.id,
                    categoryId = category?.id,
                    amount = amount,
                    type = type,
                    date = date,
                    note = note
                )

                // Insert transaction (this will also update account balance)
                val repository = com.nitin.dotledger.data.repository.DotLedgerRepository(
                    accountDao,
                    categoryDao,
                    transactionDao
                )
                repository.insertTransaction(transaction)

                // Show success message
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Transaction added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Error adding transaction: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleDismiss(context: Context, notificationId: Int) {
        // Just dismiss - notification is already cancelled above
        Toast.makeText(
            context,
            "Transaction dismissed",
            Toast.LENGTH_SHORT
        ).show()
    }
}