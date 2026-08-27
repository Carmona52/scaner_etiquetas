package com.example.etiquetas.database.methods

import android.os.Parcelable
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.parcelize.Parcelize

@Parcelize
data class CamaraGuardada(
    val id: Int,
    val idZona: Int,
    val numCamara: Int,
    val nombreCamara: String,
    val descripcion: String?
) : Parcelable

data class getConteoCamara(
    val cantidadCestas: Int, val descripcion: String?, val kilosPorProducto: Double
)

data class ActualizarCamara(
    val idZona: Int, val numCamara: Int, val nombreCamara: String, val descripcion: String?
)

class Camaras(private val connection: SQLiteConnection) {

    fun getAllCamaras(): List<CamaraGuardada> {
        val resultado = mutableListOf<CamaraGuardada>()

        connection.prepare(
            """
            SELECT
                id,
                idZona,
                numCamara,
                nombreCamara,
                descripcion
            FROM Camaras
            ORDER BY numCamara ASC
            """.trimIndent()
        ).use { stmt ->
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }

        return resultado
    }

    fun getCamaraName(id: Int): String {
        var nombre: String? = null

        connection.prepare(
            "SELECT nombreCamara FROM Camaras WHERE id = ?"
        ).use { stmt ->
            stmt.bindInt(1, id)

            if (stmt.step()) {
                nombre = stmt.getText(0)
            }
        }

        return nombre ?: ""
    }

    fun upsertCamara(camara: ActualizarCamara) {
        connection.prepare(
            """
            INSERT INTO Camaras (
                idZona,
                numCamara,
                nombreCamara,
                descripcion
            )
            VALUES (?, ?, ?, ?)

            ON CONFLICT(numCamara) DO UPDATE SET
                idZona = excluded.idZona,
                nombreCamara = excluded.nombreCamara,
                descripcion = excluded.descripcion
            """.trimIndent()
        ).use { stmt ->
            stmt.bindInt(1, camara.idZona)
            stmt.bindInt(2, camara.numCamara)
            stmt.bindText(3, camara.nombreCamara)
            stmt.bindText(4, camara.descripcion ?: "")
            stmt.step()
        }
    }

    fun getCameraWeight(idCamara: Int?): Double {
        var pesoCent = 0

        connection.prepare(
            """
            SELECT totalKilos
            FROM InventarioCamaras
            WHERE idCamara = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.bindInt(1, idCamara ?: 0)

            if (stmt.step()) {
                pesoCent = stmt.getInt(0)
            }
        }

        return pesoCent / 100.0
    }

    fun getTotalCestas(idCamara: Int?): Int {
        var cestas = 0

        connection.prepare(
            """
            SELECT COALESCE(SUM(cantidadCestas), 0)
            FROM ConteoCestas
            WHERE idCamara = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.bindInt(1, idCamara ?: 0)

            if (stmt.step()) {
                cestas = stmt.getInt(0)
            }
        }

        return cestas
    }

    fun getTotalTransacciones(idCamara: Int?): Int {
        var transacciones = 0
        connection.prepare(
            """
            SELECT COUNT(*)
            FROM Etiquetas
            WHERE idCamara = ?
            """.trimIndent()
        ).use { stmt ->
                stmt.bindInt(1, idCamara ?: 0)
                if (stmt.step()) {
                    transacciones = stmt.getInt(0)
                }
            }
        return transacciones
    }

    fun getConteoCestas(idCamara: Int?): List<getConteoCamara> {
        val resultado = mutableListOf<getConteoCamara>()

        connection.prepare(
            """
            SELECT
                c.idProducto,
                c.cantidadCestas,
                a.descripcion,
                c.totalKilos / 100.00
            FROM ConteoCestas c
            LEFT JOIN Articulos a
                ON c.idProducto = a.claveProducto
            WHERE c.idCamara = ? 
            ORDER BY c.cantidadCestas DESC
            """.trimIndent()
        ).use { stmt ->
            stmt.bindInt(1, idCamara ?: 0)

            while (stmt.step()) {
                resultado.add(
                    getConteoCamara(
                        cantidadCestas = stmt.getInt(1),
                        descripcion = stmt.getText(2),
                        kilosPorProducto = stmt.getDouble(3)
                    )
                )
            }
        }

        return resultado
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