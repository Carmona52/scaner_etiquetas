package com.example.etiquetas

import android.util.Log
import android.widget.Toast
import java.time.LocalDateTime

data class Etiqueta(
    val claveProducto: String,
    val etiquetaEscaneada: String? = "0",
    val piezas: String? = "0",
    val kilos: String,
    val lote: String? = "0",

    //Valores para Fecha
    val ultDigAnio: String? = "0",
    val primDigMes: String? = "0",
    val segDigMes: String? = "0",
    val primDigDia: String? = "0",
    val segDigDia: String? = "0",

    //valores para Hora
    val primDigHora: String? = "0",
    val segDigHora: String? = "0",
    val primDigMin: String? = "0",
    val segDigMin: String? = "0",
    val primDigSeg: String? = "0",
    val segDigSeg: String? = "0",

    val identificador: String,
    val fechaEscaneo: LocalDateTime? = null,
    val notas: String,

    val zona: String? = "0",
    val camara: String? = "0",
    val tipoMovimiento: String? = "0"
)

class Separador {

    fun etiquetaseparation(text: String): Etiqueta? {
        return when (text.length) {
            27 -> etiquetaslargas(text)
            26 -> etiquetasmedianas(text)
            25 -> etiquetasVeintiyCinco(text)
            24 -> longitudVeintiCuatro(text)
            23 -> etiquetascorta(text)
            22 -> longitudVeintidos(text)
            21 -> longitudVeintiuno(text)
            20 -> longitudVeinte(text)
            19 -> longitudDiecinueve(text)

            else -> {
                Log.d("separador", "Longitud no reconocida: ${text.length}")
                null
            }
        }
    }


    private fun etiquetaslargas(text: String): Etiqueta? {

        val claveProducto = text.substring(0, 4)
        Log.d("separador", "Clave Producto: $claveProducto")

        if (claveProducto == "0033") {
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

        if (claveProducto == "1100") {
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

    private fun longitudVeintiCuatro(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            piezas = text.substring(4, 7),
            kilos = text.substring(7, 12),

            identificador = text[23].toString(),

            lote = text.substring(20, 23),

            //obtener valores de la hora
            segDigHora = text[10].toString(),
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


            notas = "Esta Etiqueta no cuenta con: Fecha y lote completo"

        )
    }

    private fun etiquetascorta(text: String): Etiqueta? {
        val claveProducto = text.substring(0, 3)

        if(claveProducto == "624"){
            return Etiqueta(
                claveProducto = text.substring(0, 3),
                piezas = text.substring(4,7),

                kilos = text.substring(7, 13),

                identificador = text[22].toString(),

                lote = text.substring(18, 22),

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

        return Etiqueta(
            claveProducto = text.substring(0, 3),

            kilos = text.substring(5, 10),

            identificador = text[22].toString(),

            lote = text.substring(18, 22),

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

    private fun longitudVeintidos(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),
            piezas = text.substring(4, 7),
            kilos = text.substring(8, 13),
            identificador = text[21].toString(),

            lote = text.substring(17, 20),

            //obtener valores de la hora

            primDigMin = text[3].toString(),

            //valores para fecha
            ultDigAnio = text[15].toString(),
            segDigMes = text[14].toString(),
            segDigDia = text[16].toString(),


            //Esta etiqueta no cuenta con fecha y hora completas, hay etiquetas con impresión 2020, se toman como descarte, y no se toma el digito del año


            notas = "Esta Etiqueta no cuenta con: Fecha ni Hora completas"

        )
        Log.i("Long", "Longitud22")

    }

    private fun longitudVeintiuno(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            piezas = text.substring(4, 7),

            kilos = text.substring(8, 13),

            identificador = text[20].toString(),

            lote = text.substring(16, 20),

            //obtener valores de la hora
            segDigHora = text[13].toString(),
            primDigMin = text[3].toString(),

            //Esta etiqueta no cuenta con las horas completas, mientras que la fecha no viene impresa
            //

            notas = "Esta Etiqueta no cuenta con: Fecha y Hora completas"

        )
    }

    private fun longitudVeinte(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            kilos = text.substring(8, 13),

            piezas = text.substring(4, 7),

            identificador = text[19].toString(),

            lote = text.substring(14, 18),

            primDigMin = text[3].toString(),

            //Esta etiqueta solo cuenta con el primer digito del minuto, la demás está vacía


            notas = "Esta Etiqueta no cuenta con: Hora ni Fecha Completas"

        )
    }

    private fun longitudDiecinueve(text: String): Etiqueta? {
        return Etiqueta(
            claveProducto = text.substring(0, 3),

            kilos = text.substring(8, 13),

            piezas = text.substring(4, 7),

            identificador = text[18].toString(),

            lote = text.substring(14, 18),

            primDigMin = text[3].toString(),

            //Esta etiqueta no cuenta con datos como la fecha y hora de su impresión,
            //A mi entendimiento ninguna cuenta con numero de empaque


            notas = "Esta Etiqueta no cuenta con: Fecha ni hora de impresion"

        )
    }
}

