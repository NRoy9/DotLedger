package com.nitin.dotledger.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.nitin.dotledger.MainActivity
import com.nitin.dotledger.R
import com.nitin.dotledger.utils.SmsParser

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "transaction_sms_channel"
        private const val CHANNEL_NAME = "Transaction SMS"
        private const val NOTIFICATION_ID_BASE = 10000

        const val ACTION_APPROVE = "com.nitin.dotledger.SMS_APPROVE"
        const val ACTION_DISMISS = "com.nitin.dotledger.SMS_DISMISS"

        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_ACCOUNT_INFO = "extra_account_info"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SMS_BODY = "extra_sms_body"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Check if SMS auto-parse is enabled in preferences
        val prefs = context.getSharedPreferences("dotledger_prefs", Context.MODE_PRIVATE)
        val autoParseEnabled = prefs.getBoolean("auto_parse_sms", false)

        if (!autoParseEnabled) {
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (message in messages) {
                val sender = message.displayOriginatingAddress
                val body = message.messageBody

                // Parse SMS
                val parsed = SmsParser.parseSms(body, sender)

                // Only show notification if confidence is above threshold
                if (parsed != null && parsed.confidence >= 0.5f) {
                    showTransactionNotification(context, parsed, body)
                }
            }
        }
    }

    private fun showTransactionNotification(
        context: Context,
        parsed: com.nitin.dotledger.utils.ParsedTransaction,
        smsBody: String
    ) {
        createNotificationChannel(context)

        val notificationId = NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt()

        // Intent to open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent to approve transaction
        val approveIntent = Intent(context, SmsActionReceiver::class.java).apply {
            action = ACTION_APPROVE
            putExtra(EXTRA_AMOUNT, parsed.amount)
            putExtra(EXTRA_TYPE, parsed.type.name)
            putExtra(EXTRA_MERCHANT, parsed.merchantName)
            putExtra(EXTRA_ACCOUNT_INFO, parsed.accountInfo)
            putExtra(EXTRA_DATE, parsed.date)
            putExtra(EXTRA_SMS_BODY, smsBody)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            approveIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent to dismiss
        val dismissIntent = Intent(context, SmsActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val typeStr = if (parsed.type == com.nitin.dotledger.data.entities.TransactionType.EXPENSE)
            "Expense" else "Income"
        val amountStr = "₹${String.format("%.2f", parsed.amount)}"

        val title = "New $typeStr Detected: $amountStr"
        val text = if (!parsed.merchantName.isNullOrBlank()) {
            "at ${parsed.merchantName}"
        } else {
            "Tap to add to DotLedger"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(SmsParser.formatTransactionSummary(parsed)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_check, "Add", approvePendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for detected transaction SMS"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}