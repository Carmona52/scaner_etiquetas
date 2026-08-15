package com.example.etiquetas.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class DateBuilders {
    fun makeDate(etiqueta: Etiqueta): String {
        val anio = LocalDate.now().year
        val firstDigitsofYear = anio.toString().substring(0, 3)
        return "${etiqueta.primDigDia}${etiqueta.segDigDia}/${etiqueta.primDigMes}${etiqueta.segDigMes}/${firstDigitsofYear}${etiqueta.ultDigAnio}"
    }

    fun makeHour(etiqueta: Etiqueta): String {
        return "${etiqueta.primDigHora}${etiqueta.segDigHora}:${etiqueta.primDigMin}${etiqueta.segDigMin}:${etiqueta.primDigSeg}${etiqueta.segDigSeg}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val parsedDate = java.time.LocalDateTime.parse(dateString)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            parsedDate.format(formatter)
        } catch (e: Exception) {
            dateString
        }
    }
}