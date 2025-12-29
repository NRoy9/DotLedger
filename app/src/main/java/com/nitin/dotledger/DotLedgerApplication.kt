package com.nitin.dotledger

import android.app.Application
import com.nitin.dotledger.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.*
import com.nitin.dotledger.workers.RecurringTransactionWorker
import java.util.concurrent.TimeUnit

class DotLedgerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Ensure settings are initialized
        initializeSettings()
        scheduleRecurringTransactionWorker()
    }

    private fun initializeSettings() {
        applicationScope.launch {
            val settings = database.settingsDao().getSettingsSync()
            if (settings == null) {
                // Initialize with default settings
                database.settingsDao().saveSettings(
                    com.nitin.dotledger.data.entities.AppSettings()
                )
            }
        }
    }

    private fun scheduleRecurringTransactionWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val recurringWork = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            1, TimeUnit.HOURS // Check every hour
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_transactions_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWork
        )
    }
}