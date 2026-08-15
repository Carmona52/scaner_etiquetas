package com.example.etiquetas.database.methods

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.example.etiquetas.utils.Etiqueta
import java.time.LocalDateTime

data class EtiquetaGuardada(
    val id: Long,
    val etiquetaEscaneada: String,
    val claveProducto: String,
    val descripcionArticulo: String?,
    val piezas: String?,
    val kilos: String,
    val lote: String?,
    val fecha: String,
    val hora: String,
    val fechaEscaneo: String,
    val zona: String?,
    val camara: String?,
    val turno: String,
    val tipoMovimiento: String,
    val escaneadoPor: String,
    val notas: String,
    val numEmpaque: String? = null
)

data class updateEtiqueta(
    val etiquetaEscaneada: String,
    val claveProducto: String,
    val piezas: String,
    val kilos: String,
    val lote: String,
    val tipoMovimiento: String
)

class Etiqueta(private val connection: SQLiteConnection) {

    fun getOneEtiqueta(id: String): updateEtiqueta? {
        var etiqueta: updateEtiqueta? = null

        connection.prepare("SELECT etiquetaEscaneada, claveProducto, piezas, kilos, lote, tipoMovimiento FROM Etiquetas WHERE id = ?")
            .use { stmt ->
                stmt.bindText(1, id)

                if (stmt.step()) {
                    etiqueta = updateEtiqueta(
                        etiquetaEscaneada = stmt.getText(0),
                        claveProducto = stmt.getText(1),
                        piezas = stmt.getText(2),
                        kilos = stmt.getText(3),
                        lote = stmt.getText(4),
                        tipoMovimiento = stmt.getText(5)
                    )
                }
            }

        return etiqueta
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertarEtiqueta(
        e: Etiqueta,
        fecha: String,
        hora: String,
        idZona: Int,
        idCamara: Int,
        turno: String,
        escaneadoPor: String,
        etiquetaEscaneada: String,
        tipoMovimiento: String,
        notas: String? = null
    ): Boolean {
        val notasActual = notas ?: e.notas
        return try {
            connection.prepare(
                """
                INSERT INTO Etiquetas
                (etiquetaEscaneada, claveProducto, piezas, kilos, lote, numEmpaque, fecha, hora, fechaEscaneo, idZona, idCamara, turno, tipoMovimiento, escaneadoPor, notas)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.bindText(1, etiquetaEscaneada)
                stmt.bindText(2, e.claveProducto)
                stmt.bindText(3, e.piezas ?: "")
                stmt.bindText(4, e.kilos)
                stmt.bindText(5, e.lote ?: "")
                stmt.bindText(6, e.numEmpaque)
                stmt.bindText(7, fecha)
                stmt.bindText(8, hora)
                stmt.bindText(9, (e.fechaEscaneo ?: LocalDateTime.now()).toString())
                stmt.bindInt(10, idZona)
                stmt.bindInt(11, idCamara)
                stmt.bindText(12, turno)
                stmt.bindText(13, tipoMovimiento)
                stmt.bindText(14, escaneadoPor)
                stmt.bindText(15, notasActual)
                stmt.step()
            }
            Log.i("Success", "Se insertó correctamente")
            true
        } catch (ex: Exception) {
            Log.e("DataBase", "Error al insertar etiqueta [$etiquetaEscaneada]", ex)
            false
        }
    }

    fun upsertEtiqueta(id: String, etiqueta: updateEtiqueta) {
        connection.prepare(
            "UPDATE Etiquetas SET etiquetaEscaneada = ?, claveProducto = ?, piezas = ?, kilos = ?, hora = ?, tipoMovimiento = ? WHERE id = $id"
        ).use { stmt ->
            stmt.bindText(1, etiqueta.etiquetaEscaneada)
            stmt.bindText(2, etiqueta.claveProducto)
            stmt.bindText(3, etiqueta.piezas)
            stmt.bindText(4, etiqueta.kilos)
            stmt.bindText(5, etiqueta.lote)
            stmt.bindText(6, etiqueta.tipoMovimiento)
            stmt.step()
        }
    }

    fun eliminarUltimaEtiqueta() {
        connection.execSQL("DELETE FROM Etiquetas WHERE id = (SELECT MAX(id) FROM Etiquetas)")
    }

    fun eliminarEtiqueta(id: Int) {
        connection.prepare("DELETE FROM Etiquetas WHERE id = ?").use { stmt ->
            stmt.bindInt(1, id)
            stmt.step()
        }
    }

    private fun mapearFilas(stmt: SQLiteStatement): EtiquetaGuardada {
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

    private fun mapFilasUpdate(stmt: SQLiteStatement): updateEtiqueta {
        return updateEtiqueta(
            etiquetaEscaneada = stmt.getText(0),
            claveProducto = stmt.getText(1),
            piezas = stmt.getText(2),
            kilos = stmt.getText(3),
            lote = stmt.getText(4),
            tipoMovimiento = stmt.getText(5)
        )
    }

}