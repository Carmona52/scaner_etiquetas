package com.example.etiquetas.database

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.etiquetas.utils.Etiqueta
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
    val zona: String?,
    val camara: String?,
    val turno: String,
    val tipoMovimiento: String,
    val escaneadoPor: String,
    val notas: String,
    val numEmpaque: String? = null
)

data class productosGuardados(
    val claveProducto: String,
    val descripcion: String
)

data class ZonaGuardada(
    val id: Int,
    val numZona: Int,
    val nombreZona: String,
    val descripcion: String?
)

data class CamaraGuardada(
    val id: Int,
    val idZona: Int,
    val numCamara: Int,
    val nombreCamara: String,
    val descripcion: String?
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
        cargarZonas()
        cargarCamaras()
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
            CREATE TABLE IF NOT EXISTS Zonas (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                numZona INTEGER NOT NULL UNIQUE,
                nombreZona TEXT NOT NULL,
                descripcion TEXT
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Camaras (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                idZona INTEGER NOT NULL,
                numCamara INTEGER NOT NULL UNIQUE,
                nombreCamara TEXT NOT NULL,
                descripcion TEXT,
                FOREIGN KEY (idZona) REFERENCES Zonas(id)
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
                numEmpaque TEXT,
                fecha TEXT NOT NULL,
                hora TEXT NOT NULL,
                fechaEscaneo TEXT NOT NULL,
                idZona INTEGER NOT NULL,
                idCamara INTEGER NOT NULL,
                turno TEXT NOT NULL,
                tipoMovimiento TEXT NOT NULL,
                escaneadoPor TEXT NOT NULL,
                notas TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (claveProducto) REFERENCES Articulos(claveProducto),
                FOREIGN KEY (idZona) REFERENCES Zonas(id),
                FOREIGN KEY (idCamara) REFERENCES Camaras(id)
            )
            """.trimIndent()
        )

        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_camara ON Etiquetas(etiquetaEscaneada, idCamara)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_camara ON Camaras(numCamara)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_zona ON Zonas(numZona)")
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
                upsertArticulo(cod.toString().padStart(3, '0'), descripcion)
                cargados++
            }
            Log.d("DataBase", "Catálogo cargado: $cargados artículos")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar catálogo desde assets", e)
        }
    }

    private fun cargarZonas() {
        try {
            val jsonTexto =
                context.assets.open("catalogo_zonas.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonTexto)
            var cargados = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                upsertZona(
                    obj.getInt("numZona"),
                    obj.getString("nombreZona"),
                    obj.getString("descripcion")
                )
                cargados++
            }
            Log.d("DataBase", "Catálogo cargado: $cargados zonas")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar zonas desde assets", e)
        }
    }

    private fun cargarCamaras() {
        try {
            val jsonTexto =
                context.assets.open("catalogo_camaras.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonTexto)
            var cargados = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                upsertCamara(
                    obj.getInt("idZona"),
                    obj.getInt("numCamara"),
                    obj.getString("nombreCamara"),
                    obj.getString("descripcion")
                )
                cargados++
            }
            Log.d("DataBase", "Catálogo cargado: $cargados camaras")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar camaras desde assets", e)
        }
    }


    fun obtenerProductos(): List<productosGuardados> {
        val resultado = mutableListOf<productosGuardados>()
        connection.prepare("SELECT claveProducto, descripcion FROM Articulos ORDER BY claveProducto ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(productosGuardados(stmt.getText(0), stmt.getText(1)))
                }
            }
        return resultado
    }

    fun obtenerZonas(): List<ZonaGuardada> {
        val resultado = mutableListOf<ZonaGuardada>()
        connection.prepare("SELECT id, numZona, nombreZona, descripcion FROM Zonas ORDER BY numZona ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(
                        ZonaGuardada(
                            id = stmt.getInt(0),
                            numZona = stmt.getInt(1),
                            nombreZona = stmt.getText(2),
                            descripcion = stmt.getText(3)
                        )
                    )
                }
            }
        return resultado
    }

    fun obtenerCamarasPorZona(idZona: Int): List<CamaraGuardada> {
        val resultado = mutableListOf<CamaraGuardada>()
        connection.prepare(
            "SELECT id, idZona, numCamara, nombreCamara, descripcion FROM Camaras WHERE idZona = ? ORDER BY numCamara ASC"
        ).use { stmt ->
            stmt.bindInt(1, idZona)
            while (stmt.step()) {
                resultado.add(
                    CamaraGuardada(
                        id = stmt.getInt(0),
                        idZona = stmt.getInt(1),
                        numCamara = stmt.getInt(2),
                        nombreCamara = stmt.getText(3),
                        descripcion = stmt.getText(4)
                    )
                )
            }
        }
        return resultado
    }

    fun obtenerTodasLasCamaras(): List<CamaraGuardada> {
        val resultado = mutableListOf<CamaraGuardada>()
        connection.prepare("SELECT id, idZona, numCamara, nombreCamara, descripcion FROM Camaras ORDER BY numCamara ASC")
            .use { stmt ->
                while (stmt.step()) {
                    resultado.add(
                        CamaraGuardada(
                            id = stmt.getInt(0),
                            idZona = stmt.getInt(1),
                            numCamara = stmt.getInt(2),
                            nombreCamara = stmt.getText(3),
                            descripcion = stmt.getText(4)
                        )
                    )
                }
            }
        return resultado
    }

    fun obtenerNombreCamara(idCamara: Int): String? {
        var nombre: String? = null
        connection.prepare("SELECT nombreCamara FROM Camaras WHERE id = ?").use { stmt ->
            stmt.bindInt(1, idCamara)
            if (stmt.step()) nombre = stmt.getText(0)
        }
        return nombre
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

    fun upsertZona(numZona: Int, nombreZona: String, descripcion: String) {
        connection.prepare(
            "INSERT INTO Zonas (numZona, nombreZona, descripcion) VALUES (?, ?, ?) " +
                    "ON CONFLICT(numZona) DO UPDATE SET nombreZona = excluded.nombreZona, descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindInt(1, numZona)
            stmt.bindText(2, nombreZona)
            stmt.bindText(3, descripcion)
            stmt.step()
        }
    }

    fun upsertCamara(idZona: Int, numCamara: Int, nombreCamara: String, descripcion: String) {
        connection.prepare(
            "INSERT INTO Camaras (idZona, numCamara, nombreCamara, descripcion) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(numCamara) DO UPDATE SET idZona = excluded.idZona, nombreCamara = excluded.nombreCamara, descripcion = excluded.descripcion"
        ).use { stmt ->
            stmt.bindInt(1, idZona)
            stmt.bindInt(2, numCamara)
            stmt.bindText(3, nombreCamara)
            stmt.bindText(4, descripcion)
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
        idZona: Int,
        idCamara: Int,
        turno: String,
        escaneadoPor: String,
        etiquetaEscaneada: String,
        tipoMovimiento: String
    ): Boolean {
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
                stmt.bindText(6, e.numEmpaque ?: "")
                stmt.bindText(7, fecha)
                stmt.bindText(8, hora)
                stmt.bindText(9, (e.fechaEscaneo ?: LocalDateTime.now()).toString())
                stmt.bindInt(10, idZona)
                stmt.bindInt(11, idCamara)
                stmt.bindText(12, turno)
                stmt.bindText(13, tipoMovimiento)
                stmt.bindText(14, escaneadoPor)
                stmt.bindText(15, e.notas)
                stmt.step()
            }
            Log.i("Success", "Se insertó correctamente")
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
                   e.fecha, e.hora, e.fechaEscaneo, z.nombreZona, c.nombreCamara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas, e.numEmpaque
            FROM Etiquetas e
            LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
            LEFT JOIN Zonas z ON e.idZona = z.id
            LEFT JOIN Camaras c ON e.idCamara = c.id
            ORDER BY e.id ASC
            """.trimIndent()
        ).use { stmt ->
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }
        return resultado
    }

    fun obtenerReporteFiltrado(
        idZona: Int? = null,
        idCamara: Int? = null,
        turno: String? = null,
        movimiento: String? = null,
        fechaInicioISO: String? = null,
        fechaFinISO: String? = null
    ): List<EtiquetaGuardada> {
        val condiciones = mutableListOf<String>()
        val valores = mutableListOf<String>()

        if (idZona != null) condiciones.add("e.idZona = $idZona")
        if (idCamara != null) condiciones.add("e.idCamara = $idCamara")
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
                   e.fecha, e.hora, e.fechaEscaneo, z.nombreZona, c.nombreCamara, e.turno, e.tipoMovimiento, e.escaneadoPor, e.notas, e.numEmpaque
            FROM Etiquetas e
            LEFT JOIN Articulos a ON e.claveProducto = a.claveProducto
            LEFT JOIN Zonas z ON e.idZona = z.id
            LEFT JOIN Camaras c ON e.idCamara = c.id
            $whereClause
            ORDER BY e.id DESC
            """.trimIndent()
        ).use { stmt ->
            valores.forEachIndexed { index, valor -> stmt.bindText(index + 1, valor) }
            while (stmt.step()) {
                resultado.add(mapearFila(stmt))
            }
        }
        return resultado
    }

    private fun mapearFila(stmt: androidx.sqlite.SQLiteStatement): EtiquetaGuardada {
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

    /** Devuelve el idCamara donde esta etiqueta sigue "adentro" (última fila = Entrada), o null. */
    fun obtenerCamaraActualId(etiquetaEscaneada: String): Int? {
        var idCamaraActual: Int? = null
        connection.prepare(
            """
            SELECT e.idCamara
            FROM Etiquetas e
            INNER JOIN (
                SELECT idCamara, MAX(id) AS maxId
                FROM Etiquetas
                WHERE etiquetaEscaneada = ?
                GROUP BY idCamara
            ) ultimo ON e.id = ultimo.maxId
            WHERE e.tipoMovimiento = 'Entrada'
            LIMIT 1
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, etiquetaEscaneada)
            if (stmt.step()) idCamaraActual = stmt.getInt(0)
        }
        return idCamaraActual
    }

    fun hacerCorteDeInventario() {
        connection.execSQL(
            """
            DELETE FROM Etiquetas
            WHERE id NOT IN (
                SELECT e.id
                FROM Etiquetas e
                INNER JOIN (
                    SELECT etiquetaEscaneada, idCamara, MAX(id) AS maxId
                    FROM Etiquetas
                    GROUP BY etiquetaEscaneada, idCamara
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