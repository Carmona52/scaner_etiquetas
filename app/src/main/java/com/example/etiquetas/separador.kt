package com.example.etiquetas

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

    public fun etiquetaseparation(text: String) {
        if (text.length < 26) {
            return println("La cadena tiene menos de 26 caracteres")
        }

        when (text.length) {
            26 -> etiquetaslargas(text)
            24 -> etiquetasmedianas(text)
            22 -> etiquetascorta(text)
        }

    }

    private fun etiquetaslargas(text: String): Etiqueta? {

        return Etiqueta(
            claveProducto = text.substring(0, 2),

            //obtener valores de la hora
            primDigHora = text.substring(17),
            segDigHora = text.substring(13),
            primDigMin = text.substring(3),
            segDigMin = text.substring(20),
            primDigSeg = text.substring(21),
            segDigSeg = text.substring(12),

            piezas = text.substring(4, 6),
            kilos = text.substring(7, 11),

            //valores para fecha
            ultDigAnio = text.substring(15),
            primDigMes = text.substring(19),
            segDigMes = text.substring(14),
            primDigDia = text.substring(18),
            segDigDia = text.substring(16),

            lote = text.substring(22, 25),
            identificador = text.substring(26)
        )

    }

    private fun etiquetasmedianas(text: String) {

    }

    private fun etiquetascorta(text: String) {

    }
}

