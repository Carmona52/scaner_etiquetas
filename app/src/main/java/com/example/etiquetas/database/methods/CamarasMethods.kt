package com.example.etiquetas.database.methods

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
@Parcelize
data class CamaraGuardada(
    val id: Int,
    val idZona: Int,
    val numCamara: Int,
    val nombreCamara: String,
    val descripcion: String?
): Parcelable

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

    fun getCamaraName(id: Int): String {
        var nombre: String? = null
        connection.prepare("SELECT nombreCamara FROM Camaras WHERE id = ?").use { stmt ->
            stmt.bindInt(1, id)
            if (stmt.step()) nombre = stmt.getText(0)
        }
        return nombre ?: ""
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

    fun getCameraWeight(idCamara: Int?): Double {
        var peso = 0.0
        connection.prepare("SELECT totalKilos FROM InventarioCamaras WHERE idCamara = ?")
            .use { stmt ->
                stmt.bindInt(1, idCamara ?: 0)
                if (stmt.step()) peso = stmt.getDouble(0)
            }
        return String.format("%.2f", peso).toDouble()
    }

    fun getTotalCestas(idCamara: Int?): Int {
        var cestas = 0
        connection.prepare("SELECT SUM(cantidadCestas) FROM ConteoCestas WHERE idCamara = ?")
            .use { stmt ->
                stmt.bindInt(1, idCamara ?: 0)
                if (stmt.step()) cestas = stmt.getInt(0)
            }
        return cestas
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