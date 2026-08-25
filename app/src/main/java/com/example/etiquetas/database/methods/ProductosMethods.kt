package com.example.etiquetas.database.methods

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

data class productosGuardados(
    val claveProducto: String, val descripcion: String
)

class Productos(private val connection: SQLiteConnection) {
    fun getAllProductos(): List<productosGuardados> {
        val resultado = mutableListOf<productosGuardados>()
        connection.prepare("SELECT claveProducto, descripcion FROM Articulos ORDER BY claveProducto ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun getProductosPaginados(limit: Int, offset: Int): List<productosGuardados> {
        val resultado = mutableListOf<productosGuardados>()
        connection.prepare("SELECT claveProducto, descripcion FROM Articulos ORDER BY claveProducto ASC LIMIT ? OFFSET ?")
            .use { stmt ->
                stmt.bindLong(1, limit.toLong())
                stmt.bindLong(2, offset.toLong())
                while (stmt.step()) {
                    resultado.add(mapearFila(stmt))
                }
            }
        return resultado
    }

    fun upsertProducto(claveProducto: String, descripcion: String) {
        connection.prepare(
            "INSERT INTO Articulos (claveProducto, descripcion) VALUES (?, ?) " + "ON CONFLICT(claveProducto) DO UPDATE SET descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindText(1, claveProducto)
            stmt.bindText(2, descripcion)
            stmt.step()
        }
    }

    private fun mapearFila(stmt: SQLiteStatement): productosGuardados {
        return productosGuardados(
            claveProducto = stmt.getText(0), descripcion = stmt.getText(1)
        )
    }
}
