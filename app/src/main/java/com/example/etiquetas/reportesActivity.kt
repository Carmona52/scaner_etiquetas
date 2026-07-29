package com.example.etiquetas

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.EtiquetaGuardada
import com.example.etiquetas.databinding.FragmentReportesBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.appcompat.app.AlertDialog

class reportesActivity : Fragment() {

    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DataBase

    private var camaraSeleccionada: String? = null
    private var turnoSeleccionado: String? = null
    private var movimientoSeleccionado: String? = null
    private var fechaSeleccionadaISO: String? = null
    private var fechaInicioISO: String? = null
    private var fechaFinISO: String? = null

    private val camaras = arrayOf(
        "Camara 1", "Camara 2", "Camara 3", "Camara 4", "Camara 5",
        "Camara 6", "Camara 7", "Camara 8", "Camara 9"
    )
    private val turnos = arrayOf("Turno 1", "Turno 2", "Turno 3")
    private val movimientos = arrayOf("Entrada", "Salida", "Ambos")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())
        configurarSelectores()
        configurarEventos()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun configurarEventos() {
        binding.generarReporteGeneral.setOnClickListener { generarReporteGeneral() }
        binding.generarReporte.setOnClickListener { generarReporteEspecifico() }
        binding.catalogoProducts.setOnClickListener { verCatalogoProductos() }

        binding.btnFechaInicio.setOnClickListener {
            mostrarSelectorFecha { year, month, day ->
                binding.btnFechaInicio.text = "%02d/%02d/%04d".format(day, month + 1, year)
                fechaInicioISO = "%04d-%02d-%02d".format(year, month + 1, day)
            }
        }

        binding.btnFechaFin.setOnClickListener {
            mostrarSelectorFecha { year, month, day ->
                binding.btnFechaFin.text = "%02d/%02d/%04d".format(day, month + 1, year)
                fechaFinISO = "%04d-%02d-%02d".format(year, month + 1, day)
            }
        }
    }

    private fun mostrarSelectorFecha(onDateSelected: (Int, Int, Int) -> Unit) {
        val calendario = Calendar.getInstance()
        val year = calendario.get(Calendar.YEAR)
        val month = calendario.get(Calendar.MONTH)
        val day = calendario.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(), { _, y, m, d ->
                onDateSelected(y, m, d)
            }, year, month, day
        ).show()
    }

    private fun configurarSelectores() {
        val listaCamaras = camaras.toList()
        val listaTurnos = turnos.toList()
        val listaMovimientos = movimientos.toList()

        val adapterCamara =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaCamaras)
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectorCamara.adapter = adapterCamara
        binding.selectorCamara.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    camaraSeleccionada = listaCamaras[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        val adapterMovimiento =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaMovimientos)
        adapterMovimiento.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectorMovimiento.adapter = adapterMovimiento
        binding.selectorMovimiento.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    movimientoSeleccionado = if (position == 2) null else listaMovimientos[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        val adapterTurno =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaTurnos)
        adapterTurno.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectorTurno.adapter = adapterTurno
        binding.selectorTurno.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                turnoSeleccionado = listaTurnos[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generarReporteGeneral() {
        val datos = db.obtenerReporte()

        if (datos.isEmpty()) {
            Toast.makeText(requireContext(), "No hay movimientos registrados", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar corte de inventario")
            .setMessage(
                "Esto generará el reporte general del día y eliminará de la app los " +
                        "movimientos con ciclo completo (Entrada + Salida).\n\n" +
                        "Las etiquetas que sigan dentro de una cámara se conservarán como " +
                        "inventario inicial para mañana.\n\n" +
                        "Esta acción no se puede deshacer. ¿Deseas continuar?"
            )
            .setPositiveButton("Sí, generar corte") { _, _ ->
                ejecutarReporteYCorte(datos)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ejecutarReporteYCorte(datos: List<EtiquetaGuardada>) {
        val exportoConExito = exportarCSV(datos, "reporte_general_corte")

        if (exportoConExito) {
            db.hacerCorteDeInventario()
            Toast.makeText(
                requireContext(),
                "Corte realizado — las etiquetas que siguen dentro de una cámara se conservaron para mañana",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "El corte NO se realizó porque el reporte no pudo guardarse — los datos siguen intactos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generarReporteEspecifico() {
        val datos = db.obtenerReporteFiltrado(
            camara = camaraSeleccionada,
            turno = turnoSeleccionado,
            movimiento = movimientoSeleccionado,
            fechaInicioISO = fechaInicioISO,
            fechaFinISO = fechaFinISO
        )

        if (datos.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay movimientos con esos filtros",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        exportarCSV(datos, "reporte_especifico")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportarCSV(datos: List<EtiquetaGuardada>, prefijoNombre: String): Boolean {
        val formatoArchivo = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss")
        val formatoCarpeta = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val ahora = LocalDateTime.now()

        val carpetaFecha = ahora.format(formatoCarpeta)
        val nombreArchivo = "${prefijoNombre}_${ahora.format(formatoArchivo)}.csv"

        return try {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Download/Etiquetas/Reportes/$carpetaFecha/"
                )
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(
                        "Etiqueta,Clave Producto,Descripcion,Piezas,Kilos,Lote,Fecha,Hora,Fecha Escaneo,Zona,Camara,Turno,Movimiento,Escaneado por,Notas\n".toByteArray()
                    )
                    datos.forEach { e ->
                        outputStream.write(
                            "=\"${e.etiquetaEscaneada}\",${e.claveProducto},${e.descripcionArticulo},${e.piezas},${e.kilos},${e.lote},${e.fecha},${e.hora},${e.fechaEscaneo},${e.zona},${e.camara},${e.turno},${e.tipoMovimiento},${e.escaneadoPor},${e.notas}\n".toByteArray()
                        )
                    }
                }

                Toast.makeText(
                    requireContext(),
                    "Reporte guardado en Etiquetas/Reportes/$carpetaFecha/$nombreArchivo",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else {
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT)
                    .show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al generar reporte: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    fun verCatalogoProductos() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SelectProductos())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db.close()
        _binding = null
    }
}