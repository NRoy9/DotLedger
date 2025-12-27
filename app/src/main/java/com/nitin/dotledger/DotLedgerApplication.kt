package com.nitin.dotledger

import android.app.Application
import com.nitin.dotledger.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DotLedgerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Ensure settings are initialized
        initializeSettings()
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
}