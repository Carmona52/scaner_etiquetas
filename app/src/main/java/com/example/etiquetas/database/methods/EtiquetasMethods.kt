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
    val idMovimiento: Int? = null,
    val editada: Boolean = false,
    val kilosEditados: Boolean = false,
    val claveEditada: Boolean = false,
    val loteEditado: Boolean = false,
    val piezasEditadas: Boolean = false,
    val movimientoEditado: Boolean = false
)

data class UpdateEtiqueta(
    val etiquetaEscaneada: String,
    val claveProducto: String,
    val piezas: String,
    val kilos: String,
    val lote: String,
    val tipoMovimiento: String
)

private data class EtiquetaAnterior(
    val etiquetaEscaneada: String,
    val claveProducto: String,
    val piezas: String,
    val kilos: String,
    val lote: String,
    val idMovimiento: Int?
)

data class StatusEtiqueta(val idCamara: Int?, val factor: Int)


class Etiqueta(
    private val connection: SQLiteConnection,
    private val auditoria: AuditoriaEtiquetas
) {

    private inline fun <T> ejecutarEnTransaccion(
        block: () -> T
    ): T {

        connection.execSQL("BEGIN IMMEDIATE TRANSACTION")
        return try {
            val resultado = block()
            connection.execSQL("END TRANSACTION")
            resultado
        } catch (e: Throwable) {
            try {
                connection.execSQL("ROLLBACK TRANSACTION")
            } catch (_: Throwable) {
            }
            throw e
        }
    }

    private data class MovimientoSnapshot(
        val id: Int,
        val tipoMovimiento: String,
        val factor: Int
    )

    private fun obtenerMovimiento(
        tipoMovimiento: String
    ): MovimientoSnapshot? {

        var movimiento: MovimientoSnapshot? = null

        connection.prepare(
            """
        SELECT id, tipoMovimiento, factor
        FROM Movimiento
        WHERE tipoMovimiento = ?
        """.trimIndent()
        ).use { stmt ->

            stmt.bindText(1, tipoMovimiento)

            if (stmt.step()) {
                movimiento = MovimientoSnapshot(
                    id = stmt.getInt(0),
                    tipoMovimiento = stmt.getText(1),
                    factor = stmt.getInt(2)
                )
            }
        }

        return movimiento
    }

    fun getOneEtiqueta(id: String): UpdateEtiqueta? {
        var etiqueta: UpdateEtiqueta? = null

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
                    etiqueta = UpdateEtiqueta(
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

    private fun obtenerEtiquetaAnterior(
        id: Long
    ): EtiquetaAnterior? {

        var resultado: EtiquetaAnterior? = null

        connection.prepare(
            """
        SELECT
            etiquetaEscaneada,
            claveProducto,
            piezas,
            kilos,
            lote,
            idMovimiento
        FROM Etiquetas
        WHERE id = ?
        """.trimIndent()
        ).use { stmt ->

            stmt.bindLong(1, id)

            if (stmt.step()) {

                resultado = EtiquetaAnterior(
                    etiquetaEscaneada = stmt.getText(0),
                    claveProducto = stmt.getText(1),
                    piezas =
                        if (stmt.isNull(2)) "" else stmt.getText(2),
                    kilos = stmt.getText(3),
                    lote =
                        if (stmt.isNull(4)) "" else stmt.getText(4),
                    idMovimiento =
                        if (stmt.isNull(5)) null else stmt.getInt(5)
                )
            }
        }

        return resultado
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
                        idMovimiento = stmt.getInt(14)
                    )
                }
            }
        return etiqueta
    }

    private fun obtenerMovimiento(
        idMovimiento: Int?
    ): MovimientoSnapshot? {

        if (idMovimiento == null) {
            return null
        }

        var movimiento: MovimientoSnapshot? = null

        connection.prepare(
            """
        SELECT
            id,
            tipoMovimiento,
            factor
        FROM Movimiento
        WHERE id = ?
        """.trimIndent()
        ).use { stmt ->

            stmt.bindInt(1, idMovimiento)

            if (stmt.step()) {
                movimiento = MovimientoSnapshot(
                    id = stmt.getInt(0),
                    tipoMovimiento = stmt.getText(1),
                    factor = stmt.getInt(2)
                )
            }
        }

        return movimiento
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
        idMovimiento: Int? = null,
        origen: OrigenAuditoria = OrigenAuditoria.ESCANEO_COMPLETO
    ): Boolean {

        val observacion = notas ?: e.notas

        return try {

            ejecutarEnTransaccion {
                connection.prepare(
                    """
                INSERT INTO Etiquetas (
                    etiquetaEscaneada,
                    claveProducto,
                    piezas,
                    kilos,
                    lote,
                    numEmpaque,
                    fecha,
                    hora,
                    fechaEscaneo,
                    idZona,
                    idCamara,
                    idMovimiento,
                    turno,
                    escaneadoPor,
                    notas
                )
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
                    stmt.bindText(
                        9,
                        (e.fechaEscaneo ?: LocalDateTime.now()).toString()
                    )
                    stmt.bindInt(10, idZona)
                    stmt.bindInt(11, idCamara)

                    if (idMovimiento != null) {
                        stmt.bindInt(12, idMovimiento)
                    } else {
                        stmt.bindNull(12)
                    }

                    stmt.bindText(13, turno)
                    stmt.bindText(14, escaneadoPor)
                    stmt.bindText(15, observacion ?: "")

                    stmt.step()
                }

                var idEtiqueta = 0L

                connection.prepare(
                    "SELECT last_insert_rowid()"
                ).use { stmt ->

                    if (stmt.step()) {
                        idEtiqueta = stmt.getLong(0)
                    }
                }

                if (idEtiqueta <= 0) {
                    throw IllegalStateException(
                        "No fue posible obtener el ID de la etiqueta insertada"
                    )
                }

                val movimiento = obtenerMovimiento(idMovimiento)

                val mensaje = when (origen) {
                    OrigenAuditoria.ESCANEO_COMPLETO -> "Escaneo completo"
                    OrigenAuditoria.MANUAL -> "Etiqueta ingresada manualmente"
                    OrigenAuditoria.SISTEMA -> "Etiqueta registrada por el sistema"
                }

                auditoria.registrar(
                    RegistrarAuditoria(
                        idOperacion = java.util.UUID.randomUUID().toString(),
                        idEtiqueta = idEtiqueta,
                        etiquetaEscaneada = etiquetaEscaneada,
                        tipoEvento = TipoEventoAuditoria.REGISTRO,
                        origen = origen,
                        idMovimiento = movimiento?.id,
                        tipoMovimiento = movimiento?.tipoMovimiento,
                        factor = movimiento?.factor,
                        usuario = escaneadoPor,
                        mensaje = mensaje,
                        observacion = observacion,
                        fechaEvento = LocalDateTime.now().toString()
                    )
                )
            }

            Log.i(
                "EtiquetaDAO",
                "Etiqueta insertada correctamente [$etiquetaEscaneada]"
            )

            true

        } catch (ex: Exception) {

            Log.e(
                "EtiquetaDAO",
                "Error al insertar etiqueta [$etiquetaEscaneada]",
                ex
            )

            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun upsertEtiqueta(
        id: String,
        etiqueta: UpdateEtiqueta,
        usuario: String = "DESCONOCIDO"
    ): Boolean {

        return try {
            ejecutarEnTransaccion {
                val idEtiqueta = id.toLong()
                val anterior = obtenerEtiquetaAnterior(idEtiqueta)
                    ?: throw IllegalStateException(
                        "No existe la etiqueta con ID $id"
                    )
                val movimientoNuevo = obtenerMovimiento(etiqueta.tipoMovimiento)
                    ?: throw IllegalStateException("No existe el movimiento '${etiqueta.tipoMovimiento}'")

                val movimientoAnterior =
                    obtenerMovimiento(anterior.idMovimiento)

                connection.prepare(
                    """
                UPDATE Etiquetas
                SET
                    etiquetaEscaneada = ?,
                    claveProducto = ?,
                    piezas = ?,
                    kilos = ?,
                    lote = ?,
                    idMovimiento = ?
                WHERE id = ?
                """.trimIndent()
                ).use { stmt ->

                    stmt.bindText(1, etiqueta.etiquetaEscaneada)
                    stmt.bindText(2, etiqueta.claveProducto)
                    stmt.bindText(3, etiqueta.piezas)
                    stmt.bindText(4, etiqueta.kilos)
                    stmt.bindText(5, etiqueta.lote)
                    stmt.bindInt(6, movimientoNuevo.id)
                    stmt.bindLong(7, idEtiqueta)

                    stmt.step()
                }

                val idOperacion = java.util.UUID.randomUUID().toString()

                val fechaOperacion = LocalDateTime.now().toString()

                fun registrarCambio(
                    campo: String,
                    valorAnterior: String?,
                    valorNuevo: String?
                ) {

                    if (valorAnterior == valorNuevo) {
                        return
                    }

                    auditoria.registrar(
                        RegistrarAuditoria(
                            idOperacion = idOperacion,
                            idEtiqueta = idEtiqueta,
                            etiquetaEscaneada = etiqueta.etiquetaEscaneada,
                            tipoEvento = TipoEventoAuditoria.EDICION,
                            origen = OrigenAuditoria.MANUAL,
                            idMovimiento = movimientoNuevo.id,
                            tipoMovimiento = movimientoNuevo.tipoMovimiento,
                            factor = movimientoNuevo.factor,
                            usuario = usuario,
                            mensaje = "Etiqueta editada",
                            campoModificado = campo,
                            valorAnterior = valorAnterior,
                            valorNuevo = valorNuevo,
                            fechaEvento = fechaOperacion
                        )
                    )
                }

                registrarCambio(
                    campo = "etiquetaEscaneada",
                    valorAnterior = anterior.etiquetaEscaneada,
                    valorNuevo = etiqueta.etiquetaEscaneada
                )

                registrarCambio(
                    campo = "claveProducto",
                    valorAnterior = anterior.claveProducto,
                    valorNuevo = etiqueta.claveProducto
                )

                registrarCambio(
                    campo = "piezas",
                    valorAnterior = anterior.piezas,
                    valorNuevo = etiqueta.piezas
                )

                registrarCambio(
                    campo = "kilos",
                    valorAnterior = anterior.kilos,
                    valorNuevo = etiqueta.kilos
                )

                registrarCambio(
                    campo = "lote",
                    valorAnterior = anterior.lote,
                    valorNuevo = etiqueta.lote
                )

                registrarCambio(
                    campo = "tipoMovimiento",
                    valorAnterior = movimientoAnterior?.tipoMovimiento,
                    valorNuevo = movimientoNuevo.tipoMovimiento
                )
            }

            true

        } catch (e: Exception) {
            Log.e("EtiquetaDAO", "Error al editar etiqueta ID $id", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun eliminarEtiqueta(
        id: Long,
        usuario: String,
        origen: OrigenAuditoria = OrigenAuditoria.MANUAL
    ): Boolean {
        return try {
            ejecutarEnTransaccion {
                val anterior = obtenerEtiquetaAnterior(id)
                    ?: throw IllegalStateException("No existe la etiqueta con ID $id")
                val movimiento = obtenerMovimiento(anterior.idMovimiento)
                val idOperacion = java.util.UUID.randomUUID().toString()
                val fechaOperacion = LocalDateTime.now().toString()
                fun registrarValorEliminado(
                    campo: String,
                    valor: String?
                ) {

                    auditoria.registrar(
                        RegistrarAuditoria(
                            idOperacion = idOperacion,
                            idEtiqueta = id,
                            etiquetaEscaneada = anterior.etiquetaEscaneada,
                            tipoEvento = TipoEventoAuditoria.ELIMINACION,
                            origen = origen,
                            idMovimiento = movimiento?.id,
                            tipoMovimiento = movimiento?.tipoMovimiento,
                            factor = movimiento?.factor,
                            usuario = usuario,
                            mensaje = "Etiqueta eliminada",
                            campoModificado = campo,
                            valorAnterior = valor,
                            valorNuevo = null,
                            fechaEvento = fechaOperacion
                        )
                    )
                }
                registrarValorEliminado(
                    campo = "etiquetaEscaneada",
                    valor = anterior.etiquetaEscaneada
                )

                registrarValorEliminado(
                    campo = "claveProducto",
                    valor = anterior.claveProducto
                )

                registrarValorEliminado(
                    campo = "piezas",
                    valor = anterior.piezas
                )

                registrarValorEliminado(
                    campo = "kilos",
                    valor = anterior.kilos
                )

                registrarValorEliminado(
                    campo = "lote",
                    valor = anterior.lote
                )

                registrarValorEliminado(
                    campo = "tipoMovimiento",
                    valor = movimiento?.tipoMovimiento
                )
                connection.prepare(
                    """
                DELETE FROM Etiquetas
                WHERE id = ?
                """.trimIndent()
                ).use { stmt ->

                    stmt.bindLong(1, id)
                    stmt.step()
                }
            }

            Log.i(
                "EtiquetaDAO",
                "Etiqueta eliminada correctamente ID: $id"
            )

            true

        } catch (e: Exception) {

            Log.e(
                "EtiquetaDAO",
                "Error al eliminar etiqueta ID: $id",
                e
            )

            false
        }
    }
}