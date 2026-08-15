package com.example.etiquetas.database

import android.content.Context
import android.util.Log
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.etiquetas.database.methods.ActualizarCamara
import com.example.etiquetas.database.methods.Camaras
import com.example.etiquetas.database.methods.Etiqueta
import com.example.etiquetas.database.methods.Reportes
import com.example.etiquetas.database.methods.Productos
import com.example.etiquetas.database.methods.Zonas
import org.json.JSONArray

class DataBase private constructor(private val context: Context) {

    val movimientos: Movimientos
    val productos: Productos
    val camaras: Camaras
    val zonas: Zonas
    val etiquetas: Etiqueta
    val reportes: Reportes
    private val driver = BundledSQLiteDriver()
    private val connection: SQLiteConnection

    init {
        val dbFile = context.getDatabasePath("etiquetas.db")
        dbFile.parentFile?.mkdirs()
        connection = driver.open(dbFile.absolutePath)

        movimientos = Movimientos(connection)
        productos = Productos(connection)
        camaras = Camaras(connection)
        zonas = Zonas(connection)
        etiquetas = Etiqueta(connection)
        reportes = Reportes(connection)

        crearTablas()
        cargarCatalogoDesdeAssets()
        cargarZonas()
        cargarCamaras()
        cargarMovimientos()
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
                factor INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent()
        )

        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS InventarioCamaras (
                    idCamara INTEGER PRIMARY KEY,
                    totalKilos REAL NOT NULL DEFAULT 0.0,
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
                tipoMovimiento TEXT NOT NULL,
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

        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_etiqueta_camara ON Etiquetas(etiquetaEscaneada, idCamara, idZona, idMovimiento)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_camara ON Camaras(numCamara)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_zona ON Zonas(numZona)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS idx_tarimas ON Tarimas(idCamara, idEtiqueta)")

        connection.execSQL(
            """
        CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_INSERT
        AFTER INSERT ON Etiquetas
        BEGIN
            INSERT INTO InventarioCamaras (idCamara, totalKilos)
            VALUES (
                NEW.idCamara, 
                (CAST(NEW.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = NEW.tipoMovimiento), 1))
            )
            ON CONFLICT(idCamara) DO UPDATE SET
                totalKilos = totalKilos + (CAST(NEW.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = NEW.tipoMovimiento), 1));
        END;
        """.trimIndent()
        )

        connection.execSQL(
            """
        CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_UPDATE
        AFTER UPDATE ON Etiquetas
        BEGIN
            -- Restar el valor anterior
            UPDATE InventarioCamaras 
            SET totalKilos = totalKilos - (CAST(OLD.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = OLD.tipoMovimiento), 1))
            WHERE idCamara = OLD.idCamara;

            -- Sumar el nuevo valor
            INSERT INTO InventarioCamaras (idCamara, totalKilos)
            VALUES (
                NEW.idCamara, 
                (CAST(NEW.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = NEW.tipoMovimiento), 1))
            )
            ON CONFLICT(idCamara) DO UPDATE SET
                totalKilos = totalKilos + (CAST(NEW.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = NEW.tipoMovimiento), 1));
        END;
        """.trimIndent()
        )

        connection.execSQL(
            """
        CREATE TRIGGER IF NOT EXISTS trg_actualizar_kilos_camara_DELETE
        AFTER DELETE ON Etiquetas
        BEGIN
            UPDATE InventarioCamaras 
            SET totalKilos = totalKilos - (CAST(OLD.kilos AS REAL) * COALESCE((SELECT factor FROM Movimiento WHERE tipoMovimiento = OLD.tipoMovimiento), 1))
            WHERE idCamara = OLD.idCamara;
        END;
        """.trimIndent()
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
                productos.upsertProducto(cod.toString().padStart(3, '0'), descripcion)
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
                zonas.upsertZona(
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
                camaras.upsertCamara(
                    ActualizarCamara(
                        idZona = obj.getInt("idZona"),
                        numCamara = obj.getInt("numCamara"),
                        nombreCamara = obj.getString("nombreCamara"),
                        descripcion = obj.getString("descripcion")
                    )
                )
                cargados++
            }
            Log.d("DataBase", "Catálogo cargado: $cargados camaras")
        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar camaras desde assets", e)
        }
    }

    private fun cargarMovimientos() {
        try {
            val jsonTexto = context.assets.open("catalogo_movimientos.json").bufferedReader()
                .use { it.readText() }
            val jsonArray = JSONArray(jsonTexto)
            var cargados = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                movimientos.upsertMovimiento(obj.getString("tipoMovimiento"), obj.getInt("factor"))
                cargados++
            }
            Log.d("DataBase", "Catálogo cargado: $cargados movimientos")

        } catch (e: Exception) {
            Log.e("DataBase", "Error al cargar movimientos desde assets", e)
        }

    }
}