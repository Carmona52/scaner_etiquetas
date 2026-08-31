package com.example.etiquetas.database

import android.content.Context
import android.util.Log
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.etiquetas.database.methods.ActualizarCamara
import com.example.etiquetas.database.methods.Camaras
import com.example.etiquetas.database.methods.Etiqueta
import com.example.etiquetas.database.methods.Productos
import com.example.etiquetas.database.methods.Reportes
import com.example.etiquetas.database.methods.Zonas
import org.json.JSONArray
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import com.example.etiquetas.database.methods.AuditoriaEtiquetas

class DataBase private constructor(private val context: Context) {

    private val driver = BundledSQLiteDriver()
    private val connection: SQLiteConnection
    val movimientos: Movimientos
    val productos: Productos
    val camaras: Camaras
    val zonas: Zonas
    val etiquetas: Etiqueta
    val reportes: Reportes
    val auditoriaEtiquetas: AuditoriaEtiquetas

    init {
        val dbFile = context.getDatabasePath("etiquetas.db")
        dbFile.parentFile?.mkdirs()

        connection = driver.open(
            fileName = dbFile.absolutePath,
            flags = SQLITE_OPEN_READWRITE or
                    SQLITE_OPEN_CREATE or
                    SQLITE_OPEN_FULLMUTEX
        )

        connection.execSQL("PRAGMA foreign_keys = ON")

        movimientos = Movimientos(connection)
        productos = Productos(connection)
        camaras = Camaras(connection)
        zonas = Zonas(connection)
        reportes = Reportes(connection)
        auditoriaEtiquetas = AuditoriaEtiquetas(connection)
        etiquetas = Etiqueta(connection, auditoriaEtiquetas)
        inicializarBaseDeDatos()
    }

