package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL

class Reportes(private val connection: SQLiteConnection) {

    companion object {
        private val BASE_SELECT_QUERY = """
        SELECT
            e.id,
            e.etiquetaEscaneada,
            e.claveProducto,
            a.descripcion,
            e.piezas,
            e.kilos,
            e.lote,
            e.fecha,
            e.hora,
            e.fechaEscaneo,
            z.nombreZona,
            c.nombreCamara,
            e.turno,
            m.tipoMovimiento,
            e.idMovimiento,
            e.escaneadoPor,
            e.notas,
            e.numEmpaque,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.idEtiqueta = e.id
                      AND aud.tipoEvento = 'EDICION'
                )
                THEN 1
                ELSE 0
            END AS editada,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.etiquetaEscaneada = e.etiquetaEscaneada
                      AND aud.tipoEvento = 'EDICION'
                      AND aud.campoModificado = 'kilos'
                )
                THEN 1
                ELSE 0
            END AS kilosEditados,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.etiquetaEscaneada = e.etiquetaEscaneada
                      AND aud.tipoEvento = 'EDICION'
                      AND aud.campoModificado = 'claveProducto'
                )
                THEN 1
                ELSE 0
            END AS claveEditada,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.etiquetaEscaneada = e.etiquetaEscaneada
                      AND aud.tipoEvento = 'EDICION'
                      AND aud.campoModificado = 'lote'
                )
                THEN 1
                ELSE 0
            END AS loteEditado,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.etiquetaEscaneada = e.etiquetaEscaneada
                      AND aud.tipoEvento = 'EDICION'
                      AND aud.campoModificado = 'piezas'
                )
                THEN 1
                ELSE 0
            END AS piezasEditadas,

            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM AuditoriaEtiquetas aud
                    WHERE aud.etiquetaEscaneada = e.etiquetaEscaneada
                      AND aud.tipoEvento = 'EDICION'
                      AND aud.campoModificado = 'tipoMovimiento'
                )
                THEN 1
                ELSE 0
            END AS movimientoEditado

        FROM Etiquetas e
        LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
        LEFT JOIN Zonas z ON e.idZona = z.id
        LEFT JOIN Camaras c ON e.idCamara = c.id
        LEFT JOIN Movimiento m ON e.idMovimiento = m.id
    """.trimIndent()
    }
    fun obtenerReporte(): List<EtiquetaGuardada> {
        val query = "$BASE_SELECT_QUERY ORDER BY e.id ASC"
        val resultado = mutableListOf<EtiquetaGuardada>()

        connection.prepare(query).use { stmt ->
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }

        return resultado
    }

    fun obtenerReporteFiltrado(
        idZona: Int? = null,
        idCamara: Int? = null,
        turno: String? = null,
        movimiento: String? = null,
        factor: Int? = null,
        fechaInicioISO: String? = null,
        fechaFinISO: String? = null
    ): List<EtiquetaGuardada> {
        val condiciones = mutableListOf<String>()
        val parametros = mutableListOf<Any>()

        idZona?.let {
            condiciones.add("e.idZona = ?")
            parametros.add(it)
        }
        idCamara?.let {
            condiciones.add("e.idCamara = ?")
            parametros.add(it)
        }
        turno?.let {
            condiciones.add("e.turno = ?")
            parametros.add(it)
        }
        movimiento?.let {
            condiciones.add("m.tipoMovimiento = ?")
            parametros.add(it)
        }
        factor?.let {
            condiciones.add("m.factor = ?")
            parametros.add(it)
        }
        when {
            fechaInicioISO != null && fechaFinISO != null -> {
                condiciones.add("e.fechaEscaneo >= ? AND e.fechaEscaneo <= ?")
                parametros.add("${fechaInicioISO}T00:00:00")
                parametros.add("${fechaFinISO}T23:59:59")
            }
            fechaInicioISO != null -> {
                condiciones.add("e.fechaEscaneo >= ?")
                parametros.add("${fechaInicioISO}T00:00:00")
            }
            fechaFinISO != null -> {
                condiciones.add("e.fechaEscaneo <= ?")
                parametros.add("${fechaFinISO}T23:59:59")
            }
        }

        val whereClause = if (condiciones.isNotEmpty()) {
            "WHERE ${condiciones.joinToString(" AND ")}"
        } else {
            ""
        }

        val query = "$BASE_SELECT_QUERY $whereClause ORDER BY e.id DESC"
        val resultado = mutableListOf<EtiquetaGuardada>()

        connection.prepare(query).use { stmt ->
            parametros.forEachIndexed { index, valor ->
                when (valor) {
                    is Int -> stmt.bindInt(index + 1, valor)
                    is String -> stmt.bindText(index + 1, valor)
                }
            }

            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }

        return resultado
    }
    fun hacerCorteDeInventario() {
        connection.execSQL("BEGIN TRANSACTION")
        try {
            connection.execSQL(
                """
                DELETE FROM Etiquetas
                WHERE id NOT IN (
                    SELECT e.id
                    FROM Etiquetas e
                    INNER JOIN (
                        SELECT e2.etiquetaEscaneada, MAX(e2.id) AS maxId
                        FROM Etiquetas e2
                        INNER JOIN Movimiento m2 ON e2.idMovimiento = m2.id
                        WHERE m2.factor != 0
                        GROUP BY e2.etiquetaEscaneada
                    ) ultimo ON e.id = ultimo.maxId
                    INNER JOIN Movimiento m ON e.idMovimiento = m.id
                    WHERE m.factor = 1
                )
                """.trimIndent()
            )

            reconstruirInventario()
            connection.execSQL("COMMIT")
        } catch (e: Exception) {
            connection.execSQL("ROLLBACK")
            throw e
        }
    }

    private fun reconstruirInventario() {
        connection.execSQL("DELETE FROM InventarioCamaras")
        connection.execSQL(
            """
            INSERT INTO InventarioCamaras (idCamara, totalKilos)
            SELECT
                e.idCamara,
                SUM(
                    CAST(ROUND(CAST(e.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE(m.factor, 0)
                ) AS totalKilos
            FROM Etiquetas e
            LEFT JOIN Movimiento m ON e.idMovimiento = m.id
            GROUP BY e.idCamara
            """.trimIndent()
        )
        connection.execSQL("DELETE FROM ConteoCestas")
        connection.execSQL(
            """
            INSERT INTO ConteoCestas (idProducto, idCamara, cantidadCestas, totalKilos)
            SELECT
                e.claveProducto,
                e.idCamara,
                SUM(COALESCE(m.factor, 0)) AS cantidadCestas,
                SUM(
                    CAST(ROUND(CAST(e.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE(m.factor, 0)
                ) AS totalKilos
            FROM Etiquetas e
            LEFT JOIN Movimiento m ON e.idMovimiento = m.id
            GROUP BY e.claveProducto, e.idCamara
            """.trimIndent()
        )
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
            idMovimiento = stmt.getInt(14),
            escaneadoPor = stmt.getText(15),
            notas = stmt.getText(16),
            numEmpaque = stmt.getText(17),
            editada = stmt.getInt(18) == 1,
            kilosEditados = stmt.getInt(19) == 1,
            claveEditada = stmt.getInt(20) == 1,
            loteEditado = stmt.getInt(21) == 1,
            piezasEditadas = stmt.getInt(22) == 1,
            movimientoEditado = stmt.getInt(23) == 1
        )
    }
}