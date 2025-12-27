package com.nitin.dotledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nitin.dotledger.data.dao.AccountDao
import com.nitin.dotledger.data.dao.CategoryDao
import com.nitin.dotledger.data.dao.SettingsDao
import com.nitin.dotledger.data.dao.TransactionDao
import com.nitin.dotledger.data.entities.Account
import com.nitin.dotledger.data.entities.AppSettings
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.CategoryType
import com.nitin.dotledger.data.entities.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Category::class, Transaction::class, AppSettings::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dotledger_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.categoryDao())
                        initializeSettings(database.settingsDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Check if categories exist, if not, populate them
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val categoryCount = database.categoryDao().getCategoryCount(CategoryType.EXPENSE)
                        if (categoryCount == 0) {
                            populateDatabase(database.categoryDao())
                        }

                        // Ensure settings exist
                        val settings = database.settingsDao().getSettingsSync()
                        if (settings == null) {
                            initializeSettings(database.settingsDao())
                        }
                    }
                }
            }
        }

        suspend fun populateDatabase(categoryDao: CategoryDao) {
            // Default Expense Categories
            val expenseCategories = listOf(
                Category(name = "Food & Dining", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#FF6384"),
                Category(name = "Shopping", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#36A2EB"),
                Category(name = "Transport", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#FFCE56"),
                Category(name = "Entertainment", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#4BC0C0"),
                Category(name = "Bills & Utilities", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#9966FF"),
                Category(name = "Healthcare", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#FF9F40"),
                Category(name = "Investment", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#FF6384"),
                Category(name = "Others", type = CategoryType.EXPENSE, isDefault = true, colorCode = "#C9CBCF")
            )

            // Default Income Categories
            val incomeCategories = listOf(
                Category(name = "Salary", type = CategoryType.INCOME, isDefault = true, colorCode = "#4CAF50"),
                Category(name = "Reimbursement", type = CategoryType.INCOME, isDefault = true, colorCode = "#8BC34A"),
                Category(name = "Refund", type = CategoryType.INCOME, isDefault = true, colorCode = "#CDDC39"),
                Category(name = "Interest", type = CategoryType.INCOME, isDefault = true, colorCode = "#00BCD4")
            )

            categoryDao.insertAll(expenseCategories + incomeCategories)
        }

        suspend fun initializeSettings(settingsDao: SettingsDao) {
            // Initialize default settings
            settingsDao.saveSettings(AppSettings())
        }
    }
}