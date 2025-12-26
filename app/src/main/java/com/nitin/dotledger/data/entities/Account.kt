package com.nitin.dotledger.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType, // BANK, CREDIT_CARD, WALLET, etc.
    val balance: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    BANK,
    CREDIT_CARD,
    WALLET,
    CASH,
    OTHER
}