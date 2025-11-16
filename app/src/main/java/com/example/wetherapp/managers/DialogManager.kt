package com.example.wetherapp.managers

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import com.example.wetherapp.R

object DialogManager {
    fun locatiionSettingsDialog(context: Context, listener: Listener) {
        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_title)
            .setMessage(R.string.dialog_message)
            .setPositiveButton(R.string.dialog_butt1) { _, _ ->
                listener.onClick(null)
            }
            .setNegativeButton(R.string.dialog_butt2) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun confirmDialog(context: Context, message: String, onConfirm: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Подтверждение")
        builder.setMessage(message)
        builder.setPositiveButton("Да") { _, _ -> onConfirm(true) }
        builder.setNegativeButton("Нет") { _, _ -> onConfirm(false) }
        builder.show()
    }
    interface Listener{
        fun onClick(name: String?)
    }
}