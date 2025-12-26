package com.nitin.dotledger.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType, // EXPENSE or INCOME
    val isDefault: Boolean = false, // Default categories can't be deleted
    val colorCode: String = "#000000", // For pie chart colors
    val createdAt: Long = System.currentTimeMillis()
)

enum class CategoryType {
    EXPENSE,
    INCOME
}