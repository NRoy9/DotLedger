package com.nitin.dotledger.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nitin.dotledger.data.entities.Category
import com.nitin.dotledger.data.entities.CategoryType

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>>

    @Query("SELECT * FROM categories ORDER BY type, name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    suspend fun getCategoryCount(type: CategoryType): Int
}