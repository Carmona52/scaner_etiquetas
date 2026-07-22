package com.example.etiquetas

import android.util.Log

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


    val identificador: String
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
            }as Etiqueta
        }

    private fun etiquetaslargas(text: String): Etiqueta? {

        return Etiqueta(
            claveProducto = text.substring(0, 3),

            //obtener valores de la hora
            primDigHora = text[18].toString(),
            segDigHora = text[14].toString(),
            primDigMin = text[4].toString(),
            segDigMin = text[21].toString(),
            primDigSeg = text[22].toString(),
            segDigSeg = text[13].toString(),

            piezas = text.substring(5, 7),
            kilos = text.substring(7, 11),

            //valores para fecha
            ultDigAnio = text[16].toString(),
            primDigMes = text[20].toString(),
            segDigMes = text[15].toString(),
            primDigDia = text[19].toString(),
            segDigDia = text[17].toString(),

            lote = text.substring(23, 26),
            identificador = text[27].toString()
        )

    }

    private fun etiquetasmedianas(text: String): Etiqueta? {
        return null
    }

    private fun etiquetascorta(text: String): Etiqueta? {

        return null
    }
}

