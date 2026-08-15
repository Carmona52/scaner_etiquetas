package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL

class Reportes(private val connection: SQLiteConnection) {
    fun obtenerReporte(): List<EtiquetaGuardada> {
        val resultado = mutableListOf<EtiquetaGuardada>()
        connection.prepare(
            """
            SELECT e.id, e.etiquetaEscaneada, e.claveProducto, a.descripcion, e.piezas, e.kilos, e.lote,
                   e.fecha, e.hora, e.fechaEscaneo, z.nombreZona, c.nombreCamara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas, e.numEmpaque
            FROM Etiquetas e
            LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
            LEFT JOIN Zonas z ON e.idZona = z.id
            LEFT JOIN Camaras c ON e.idCamara = c.id
            ORDER BY e.id ASC
            """.trimIndent()
        ).use { stmt ->
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }
        return resultado
    }

    fun hacerCorteDeInventario() {
        connection.execSQL(
            """
            DELETE FROM Etiquetas
            WHERE id NOT IN (
                SELECT e.id
                FROM Etiquetas e
                INNER JOIN (
                    SELECT etiquetaEscaneada, idCamara, MAX(id) AS maxId
                    FROM Etiquetas
                    GROUP BY etiquetaEscaneada, idCamara
                ) ultimo ON e.id = ultimo.maxId
                WHERE e.tipoMovimiento = 'Entrada'
            )
            """.trimIndent()
        )
    }

    fun obtenerReporteFiltrado(
        idZona: Int? = null,
        idCamara: Int? = null,
        turno: String? = null,
        movimiento: String? = null,
        fechaInicioISO: String? = null,
        fechaFinISO: String? = null
    ): List<EtiquetaGuardada> {
        val condiciones = mutableListOf<String>()
        val valores = mutableListOf<String>()

        if (idZona != null) condiciones.add("e.idZona = $idZona")
        if (idCamara != null) condiciones.add("e.idCamara = $idCamara")
        if (turno != null) {
            condiciones.add("e.turno = ?"); valores.add(turno)
        }
        if (movimiento != null) {
            condiciones.add("e.tipoMovimiento = ?"); valores.add(movimiento)
        }

        if (fechaInicioISO != null && fechaFinISO != null) {
            condiciones.add("e.fechaEscaneo >= ? AND e.fechaEscaneo <= ?")
            valores.add("${fechaInicioISO}T00:00:00")
            valores.add("${fechaFinISO}T23:59:59")
        } else if (fechaInicioISO != null) {
            condiciones.add("e.fechaEscaneo >= ?")
            valores.add("${fechaInicioISO}T00:00:00")
        } else if (fechaFinISO != null) {
            condiciones.add("e.fechaEscaneo <= ?")
            valores.add("${fechaFinISO}T23:59:59")
        }

        val whereClause =
            if (condiciones.isNotEmpty()) "WHERE " + condiciones.joinToString(" AND ") else ""

        val resultado = mutableListOf<EtiquetaGuardada>()
        connection.prepare(
            """
            SELECT e.id, e.etiquetaEscaneada, e.claveProducto, a.descripcion, e.piezas, e.kilos, e.lote,
                   e.fecha, e.hora, e.fechaEscaneo, z.nombreZona, c.nombreCamara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas, e.numEmpaque
            FROM Etiquetas e
            LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
            LEFT JOIN Zonas z ON e.idZona = z.id
            LEFT JOIN Camaras c ON e.idCamara = c.id
            $whereClause
            ORDER BY e.id DESC
            """.trimIndent()
        ).use { stmt ->
            valores.forEachIndexed { index, valor -> stmt.bindText(index + 1, valor) }
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }
        return resultado
    }

    private fun mapearFila(stmt: SQLiteStatement): EtiquetaGuardada {
        return EtiquetaGuardada(
            id = stmt.getLong(0),
            etiquetaEscaneada = stmt.getText(1),
            claveProducto = stmt.getText(2),
            descripcionArticulo = stmt.getText(3),
            piezas = stmt.getText(4),
            kilos = stmt.getText(5),
            lote = stmt.getText(6),
            fecha = stmt.getText(7),
            hora = stmt.getText(8),
            fechaEscaneo = stmt.getText(9),
            zona = stmt.getText(10),
            camara = stmt.getText(11),
            turno = stmt.getText(12),
            tipoMovimiento = stmt.getText(13),
            escaneadoPor = stmt.getText(14),
            notas = stmt.getText(15),
            numEmpaque = stmt.getText(16)
        )
    }
}