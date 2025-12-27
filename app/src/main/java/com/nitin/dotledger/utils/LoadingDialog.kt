package com.nitin.dotledger.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.nitin.dotledger.databinding.DialogLoadingBinding

class LoadingDialog(context: Context) {
    private val dialog: Dialog = Dialog(context)
    private val binding: DialogLoadingBinding

    init {
        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
    }

    fun show(message: String = "Loading...") {
        binding.tvLoadingMessage.text = message
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun updateMessage(message: String) {
        binding.tvLoadingMessage.text = message
    }
}