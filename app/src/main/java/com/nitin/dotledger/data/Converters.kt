package com.nitin.dotledger.data

import androidx.room.TypeConverter
import com.nitin.dotledger.data.entities.AccountType
import com.nitin.dotledger.data.entities.CategoryType
import com.nitin.dotledger.data.entities.NumberFormat
import com.nitin.dotledger.data.entities.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromNumberFormat(value: NumberFormat): String = value.name

    @TypeConverter
    fun toNumberFormat(value: String): NumberFormat = NumberFormat.valueOf(value)
}