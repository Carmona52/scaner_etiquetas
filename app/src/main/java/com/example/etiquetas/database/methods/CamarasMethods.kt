package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

data class CamaraGuardada(
    val id: Int,
    val idZona: Int,
    val numCamara: Int,
    val nombreCamara: String,
    val descripcion: String?
)

data class ActualizarCamara(
    val idZona: Int, val numCamara: Int, val nombreCamara: String, val descripcion: String?
)

class Camaras(private val connection: SQLiteConnection) {
    fun getAllCamaras(): List<CamaraGuardada> {
        val resultado = mutableListOf<CamaraGuardada>()
        connection.prepare("SELECT id, idZona, numCamara, nombreCamara, descripcion FROM Camaras ORDER BY numCamara ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun getCamarasPorZona(idZona: Int): List<CamaraGuardada> {
        val resultado = mutableListOf<CamaraGuardada>()
        connection.prepare("SELECT id, idZona, numCamara, nombreCamara, descripcion FROM Camaras WHERE idZona = ? ORDER BY numCamara ASC")
            .use { stmt ->
                stmt.bindInt(1, idZona)
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun getCamaraName(id: Int): String {
        var nombre: String? = null
        connection.prepare("SELECT nombreCamara FROM Camaras WHERE id = ?").use { stmt ->
            stmt.bindInt(1, id)
            if (stmt.step()) nombre = stmt.getText(0)
        }
        return nombre ?: ""
    }

    fun obtenerCamaraActualId(etiquetaEscaneada: String): Int? {
        var idCamaraActual: Int? = null
        connection.prepare(
            """
            SELECT e.idCamara
            FROM Etiquetas e
            INNER JOIN (
                SELECT idCamara, MAX(id) AS maxId
                FROM Etiquetas
                WHERE etiquetaEscaneada = ?
                GROUP BY idCamara
            ) ultimo ON e.id = ultimo.maxId
            WHERE e.tipoMovimiento IN ('Entrada', 'Inventario')
        LIMIT 1
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, etiquetaEscaneada)
            if (stmt.step()) idCamaraActual = stmt.getInt(0)
        }
        return idCamaraActual
    }

    fun upsertCamara(camara: ActualizarCamara) {
        connection.prepare(
            "INSERT INTO Camaras (idZona, numCamara, nombreCamara, descripcion) VALUES (?, ?, ?, ?) " + "ON CONFLICT(numCamara) DO UPDATE SET idZona = excluded.idZona, nombreCamara = excluded.nombreCamara, descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindInt(1, camara.idZona)
            stmt.bindInt(2, camara.numCamara)
            stmt.bindText(3, camara.nombreCamara)
            stmt.bindText(4, camara.descripcion ?: "")
            stmt.step()
        }
    }


    private fun mapearFila(stmt: SQLiteStatement): CamaraGuardada {
        return CamaraGuardada(
            id = stmt.getInt(0),
            idZona = stmt.getInt(1),
            numCamara = stmt.getInt(2),
            nombreCamara = stmt.getText(3),
            descripcion = stmt.getText(4)
        )
    }
}