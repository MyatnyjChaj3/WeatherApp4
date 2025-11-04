package com.example.wetherapp.managers

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

object DialogManager {
    fun locatiionSettingsDialog(context: Context, listener: Listener){
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()

        dialog.setTitle("Enable location?")
        dialog.setMessage("Location disabled, do you wanna enable location?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK"){ _, _ ->
            listener.onClick(null)
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel"){ _, _ ->
            dialog.dismiss()
        }
        dialog.show()
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