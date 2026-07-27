package com.example.etiquetas

import android.util.Log
import java.time.LocalDateTime

data class Etiqueta(
    val claveProducto: String,
    val piezas: String? = null,
    val kilos: String,
    val lote: String? = null,

    //Valores para Fecha
    val ultDigAnio: String,
    val primDigMes: String,
    val segDigMes: String,
    val primDigDia: String,
    val segDigDia: String,

    //valores para Hora
    val primDigHora: String? = "0",
    val segDigHora: String,
    val primDigMin: String,
    val segDigMin: String,
    val primDigSeg: String? = "0",
    val segDigSeg: String? = "0",

    val identificador: String,
    val fechaEscaneo: LocalDateTime? = null,
    val notas: String,

    val zona: String? = null,
    val camara: String? = null
)

class separador {

    fun etiquetaseparation(text: String): Etiqueta? {
        return when (text.length) {
            27 -> etiquetaslargas(text)
            26 -> etiquetasmedianas(text)
            25 -> etiquetasVeintiyCinco(text)
            23 -> etiquetascorta(text)
            else -> {
                Log.d("separador", "Longitud no reconocida: ${text.length}")
                null
            }
        }
    }

    private fun etiquetaslargas(text: String): Etiqueta? {

        val claveProducto = text.substring(0,4)

        if (claveProducto == "0033"){
            return Etiqueta(
                claveProducto = text.substring(0, 4),

                piezas = text.substring(5, 7),

                kilos = text.substring(8, 13),

                lote = text.substring(22, 26),

                identificador = text[26].toString(),

                //obtener valores de la hora
                primDigHora = text[17].toString(),
                segDigHora = text[13].toString(),
                primDigMin = text[3].toString(),
                segDigMin = text[20].toString(),
                primDigSeg = text[21].toString(),
                segDigSeg = text[12].toString(),

                //valores para fecha
                ultDigAnio = text[15].toString(),
                primDigMes = text[19].toString(),
                segDigMes = text[14].toString(),
                primDigDia = text[18].toString(),
                segDigDia = text[16].toString(),

                notas = "Escaneo Completo"
                )
        }

        if (claveProducto == "1100"){
            return Etiqueta(
                claveProducto = text.substring(0, 4),

                piezas = text.substring(5, 8),

                kilos = text.substring(9, 14),

                lote = text.substring(22, 26),

                identificador = text[26].toString(),

                //obtener valores de la hora
                primDigHora = text[17].toString(),
                segDigHora = text[13].toString(),
                primDigMin = text[3].toString(),
                segDigMin = text[20].toString(),
                primDigSeg = text[21].toString(),
                segDigSeg = text[12].toString(),

                //valores para fecha
                ultDigAnio = text[15].toString(),
                primDigMes = text[19].toString(),
                segDigMes = text[14].toString(),
                primDigDia = text[18].toString(),
                segDigDia = text[16].toString(),

                notas = "Escaneo Completo"
            )
        }

        return Etiqueta(
            claveProducto = text.substring(0, 3),

            piezas = text.substring(5, 7),

            kilos = text.substring(8, 13),

            lote = text.substring(22, 26),

            identificador = text[26].toString(),

            //obtener valores de la hora
            primDigHora = text[17].toString(),
            segDigHora = text[13].toString(),
            primDigMin = text[3].toString(),
            segDigMin = text[20].toString(),
            primDigSeg = text[21].toString(),
            segDigSeg = text[12].toString(),

            //valores para fecha
            ultDigAnio = text[15].toString(),
            primDigMes = text[19].toString(),
            segDigMes = text[14].toString(),
            primDigDia = text[18].toString(),
            segDigDia = text[16].toString(),

            notas = "Escaneo Completo"
        )

    }
    private fun etiquetasmedianas(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            piezas = text.substring(4, 7),

            kilos = text.substring(8, 13),

            lote = text.substring(21, 25),

            identificador = text[25].toString(),

            //obtener valores de la hora
            primDigHora = text[17].toString(),
            segDigHora = text[13].toString(),
            primDigMin = text[3].toString(),
            segDigMin = text[20].toString(),

            //valores para fecha
            ultDigAnio = text[15].toString(),
            primDigMes = text[19].toString(),
            segDigMes = text[14].toString(),
            primDigDia = text[18].toString(),
            segDigDia = text[16].toString(),

            notas = "Faltan Segundos"
        )
    }
    private fun etiquetasVeintiyCinco(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            piezas = text.substring(3, 6),

            kilos = text.substring(7, 12),

            lote = text.substring(20, 24),

            identificador = text[24].toString(),

            //obtener valores de la hora
            primDigHora = text[16].toString(),
            segDigHora = text[12].toString(),
            primDigMin = text[2].toString(),
            segDigMin = text[19].toString(),
            segDigSeg = text[11].toString(),

            //valores para fecha
            ultDigAnio = text[14].toString(),
            primDigMes = text[18].toString(),
            segDigMes = text[13].toString(),
            primDigDia = text[17].toString(),
            segDigDia = text[15].toString(),

            notas = "No cuenta el primer digito del segundo"
        )
    }
    private fun etiquetascorta(text: String): Etiqueta? {

        return Etiqueta(
            claveProducto = text.substring(0, 3),

            kilos = text.substring(6, 10),

            identificador = text[22].toString(),

            lote = text.substring(18,22),

            //obtener valores de la hora
            segDigHora = text[10].toString(),
            primDigMin = text[3].toString(),
            segDigMin = text[17].toString(),

            //valores para fecha
            ultDigAnio = text[12].toString(),
            primDigMes = text[16].toString(),
            segDigMes = text[11].toString(),
            primDigDia = text[15].toString(),
            segDigDia = text[13].toString(),


            notas = "Esta Etiqueta no cuenta con: Lote ni Piezas"

        )
    }
}

