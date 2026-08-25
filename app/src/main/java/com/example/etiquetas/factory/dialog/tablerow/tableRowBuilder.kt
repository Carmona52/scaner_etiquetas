package com.example.etiquetas.factory.dialog.tablerow

import android.content.Context
import android.graphics.Typeface
import android.widget.TableRow
import android.widget.TextView

object TableCellFactory {
    enum class TypeCelda { Normal, Header, Numerica, Tabla }

    fun createCelda(
        context: Context,
        texto: String,
        tipo: TypeCelda = TypeCelda.Normal,
        weight: Float = 0f,
        onClick: (() -> Unit)? = null
    ): TextView {
        return TextView(context).apply {

            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                weight
            )
            setPadding(20, 20, 20, 20)
            text = texto

            when (tipo) {
                TypeCelda.Header -> {
                    setTypeface(typeface, Typeface.BOLD)
                }

                TypeCelda.Numerica -> {
                    textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
                }

                TypeCelda.Tabla -> {
                    textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                }

                TypeCelda.Normal -> {

                }

            }

            onClick?.let { listener -> setOnClickListener { listener() } }
        }
    }
}