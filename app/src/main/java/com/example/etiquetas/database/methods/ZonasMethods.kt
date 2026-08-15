package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

data class ZonaGuardada(
    val id: Int, val numZona: Int, val nombreZona: String, val descripcion: String?
)

class Zonas(private val connection: SQLiteConnection) {
    fun getAllZonas(): List<ZonaGuardada> {
        val resultado = mutableListOf<ZonaGuardada>()
        connection.prepare("SELECT id, numZona, nombreZona, descripcion FROM Zonas ORDER BY numZona ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun upsertZona(numZona: Int, nombreZona: String, descripcion: String) {
        connection.prepare(
            "INSERT INTO Zonas (numZona, nombreZona, descripcion) VALUES (?, ?, ?) " + "ON CONFLICT(numZona) DO UPDATE SET nombreZona = excluded.nombreZona, descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindInt(1, numZona)
            stmt.bindText(2, nombreZona)
            stmt.bindText(3, descripcion)
            stmt.step()
        }
    }

    private fun mapearFila(stmt: SQLiteStatement): ZonaGuardada {
        return ZonaGuardada(
            id = stmt.getInt(0),
            numZona = stmt.getInt(1),
            nombreZona = stmt.getText(2),
            descripcion = stmt.getText(3)
        )
    }
}
