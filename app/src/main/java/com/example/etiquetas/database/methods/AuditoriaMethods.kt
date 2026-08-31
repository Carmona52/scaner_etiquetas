package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection

enum class TipoEventoAuditoria {
    REGISTRO,
    EDICION,
    ELIMINACION
}

enum class OrigenAuditoria {
    ESCANEO_COMPLETO,
    MANUAL,
    SISTEMA
}

data class AuditoriaEtiqueta(
    val id: Long,
    val idOperacion: String,
    val idEtiqueta: Long,
    val etiquetaEscaneada: String,
    val tipoEvento: String,
    val origen: String,
    val campoModificado: String?,
    val valorAnterior: String?,
    val valorNuevo: String?,
    val idMovimiento: Int?,
    val tipoMovimiento: String?,
    val factor: Int?,
    val mensaje: String,
    val observacion: String?,
    val usuario: String,
    val fechaEvento: String
)

data class RegistrarAuditoria(
    val idOperacion: String,
    val idEtiqueta: Long,
    val etiquetaEscaneada: String,
    val tipoEvento: TipoEventoAuditoria,
    val origen: OrigenAuditoria,

    val idMovimiento: Int?,
    val tipoMovimiento: String?,
    val factor: Int?,

    val usuario: String,
    val mensaje: String,

    val observacion: String? = null,

    val campoModificado: String? = null,
    val valorAnterior: String? = null,
    val valorNuevo: String? = null,

    val fechaEvento: String
)

class AuditoriaEtiquetas(
    private val connection: SQLiteConnection
) {

    fun registrar(auditoria: RegistrarAuditoria) {

        connection.prepare(
            """
            INSERT INTO AuditoriaEtiquetas (
                idOperacion,
                idEtiqueta,
                etiquetaEscaneada,
                tipoEvento,
                origen,
                campoModificado,
                valorAnterior,
                valorNuevo,
                idMovimiento,
                tipoMovimiento,
                factor,
                mensaje,
                observacion,
                usuario,
                fechaEvento
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->

            stmt.bindText(1, auditoria.idOperacion)
            stmt.bindLong(2, auditoria.idEtiqueta)
            stmt.bindText(3, auditoria.etiquetaEscaneada)
            stmt.bindText(4, auditoria.tipoEvento.name)
            stmt.bindText(5, auditoria.origen.name)

            if (auditoria.campoModificado != null) {
                stmt.bindText(6, auditoria.campoModificado)
            } else {
                stmt.bindNull(6)
            }

            if (auditoria.valorAnterior != null) {
                stmt.bindText(7, auditoria.valorAnterior)
            } else {
                stmt.bindNull(7)
            }

            if (auditoria.valorNuevo != null) {
                stmt.bindText(8, auditoria.valorNuevo)
            } else {
                stmt.bindNull(8)
            }

            if (auditoria.idMovimiento != null) {
                stmt.bindInt(9, auditoria.idMovimiento)
            } else {
                stmt.bindNull(9)
            }

            if (auditoria.tipoMovimiento != null) {
                stmt.bindText(10, auditoria.tipoMovimiento)
            } else {
                stmt.bindNull(10)
            }

            if (auditoria.factor != null) {
                stmt.bindInt(11, auditoria.factor)
            } else {
                stmt.bindNull(11)
            }

            stmt.bindText(12, auditoria.mensaje)

            if (auditoria.observacion != null) {
                stmt.bindText(13, auditoria.observacion)
            } else {
                stmt.bindNull(13)
            }

            stmt.bindText(14, auditoria.usuario)
            stmt.bindText(15, auditoria.fechaEvento)

            stmt.step()
        }
    }

    fun getHistorialEtiqueta(
        idEtiqueta: Long
    ): List<AuditoriaEtiqueta> {

        val resultado = mutableListOf<AuditoriaEtiqueta>()

        connection.prepare(
            """
            SELECT
                id,
                idOperacion,
                idEtiqueta,
                etiquetaEscaneada,
                tipoEvento,
                origen,
                campoModificado,
                valorAnterior,
                valorNuevo,
                idMovimiento,
                tipoMovimiento,
                factor,
                mensaje,
                observacion,
                usuario,
                fechaEvento
            FROM AuditoriaEtiquetas
            WHERE idEtiqueta = ?
            ORDER BY id DESC
            """.trimIndent()
        ).use { stmt ->
            stmt.bindLong(1, idEtiqueta)

            while (stmt.step()) {

                resultado.add(
                    AuditoriaEtiqueta(
                        id = stmt.getLong(0),
                        idOperacion = stmt.getText(1),
                        idEtiqueta = stmt.getLong(2),
                        etiquetaEscaneada = stmt.getText(3),
                        tipoEvento = stmt.getText(4),
                        origen = stmt.getText(5),
                        campoModificado = if (stmt.isNull(6)) null else stmt.getText(6),
                        valorAnterior = if (stmt.isNull(7)) null else stmt.getText(7),
                        valorNuevo = if (stmt.isNull(8)) null else stmt.getText(8),
                        idMovimiento = if (stmt.isNull(9)) null else stmt.getInt(9),
                        tipoMovimiento = if (stmt.isNull(10)) null else stmt.getText(10),
                        factor = if (stmt.isNull(11)) null else stmt.getInt(11),
                        mensaje = stmt.getText(12),
                        observacion = if (stmt.isNull(13)) null else stmt.getText(13),
                        usuario = stmt.getText(14),
                        fechaEvento = stmt.getText(15)
                    )
                )
            }
        }

        return resultado
    }
}