    companion object {
        @Volatile
        private var instance: DataBase? = null

        fun getInstance(context: Context): DataBase {
            return instance ?: synchronized(this) {
                instance ?: DataBase(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun inicializarBaseDeDatos() {
        crearTablas()
        crearIndices()
        crearTriggers()

        cargarCatalogoDesdeAssets()
        cargarZonas()
        cargarCamaras()
        cargarMovimientos()
    }

    private fun crearTablas() {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Articulos (
                claveProducto TEXT PRIMARY KEY NOT NULL UNIQUE,
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
            CREATE TABLE IF NOT EXISTS Tarimas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                idCamara INTEGER NOT NULL,
                idEtiqueta INTEGER NOT NULL,
                FOREIGN KEY (idCamara) REFERENCES Camaras(id)
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Movimiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipoMovimiento TEXT NOT NULL UNIQUE,
                factor INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS InventarioCamaras (
                idCamara INTEGER PRIMARY KEY,
                totalKilos INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (idCamara) REFERENCES Camaras(id)
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ConteoCestas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                idProducto TEXT NOT NULL,
                idCamara INTEGER NOT NULL,
                cantidadCestas INTEGER NOT NULL DEFAULT 0,
                totalKilos INTEGER NOT NULL DEFAULT 0,
                UNIQUE(idProducto, idCamara),
                FOREIGN KEY (idProducto) REFERENCES Articulos(claveProducto),
                FOREIGN KEY (idCamara) REFERENCES Camaras(id)
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
                idTarimas INTEGER,
                idMovimiento INTEGER,
                turno TEXT NOT NULL,
                escaneadoPor TEXT NOT NULL,
                notas TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (claveProducto) REFERENCES Articulos(claveProducto),
                FOREIGN KEY (idZona) REFERENCES Zonas(id),
                FOREIGN KEY (idCamara) REFERENCES Camaras(id),
                FOREIGN KEY (idTarimas) REFERENCES Tarimas(id),
                FOREIGN KEY (idMovimiento) REFERENCES Movimiento(id)
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS AuditoriaEtiquetas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                idOperacion TEXT NOT NULL,
                idEtiqueta INTEGER NOT NULL,
                etiquetaEscaneada TEXT NOT NULL,
                tipoEvento TEXT NOT NULL,
                origen TEXT NOT NULL,   
                campoModificado TEXT,
                valorAnterior TEXT,
                valorNuevo TEXT,
                idMovimiento INTEGER,
                tipoMovimiento TEXT,
                factor INTEGER,
                mensaje TEXT NOT NULL DEFAULT '',
                observacion TEXT,
                usuario TEXT NOT NULL, 
                fechaEvento TEXT NOT NULL
                )
            """.trimIndent()
        )
    }

    private fun crearIndices() {
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_camara ON Etiquetas(etiquetaEscaneada, idCamara, idZona, idMovimiento)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_tarimas ON Tarimas(idCamara, idEtiqueta)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_camara_conteo ON ConteoCestas(idCamara)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_status ON Etiquetas(etiquetaEscaneada, id DESC)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_camara_fecha ON Etiquetas(idCamara, fechaEscaneo)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_zona_fecha ON Etiquetas(idZona, fechaEscaneo)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_auditoria_etiqueta ON AuditoriaEtiquetas(idEtiqueta, fechaEvento DESC)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_auditoria_codigo ON AuditoriaEtiquetas(etiquetaEscaneada, fechaEvento DESC)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_auditoria_operacion ON AuditoriaEtiquetas(idOperacion)")
    }

    private fun crearTriggers() {
        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_INSERT
            AFTER INSERT ON Etiquetas
            BEGIN
                INSERT INTO InventarioCamaras (idCamara, totalKilos)
                VALUES (
                    NEW.idCamara,
                    CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                )
                ON CONFLICT(idCamara) DO UPDATE SET
                    totalKilos = totalKilos + (
                        CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                        COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                    );
            END;
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_UPDATE
            AFTER UPDATE ON Etiquetas
            BEGIN
                UPDATE InventarioCamaras
                SET totalKilos = totalKilos - (
                    CAST(ROUND(CAST(OLD.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento), 0)
                )
                WHERE idCamara = OLD.idCamara;

                INSERT INTO InventarioCamaras (idCamara, totalKilos)
                VALUES (
                    NEW.idCamara,
                    CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                )
                ON CONFLICT(idCamara) DO UPDATE SET
                    totalKilos = totalKilos + (
                        CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                        COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                    );
            END;
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_DELETE
            AFTER DELETE ON Etiquetas
            BEGIN
                UPDATE InventarioCamaras
                SET totalKilos = totalKilos - (
                    CAST(ROUND(CAST(OLD.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento), 0)
                )
                WHERE idCamara = OLD.idCamara;
            END;
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_cestas_INSERT
            AFTER INSERT ON Etiquetas
            BEGIN
                INSERT INTO ConteoCestas (idProducto, idCamara, cantidadCestas, totalKilos)
                VALUES (
                    NEW.claveProducto,
                    NEW.idCamara,
                    COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0),
                    CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                    COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                )
                ON CONFLICT(idProducto, idCamara) DO UPDATE SET
                    cantidadCestas = cantidadCestas + COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0),
                    totalKilos = totalKilos + (
                        CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) *
                        COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento), 0)
                    );
            END;
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_cestas_UPDATE
AFTER UPDATE ON Etiquetas
BEGIN
    UPDATE ConteoCestas
    SET
        cantidadCestas = cantidadCestas - COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento),0),
        totalKilos = totalKilos - (CAST(ROUND(CAST(OLD.kilos AS REAL) * 100) AS INTEGER) * COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento),0))
    WHERE idProducto = OLD.claveProducto AND idCamara = OLD.idCamara;
    INSERT INTO ConteoCestas (
        idProducto,
        idCamara,
        cantidadCestas,
        totalKilos
    )
    VALUES (
        NEW.claveProducto,
        NEW.idCamara,
        COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento),0),
        CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) * COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento),0)
    )

    ON CONFLICT(idProducto, idCamara)
    DO UPDATE SET
        cantidadCestas = cantidadCestas + COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento),0),
        totalKilos =totalKilos + (CAST(ROUND(CAST(NEW.kilos AS REAL) * 100) AS INTEGER) * COALESCE((SELECT factor FROM Movimiento WHERE id = NEW.idMovimiento),0));

END;
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_actualizar_cestas_DELETE
            AFTER DELETE ON Etiquetas
            BEGIN
                UPDATE ConteoCestas
                SET 
                 cantidadCestas = cantidadCestas - COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento), 0),
                totalKilos = totalKilos - (
                        CAST(ROUND(CAST(OLD.kilos AS REAL) * 100) AS INTEGER) *
                        COALESCE((SELECT factor FROM Movimiento WHERE id = OLD.idMovimiento), 0)
                    )
                WHERE idProducto = OLD.claveProducto AND idCamara = OLD.idCamara;
            END;
            """.trimIndent()
        )
    }

    private fun cargarCatalogoDesdeAssets() {
        leerJsonFromAssets("catalogo_productos.json") { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val cod = obj.getInt("COD")
                val descripcion = obj.getString("DESCRIPCION")
                productos.upsertProducto(cod.toString().padStart(3, '0'), descripcion)
            }
        }
    }

    private fun cargarZonas() {
        leerJsonFromAssets("catalogo_zonas.json") { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                zonas.upsertZona(
                    obj.getInt("numZona"), obj.getString("nombreZona"), obj.getString("descripcion")
                )
            }
        }
    }

    private fun cargarCamaras() {
        leerJsonFromAssets("catalogo_camaras.json") { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                camaras.upsertCamara(
                    ActualizarCamara(
                        idZona = obj.getInt("idZona"),
                        numCamara = obj.getInt("numCamara"),
                        nombreCamara = obj.getString("nombreCamara"),
                        descripcion = obj.getString("descripcion")
                    )
                )
            }
        }
    }

    private fun cargarMovimientos() {
        leerJsonFromAssets("catalogo_movimientos.json") { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                movimientos.upsertMovimiento(obj.getString("tipoMovimiento"), obj.getInt("factor"))
            }
        }
    }

    private inline fun leerJsonFromAssets(filename: String, block: (JSONArray) -> Unit) {
        try {
            val jsonTexto = context.assets.open(filename).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonTexto)
            block(jsonArray)
            Log.d("DataBase", "Catálogo cargado: ${jsonArray.length()} elementos desde $filename")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar $filename desde assets", e)
        }
    }
}