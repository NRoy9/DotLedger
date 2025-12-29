package com.nitin.dotledger.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nitin.dotledger.MainActivity
import com.nitin.dotledger.R
import com.nitin.dotledger.data.AppDatabase
import com.nitin.dotledger.data.entities.Transaction
import com.nitin.dotledger.data.repository.DotLedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "recurring_transactions_channel"
        const val CHANNEL_NAME = "Recurring Transactions"
        private const val NOTIFICATION_ID_BASE = 20000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val recurringDao = database.recurringTransactionDao()
            val transactionDao = database.transactionDao()
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()

            val repository = DotLedgerRepository(accountDao, categoryDao, transactionDao)

            // Get all due recurring transactions
            val currentTime = System.currentTimeMillis()
            val dueRecurring = recurringDao.getDueRecurring(currentTime)

            var successCount = 0
            var failCount = 0

            for (recurring in dueRecurring) {
                try {
                    // Check if end date has passed
                    if (recurring.endDate != null && currentTime > recurring.endDate) {
                        // Deactivate this recurring transaction
                        recurringDao.updateActiveStatus(recurring.id, false)
                        continue
                    }

                    // Create the transaction
                    val transaction = Transaction(
                        accountId = recurring.accountId,
                        categoryId = recurring.categoryId,
                        amount = recurring.amount,
                        type = recurring.type,
                        date = currentTime,
                        note = "${recurring.note} (Recurring: ${recurring.name})".trim(),
                        toAccountId = recurring.toAccountId
                    )

                    repository.insertTransaction(transaction)

                    // Update next occurrence
                    val nextOccurrence = recurring.frequency.getNextOccurrence(recurring.nextOccurrence)
                    recurringDao.updateNextOccurrence(recurring.id, nextOccurrence, currentTime)

                    successCount++

                    // Send notification for this transaction
                    showTransactionNotification(recurring, transaction)

                } catch (e: Exception) {
                    e.printStackTrace()
                    failCount++
                }
            }

            // Show summary notification if any transactions were created
            if (successCount > 0) {
                showSummaryNotification(successCount, failCount)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showTransactionNotification(
        recurring: com.nitin.dotledger.data.entities.RecurringTransaction,
        transaction: Transaction
    ) {
        createNotificationChannel()

        val notificationId = NOTIFICATION_ID_BASE + recurring.id.toInt()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val typeStr = when (transaction.type) {
            com.nitin.dotledger.data.entities.TransactionType.INCOME -> "Income"
            com.nitin.dotledger.data.entities.TransactionType.EXPENSE -> "Expense"
            com.nitin.dotledger.data.entities.TransactionType.TRANSFER -> "Transfer"
        }

        val amountStr = "₹${String.format("%.2f", transaction.amount)}"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle("Recurring $typeStr: $amountStr")
            .setContentText(recurring.name)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun showSummaryNotification(successCount: Int, failCount: Int) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Recurring Transactions Processed"
        val text = buildString {
            append("$successCount transaction(s) created")
            if (failCount > 0) {
                append(", $failCount failed")
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE - 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for recurring transactions"
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}