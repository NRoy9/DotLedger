package com.nitin.dotledger.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View

object ErrorHandler {

    private const val TAG = "DotLedger_Error"

    /**
     * Handle and log error with toast
     */
    fun handleError(context: Context, error: Throwable, userMessage: String? = null) {
        Log.e(TAG, "Error occurred: ${error.message}", error)

        val message = userMessage ?: getErrorMessage(error)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Handle error with Snackbar
     */
    fun handleErrorWithSnackbar(
        view: View,
        error: Throwable,
        userMessage: String? = null,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        Log.e(TAG, "Error occurred: ${error.message}", error)

        val message = userMessage ?: getErrorMessage(error)
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)

        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        snackbar.show()
    }

    /**
     * Get user-friendly error message
     */
    private fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is java.io.IOException -> "Network error. Please check your connection."
            is IllegalStateException -> "Invalid operation. Please try again."
            is IllegalArgumentException -> "Invalid input. Please check your data."
            is NullPointerException -> "Missing required data."
            else -> "An error occurred: ${error.message ?: "Unknown error"}"
        }
    }

    /**
     * Log info message
     */
    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Log warning
     */
    fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    /**
     * Log error
     */
    fun logError(message: String, error: Throwable? = null) {
        if (error != null) {
            Log.e(TAG, message, error)
        } else {
            Log.e(TAG, message)
        }
    }

    /**
     * Show success message
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, "✓ $message", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show info message
     */
    fun showInfo(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}