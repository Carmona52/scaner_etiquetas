package com.example.etiquetas.database

import android.util.Log
import androidx.sqlite.SQLiteConnection

data class MovimientoGuardado(
    val id: Int, val tipoMovimiento: String, val factor: Int
)

class Movimientos(private val connection: SQLiteConnection) {
    fun getAllMovimientos(): List<MovimientoGuardado> {
        val resultado = mutableListOf<MovimientoGuardado>()
        connection.prepare("SELECT id, tipoMovimiento, factor FROM Movimiento ORDER BY id ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun getMovimientosByFactor(factor: Int): List<MovimientoGuardado> {
        val resultado = mutableListOf<MovimientoGuardado>()
        connection.prepare("SELECT id, tipoMovimiento, factor FROM Movimiento WHERE factor = ? ORDER BY id ASC")
            .use { stmt ->
                stmt.bindInt(1, factor)
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun upsertMovimiento(tipoMovimiento: String, factor: Int) {
        connection.prepare("INSERT INTO Movimiento (tipoMovimiento, factor) VALUES (?, ?) ON CONFLICT(tipoMovimiento) DO UPDATE SET factor = excluded.factor")
            .use { stmt ->
                stmt.bindText(1, tipoMovimiento)
                stmt.bindInt(2, factor)
                stmt.step()
            }
    }


    fun deleteMovimiento(id: Int): Boolean {
        return try {
            connection.prepare("DELETE FROM Movimiento WHERE id = ?").use { stmt ->
                stmt.bindInt(1, id)
                stmt.step()
            }
            true
        } catch (e: Exception) {
            Log.e("MovimientosDAO", "Error al eliminar movimiento con ID: $id", e)
            false
        }
    }

    private fun mapearFila(stmt: androidx.sqlite.SQLiteStatement): MovimientoGuardado {
        return MovimientoGuardado(
            id = stmt.getInt(0), tipoMovimiento = stmt.getText(1), factor = stmt.getInt(2)
        )
    }
}