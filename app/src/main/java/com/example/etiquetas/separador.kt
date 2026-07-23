package com.example.etiquetas

import android.util.Log
import java.sql.Date
import java.time.LocalDateTime

data class Etiqueta(
    val claveProducto: String,
    val piezas: String,
    val kilos: String,
    val lote: String,

    //Valores para Fecha
    val ultDigAnio: String,
    val primDigMes: String,
    val segDigMes: String,
    val primDigDia: String,
    val segDigDia: String,

    //valores para Hora
    val primDigHora: String,
    val segDigHora: String,
    val primDigMin: String,
    val segDigMin: String,
    val primDigSeg: String,
    val segDigSeg: String,


    val identificador: String,
    val fechaEscaneo: LocalDateTime? = null
)

class separador {

    public fun etiquetaseparation(text: String): Etiqueta? {
        return when (text.length) {
            27 -> etiquetaslargas(text)
            25 -> etiquetasmedianas(text)
            23 -> etiquetascorta(text)
            else -> {
                Log.d("separador", "Longitud no reconocida: ${text.length}")
                null
            }
        }
    }

    private fun etiquetaslargas(text: String): Etiqueta? {

        return Etiqueta(
            claveProducto = text.substring(0, 3),

            //obtener valores de la hora
            primDigHora = text[17].toString(),
            segDigHora = text[13].toString(),
            primDigMin = text[3].toString(),
            segDigMin = text[20].toString(),
            primDigSeg = text[21].toString(),
            segDigSeg = text[12].toString(),

            piezas = text.substring(5, 7),
            kilos = text.substring(7, 12),

            //valores para fecha
            ultDigAnio = text[15].toString(),
            primDigMes = text[19].toString(),
            segDigMes = text[14].toString(),
            primDigDia = text[18].toString(),
            segDigDia = text[16].toString(),

            lote = text.substring(22, 26),
            identificador = text[26].toString(),

        )

    }

    private fun etiquetasmedianas(text: String): Etiqueta? {
        return null
    }

    private fun etiquetascorta(text: String): Etiqueta? {

        return null
    }
}

