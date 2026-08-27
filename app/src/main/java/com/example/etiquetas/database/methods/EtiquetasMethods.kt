package com.example.etiquetas.database.methods

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.etiquetas.utils.Etiqueta
import java.time.LocalDateTime

data class EtiquetaGuardada(
    val id: Long?,
    val etiquetaEscaneada: String?,
    val claveProducto: String?,
    val descripcionArticulo: String?,
    val piezas: String?,
    val kilos: String?,
    val lote: String?,
    val fecha: String?,
    val hora: String?,
    val fechaEscaneo: String?,
    val zona: String?,
    val camara: String?,
    val turno: String?,
    val tipoMovimiento: String?,
    val escaneadoPor: String?,
    val notas: String?,
    val numEmpaque: String? = null,
    val idMovimiento: Int? = null
)

data class updateEtiqueta(
    val etiquetaEscaneada: String,
    val claveProducto: String,
    val piezas: String,
    val kilos: String,
    val lote: String,
    val tipoMovimiento: String
)

data class lastEtiqueta(
    val id: Long?,
    val etiquetaEscaneada: String?,
    val claveProducto: String?,
    val descripcionArticulo: String?,
    val piezas: String?,
    val kilos: String?,
    val lote: String?,
    val fecha: String?,
    val hora: String?,
    val fechaEscaneo: String?,
    val zona: String?,
    val camara: String?,
    val turno: String?,
    val tipoMovimiento: String?,
)

data class StatusEtiqueta(val idCamara: Int?, val factor: Int)

class Etiqueta(private val connection: SQLiteConnection) {

    fun getOneEtiqueta(id: String): updateEtiqueta? {
        var etiqueta: updateEtiqueta? = null

        connection.prepare(
            """
            SELECT e.etiquetaEscaneada, e.claveProducto, e.piezas, e.kilos, e.lote, m.tipoMovimiento 
            FROM Etiquetas e
            LEFT JOIN Movimiento m ON e.idMovimiento = m.id
            WHERE e.id = ?
            """.trimIndent()
        )
            .use { stmt ->
                stmt.bindInt(1, id.toInt())

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

    fun getLastEtiqueta(): EtiquetaGuardada? {
        var etiqueta: EtiquetaGuardada? = null
        connection.prepare(
            """SELECT
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
                e.idMovimiento
            FROM Etiquetas e
            LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
            LEFT JOIN Zonas z ON e.idZona = z.id
            LEFT JOIN Camaras c ON e.idCamara = c.id
            LEFT JOIN Movimiento m ON e.idMovimiento = m.id
           ORDER BY e.id DESC LIMIT 1""".trimIndent()
        )
            .use { stmt ->
                if (stmt.step()) {
                    etiqueta = EtiquetaGuardada(
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
                        escaneadoPor = "",
                        notas = "",
                        numEmpaque = "",
                        idMovimiento = stmt.getInt(14),
                    )
                }
            }
        return etiqueta
    }

    fun getStatusActual(etiqueta: String): StatusEtiqueta {
        var status = StatusEtiqueta(null, 0)
        connection.prepare(
            """
            SELECT e.idCamara, m.factor
            FROM Etiquetas e
            JOIN Movimiento m ON e.idMovimiento = m.id
            WHERE e.etiquetaEscaneada = ? AND m.factor != 0
            ORDER BY e.id DESC
            LIMIT 1
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, etiqueta)
            if (stmt.step()) {
                status = StatusEtiqueta(stmt.getInt(0), stmt.getInt(1))
            }
        }
        return status
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
        notas: String? = null,
        idMovimiento: Int? = null
    ): Boolean {
        val notasActual = notas ?: e.notas
        return try {
            connection.prepare(
                """
                INSERT INTO Etiquetas
                (etiquetaEscaneada, claveProducto, piezas, kilos, lote, numEmpaque, fecha, hora, fechaEscaneo, idZona, idCamara, idMovimiento, turno, escaneadoPor, notas)
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
                stmt.bindInt(12, idMovimiento ?: 0)
                stmt.bindText(13, turno)
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

    fun upsertEtiqueta(id: String, etiqueta: updateEtiqueta): Boolean {
        var movimientoId: Int? = null
        var factorEncontrado: Int? = null

        connection.prepare("SELECT id, factor FROM Movimiento WHERE tipoMovimiento = ?")
            .use { stmt ->
                stmt.bindText(1, etiqueta.tipoMovimiento)
                if (stmt.step()) {
                    movimientoId = stmt.getInt(0)
                    factorEncontrado = stmt.getInt(1)
                }
            }

        if (movimientoId == null) {
            Log.e(
                "UPSERT_DEBUG",
                "NO HUBO MATCH para '${etiqueta.tipoMovimiento}' — idMovimiento quedará NULL"
            )
            return false
        }

        connection.prepare(
            "UPDATE Etiquetas SET etiquetaEscaneada = ?, claveProducto = ?, piezas = ?, kilos = ?, lote = ?, idMovimiento = ? WHERE id = ?"
        ).use { stmt ->
            stmt.bindText(1, etiqueta.etiquetaEscaneada)
            stmt.bindText(2, etiqueta.claveProducto)
            stmt.bindText(3, etiqueta.piezas)
            stmt.bindText(4, etiqueta.kilos)
            stmt.bindText(5, etiqueta.lote)
            stmt.bindInt(6, movimientoId!!)
            stmt.bindInt(7, id.toInt())
            stmt.step()
        }
        return true
    }

    fun eliminarUltimaEtiqueta() {
        connection.execSQL("DELETE FROM Etiquetas WHERE id = (SELECT MAX(id) FROM Etiquetas)")
    }

    fun eliminarEtiqueta(id: Long) {
        connection.prepare("DELETE FROM Etiquetas WHERE id = ?").use { stmt ->
            stmt.bindInt(1, id.toInt())
            stmt.step()
        }
    }

    fun elminarEtiquetaText(etiqueta: String) {
        connection.prepare("DELETE FROM Etiquetas WHERE etiquetaEscaneada = ?").use { stmt ->
            stmt.bindText(1, etiqueta)
            stmt.step()
        }
    }
}