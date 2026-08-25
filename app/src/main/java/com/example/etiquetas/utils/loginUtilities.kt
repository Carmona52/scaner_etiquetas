package com.example.etiquetas.utils

import android.content.Context
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.etiquetas.factory.dialog.dialoFactory

class LoginUtilities(private val Context: Context) {
    fun verifyIdentity(id: String) {
        val context = Context
        val layout = dialoFactory.createContenedor(context)

        val passwordInput = dialoFactory.addInputField(
            container = layout,
            titulo = "",
            hint = "Ingrese la contraseña",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val dialog = dialoFactory.createDialog(
            context = context,
            title = "Para poder editar la etiqueta, ingrese la contraseña",
            contentView = layout
        )

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = passwordInput.text.toString().trim()
                if (password == "securePass") {
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}