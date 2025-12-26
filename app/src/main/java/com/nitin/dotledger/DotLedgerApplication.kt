package com.nitin.dotledger

import android.app.Application
import com.nitin.dotledger.data.AppDatabase

class DotLedgerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}