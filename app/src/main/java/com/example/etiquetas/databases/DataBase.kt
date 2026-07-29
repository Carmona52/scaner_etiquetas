package com.example.etiquetas.database

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.etiquetas.Etiqueta
import org.json.JSONArray
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
    val zona: String,
    val camara: String,
    val turno: String,
    val tipoMovimiento: String,
    val escaneadoPor: String,
    val notas: String
)

data class productosGuardados(
    val claveProducto: String,
    val descripcion: String
)

class DataBase(private val context: Context) {

    private val driver = BundledSQLiteDriver()
    private val connection: SQLiteConnection

    init {
        val dbFile = context.getDatabasePath("etiquetas.db")
        dbFile.parentFile?.mkdirs()
        connection = driver.open(dbFile.absolutePath)
        crearTablas()
        cargarCatalogoDesdeAssets()
    }

    private fun crearTablas() {
        connection.execSQL(
            """
        CREATE TABLE IF NOT EXISTS Articulos (
            claveProducto TEXT PRIMARY KEY,
            descripcion TEXT NOT NULL
        )
        """.trimIndent()
        )

        connection.execSQL(
            """
        CREATE TABLE IF NOT EXISTS Etiquetas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            etiquetaEscaneada TEXT NOT NULL,
            claveProducto TEXT NOT NULL,
            piezas TEXT,
            kilos TEXT NOT NULL,
            lote TEXT,
            fecha TEXT NOT NULL,
            hora TEXT NOT NULL,
            fechaEscaneo TEXT NOT NULL,
            zona TEXT NOT NULL,
            camara TEXT NOT NULL,
            turno TEXT NOT NULL,
            tipoMovimiento TEXT NOT NULL,
            escaneadoPor TEXT NOT NULL,
            notas TEXT NOT NULL DEFAULT '',
            FOREIGN KEY (claveProducto) REFERENCES Articulos(claveProducto)
        )
        """.trimIndent()
        )

        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_etiqueta_camara ON Etiquetas(etiquetaEscaneada, camara)"
        )
    }

    private fun cargarCatalogoDesdeAssets() {
        try {
            val jsonTexto = context.assets.open("catalogo_productos.json").bufferedReader()
                .use { it.readText() }

            val jsonArray = JSONArray(jsonTexto)
            var cargados = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val cod = obj.getInt("COD")
                val descripcion = obj.getString("DESCRIPCION")
                val claveNormalizada = cod.toString().padStart(3, '0')

                upsertArticulo(claveNormalizada, descripcion)
                cargados++
            }

            Log.d("DataBase", "Catálogo cargado: $cargados artículos")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar catálogo desde assets", e)
        }
    }

    fun obtenerProductos(): List<productosGuardados> {
        val resultado = mutableListOf<productosGuardados>()

        connection.prepare("SELECT claveProducto, descripcion FROM Articulos").use { stmt ->
            while (stmt.step()) {
                resultado.add(
                    productosGuardados(
                        claveProducto = stmt.getText(0),
                        descripcion = stmt.getText(1)
                    )
                )
            }
        }
        return resultado
    }

    fun upsertArticulo(claveProducto: String, descripcion: String) {
        connection.prepare(
            "INSERT INTO Articulos (claveProducto, descripcion) VALUES (?, ?) " +
                    "ON CONFLICT(claveProducto) DO UPDATE SET descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindText(1, claveProducto)
            stmt.bindText(2, descripcion)
            stmt.step()
        }
    }

    fun existeArticulo(claveProducto: String): Boolean {
        var existe = false
        connection.prepare("SELECT 1 FROM Articulos WHERE claveProducto = ?").use { stmt ->
            stmt.bindText(1, claveProducto)
            existe = stmt.step()
        }
        return existe
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertarEtiqueta(
        e: Etiqueta,
        fecha: String,
        hora: String,
        zona: String,
        camara: String,
        turno: String,
        escaneadoPor: String,
        etiquetaEscaneada: String,
        tipoMovimiento: String
    ): Boolean {
        return try {
            connection.prepare(
                """
            INSERT INTO Etiquetas
            (etiquetaEscaneada, claveProducto, piezas, kilos, lote, fecha, hora, fechaEscaneo, zona, camara, turno, tipoMovimiento, escaneadoPor, notas)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ).use { stmt ->
                stmt.bindText(1, etiquetaEscaneada)
                stmt.bindText(2, e.claveProducto)
                stmt.bindText(3, e.piezas ?: "")
                stmt.bindText(4, e.kilos)
                stmt.bindText(5, e.lote ?: "")
                stmt.bindText(6, fecha)
                stmt.bindText(7, hora)
                stmt.bindText(8, (e.fechaEscaneo ?: LocalDateTime.now()).toString())
                stmt.bindText(9, zona)
                stmt.bindText(10, camara)
                stmt.bindText(11, turno)
                stmt.bindText(12, tipoMovimiento)
                stmt.bindText(13, escaneadoPor)
                stmt.bindText(14, e.notas)
                stmt.step()
            }
            Log.i("Succes", "Se inserto correctamente")
            true
        } catch (ex: Exception) {
            Log.e("DataBase", "Error al insertar etiqueta [$etiquetaEscaneada]", ex)
            false
        }
    }

    fun obtenerReporte(): List<EtiquetaGuardada> {
        val resultado = mutableListOf<EtiquetaGuardada>()

        connection.prepare(
            """
        SELECT e.id, e.etiquetaEscaneada, e.claveProducto, a.descripcion, e.piezas, e.kilos, e.lote,
               e.fecha, e.hora, e.fechaEscaneo, e.zona, e.camara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas
        FROM Etiquetas e
        INNER JOIN Articulos a ON e.claveProducto = a.claveProducto
        ORDER BY e.id ASC
        """.trimIndent()
        ).use { stmt ->
            while (stmt.step()) {
                resultado.add(
                    EtiquetaGuardada(
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
                        notas = stmt.getText(15)
                    )
                )
            }
        }

        return resultado
    }

    fun obtenerReporteFiltrado(
        zona: String? = null,
        camara: String? = null,
        turno: String? = null,
        movimiento: String? = null,
        fechaInicioISO: String? = null,
        fechaFinISO: String? = null
    ): List<EtiquetaGuardada> {
        val condiciones = mutableListOf<String>()
        val valores = mutableListOf<String>()

        if (zona != null) {
            condiciones.add("e.zona = ?"); valores.add(zona)
        }
        if (camara != null) {
            condiciones.add("e.camara = ?"); valores.add(camara)
        }
        if (turno != null) {
            condiciones.add("e.turno = ?"); valores.add(turno)
        }
        if (movimiento != null) {
            condiciones.add("e.tipoMovimiento = ?"); valores.add(movimiento)
        }


        if (fechaInicioISO != null && fechaFinISO != null) {
            condiciones.add("e.fechaEscaneo >= ? AND e.fechaEscaneo <= ?")
            valores.add("${fechaInicioISO}T00:00:00")
            valores.add("${fechaFinISO}T23:59:59")
        } else if (fechaInicioISO != null) {
            condiciones.add("e.fechaEscaneo >= ?")
            valores.add("${fechaInicioISO}T00:00:00")
        } else if (fechaFinISO != null) {
            condiciones.add("e.fechaEscaneo <= ?")
            valores.add("${fechaFinISO}T23:59:59")
        }

        val whereClause =
            if (condiciones.isNotEmpty()) "WHERE " + condiciones.joinToString(" AND ") else ""

        val resultado = mutableListOf<EtiquetaGuardada>()

        connection.prepare(
            """
            SELECT e.id, e.etiquetaEscaneada, e.claveProducto, a.descripcion, e.piezas, e.kilos, e.lote,
                   e.fecha, e.hora, e.fechaEscaneo, e.zona, e.camara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas
            FROM Etiquetas e
            INNER JOIN Articulos a ON e.claveProducto = a.claveProducto
            $whereClause
            ORDER BY e.id ASC
            """.trimIndent()
        ).use { stmt ->
            valores.forEachIndexed { index, valor -> stmt.bindText(index + 1, valor) }

            while (stmt.step()) {
                resultado.add(
                    EtiquetaGuardada(
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
                        notas = stmt.getText(15)
                    )
                )
            }
        }
        return resultado
    }

    fun obtenerUltimoMovimiento(etiquetaEscaneada: String, camara: String): String? {
        var ultimoMovimiento: String? = null

        connection.prepare(
            "SELECT tipoMovimiento FROM Etiquetas WHERE etiquetaEscaneada = ? AND camara = ? ORDER BY id DESC LIMIT 1"
        ).use { stmt ->
            stmt.bindText(1, etiquetaEscaneada)
            stmt.bindText(2, camara)
            if (stmt.step()) {
                ultimoMovimiento = stmt.getText(0)
            }
        }

        return ultimoMovimiento
    }


    fun obtenerCamaraActual(etiquetaEscaneada: String): String? {
        var camaraActual: String? = null

        connection.prepare(
            """
        SELECT e.camara
        FROM Etiquetas e
        INNER JOIN (
            SELECT camara, MAX(id) AS maxId
            FROM Etiquetas
            WHERE etiquetaEscaneada = ?
            GROUP BY camara
        ) ultimo ON e.id = ultimo.maxId
        WHERE e.tipoMovimiento = 'Entrada'
        LIMIT 1
        """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, etiquetaEscaneada)
            if (stmt.step()) {
                camaraActual = stmt.getText(0)
            }
        }

        return camaraActual
    }

    fun hacerCorteDeInventario() {
        connection.execSQL(
            """
        DELETE FROM Etiquetas
        WHERE id NOT IN (
            SELECT e.id
            FROM Etiquetas e
            INNER JOIN (
                SELECT etiquetaEscaneada, camara, MAX(id) AS maxId
                FROM Etiquetas
                GROUP BY etiquetaEscaneada, camara
            ) ultimo ON e.id = ultimo.maxId
            WHERE e.tipoMovimiento = 'Entrada'
        )
        """.trimIndent()
        )
    }

    fun limpiarEtiquetas() {
        connection.execSQL("DELETE FROM Etiquetas")
    }

    fun eliminarUltimaEtiqueta() {
        connection.execSQL("DELETE FROM Etiquetas WHERE id = (SELECT MAX(id) FROM Etiquetas)")
    }

    fun close() {
        connection.close()
    }
}