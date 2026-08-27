package com.example.etiquetas.factory.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView


object dialoFactory {
    fun createContenedor(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
    }

    fun addTitle(container: LinearLayout, title: String) {
        val titleView = TextView(container.context).apply {
            text = title
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 24, 0, 4)
        }
        container.addView(titleView)
    }

    fun addInputField(
        container: LinearLayout,
        titulo: String,
        hint: String? = null,
        valorInicial: String? = null,
        habilitado: Boolean = true,
        inputType: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        if (titulo.isNotEmpty()) {
            addTitle(container, titulo)
        }

        val campo = EditText(container.context).apply {
            setText(valorInicial)
            this.hint = hint
            this.inputType = inputType
            isEnabled = habilitado
        }
        container.addView(campo)
        return campo
    }

    fun createDialog(
        context: Context,
        title: String,
        contentView: LinearLayout,
        positiveText: String? = "Guardar",
        negativeText: String? = "Cancelar"
    ): AlertDialog {
        return AlertDialog.Builder(context).setTitle(title).setView(contentView)
            .setPositiveButton(positiveText, null)
            .setNegativeButton(negativeText, null)
            .setNeutralButton("Neutro", null)
            .setCancelable(true).create()
    }

}