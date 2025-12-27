package com.nitin.dotledger.utils

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

object ValidationUtils {

    /**
     * Validate that a field is not empty
     */
    fun validateNotEmpty(
        editText: EditText,
        textInputLayout: TextInputLayout? = null,
        errorMessage: String = "This field is required"
    ): Boolean {
        val text = editText.text.toString().trim()

        return if (text.isEmpty()) {
            textInputLayout?.error = errorMessage
            editText.error = errorMessage
            false
        } else {
            textInputLayout?.error = null
            editText.error = null
            true
        }
    }

    /**
     * Validate amount is valid
     */
    fun validateAmount(amount: String): Boolean {
        val value = amount.toDoubleOrNull()
        return value != null && value > 0
    }

    /**
     * Validate account name
     */
    fun validateAccountName(name: String): ValidationResult {
        return when {
            name.trim().isEmpty() -> ValidationResult(false, "Account name cannot be empty")
            name.length < 2 -> ValidationResult(false, "Account name too short")
            name.length > 50 -> ValidationResult(false, "Account name too long")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Validate category name
     */
    fun validateCategoryName(name: String): ValidationResult {
        return when {
            name.trim().isEmpty() -> ValidationResult(false, "Category name cannot be empty")
            name.length < 2 -> ValidationResult(false, "Category name too short")
            name.length > 30 -> ValidationResult(false, "Category name too long")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Validate transaction note
     */
    fun validateNote(note: String): ValidationResult {
        return when {
            note.length > 200 -> ValidationResult(false, "Note too long (max 200 characters)")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Show error on TextInputLayout
     */
    fun showError(textInputLayout: TextInputLayout, message: String) {
        textInputLayout.error = message
        textInputLayout.isErrorEnabled = true
    }

    /**
     * Clear error from TextInputLayout
     */
    fun clearError(textInputLayout: TextInputLayout) {
        textInputLayout.error = null
        textInputLayout.isErrorEnabled = false
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)