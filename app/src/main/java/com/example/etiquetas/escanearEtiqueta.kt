package com.example.etiquetas

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import java.io.File
import java.time.LocalDateTime
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.EtiquetaGuardada

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private var zonaSeleccionada: String? = null
    private var camaraSeleccionada: String? = null
    private var turnoSeleccionado: String? = null
    private var movimientoSeleccionado: String? = null
    private lateinit var db: DataBase

    private val zonaCamaraMap = mapOf(
        "Zona 1" to listOf("Camara de Fresco 1", "Camara 3"),
        "Zona 2" to listOf("Camara 2", "Camara 4"),
        "Zona 3" to listOf("Camara 5"),
        "Zona 4" to listOf("Camara 6", "Camara de Fresco 7"),
        "Zona 5" to listOf("Camara 8", "Camara 9")
    )

    private val turnos = arrayOf("Turno 1", "Turno 2", "Turno 3")

    private val movimientos = arrayOf("Entrada", "Salida")

    private val scanReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val codigo = when (intent.action) {
                ACTION_SUNMI -> intent.getStringExtra(EXTRA_SUNMI)
                ACTION_ZEBRA -> intent.getStringExtra(EXTRA_ZEBRA)
                else -> null
            }
            if (!codigo.isNullOrEmpty()) {
                guardarEtiqueta(codigo)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = EscanearEtiquetaFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())

        configurarSelectores()
        configurarEventos()


    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ACTION_SUNMI)
            addAction(ACTION_ZEBRA)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        ContextCompat.registerReceiver(
            requireContext(), scanReceiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(scanReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun configurarEventos() {
        binding.btnCancelar.setOnClickListener { cancelarEscaneo() }
        binding.generarReporte.setOnClickListener { generarReportePantalla() }
    }

    private fun configurarSelectores() {
        val zonas = zonaCamaraMap.keys.toList()
        val listaTurnos = turnos.toList()
        val listaMovimientos = movimientos.toList()

        val adapterZona =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapterZona.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZona.adapter = adapterZona

        binding.spinnerZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                zonaSeleccionada = zonas[position]
                actualizarCamaras(zonaSeleccionada!!)
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerCamara.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val camaras = zonaCamaraMap[zonaSeleccionada] ?: emptyList()
                if (position < camaras.size) {
                    camaraSeleccionada = camaras[position]
                    actualizarTablaFiltrada()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val adapterTurno =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaTurnos)
        adapterTurno.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.turnoSpinner.adapter = adapterTurno

        binding.turnoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                turnoSeleccionado = listaTurnos[position]
                actualizarTablaFiltrada()

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val adapterMovimiento =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaMovimientos)
        adapterMovimiento.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.tipoEntrada.adapter = adapterMovimiento

        binding.tipoEntrada.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                movimientoSeleccionado = listaMovimientos[position]
                actualizarTablaFiltrada()

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (zonas.isNotEmpty()) {
            actualizarCamaras(zonas[0])
        }
    }

    private fun actualizarCamaras(zona: String) {
        val camaras = zonaCamaraMap[zona] ?: emptyList()
        val adapterCamara =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, camaras)
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCamara.adapter = adapterCamara
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun guardarEtiqueta(etiqueta: String) {
        val zona = zonaSeleccionada
        val camara = camaraSeleccionada
        val turno = turnoSeleccionado
        val movimiento = movimientoSeleccionado

        if (zona == null || camara == null || turno == null || movimiento == null) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Selecciona zona, cámara, turno y tipo de movimiento antes de escanear",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val ultimoMovimiento = db.obtenerUltimoMovimiento(etiqueta, camara)
        val camaraActual = db.obtenerCamaraActual(etiqueta)

        when (movimiento) {
            "Entrada" -> {
                if (camaraActual != null) {
                    val mensaje = if (camaraActual == camara) {
                        "Esta etiqueta ya está dentro de esta cámara — falta registrar su salida"
                    } else {
                        "Esta etiqueta está dentro de $camaraActual — debe salir antes de entrar a $camara"
                    }
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    }
                    return
                }
            }

            "Salida" -> {
                if (camaraActual != camara) {
                    val mensaje = if (camaraActual == null) {
                        "Esta etiqueta no tiene una entrada registrada"
                    } else {
                        "Esta etiqueta está dentro de $camaraActual, no de $camara"
                    }
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    }
                    return
                }

            }
        }

        val util = Separador()
        val etiquetaParseada = util.etiquetaseparation(etiqueta)

        if (etiquetaParseada == null) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "No hay etiqueta por procesar (long=${etiqueta.length})",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val tempFile = userNameCache.userNameRoute
        val userName = if (tempFile != null) File(tempFile).readText() else "desconocido"

        val fecha = construirFecha(etiquetaParseada)
        val hora = construirHora(etiquetaParseada)

        val insertado = db.insertarEtiqueta(
            e = etiquetaParseada,
            fecha = fecha,
            hora = hora,
            zona = zona,
            camara = camara,
            turno = turno,
            escaneadoPor = userName,
            etiquetaEscaneada = etiqueta,
            tipoMovimiento = movimiento
        )

        if (!insertado) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Etiqueta Anomala, ver a Administrador",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            return
        }

        Log.i("Etiqueta", "${etiqueta.length} ")

        requireActivity().runOnUiThread {
            actualizarTablaFiltrada()
        }
    }

    private fun cancelarEscaneo() {
        db.eliminarUltimaEtiqueta()
        actualizarTablaFiltrada()
        Toast.makeText(requireContext(), "Última etiqueta eliminada", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun construirFecha(e: Etiqueta): String {
        val anio = LocalDateTime.now().year
        val firstDigitsofYear = anio.toString().substring(0, 3)
        return "${e.primDigDia}${e.segDigDia}/${e.primDigMes}${e.segDigMes}/${firstDigitsofYear}${e.ultDigAnio}"
    }

    private fun construirHora(e: Etiqueta): String {
        return "${e.primDigHora}${e.segDigHora}:${e.primDigMin}${e.segDigMin}:${e.primDigSeg}${e.segDigSeg}"
    }

    private fun actualizarTablaFiltrada() {
        val resultados = db.obtenerReporteFiltrado(
            zona = zonaSeleccionada,
            camara = camaraSeleccionada,
            turno = turnoSeleccionado,
            movimiento = movimientoSeleccionado
        )

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        resultados.forEach { agregarFilaDesdeDB(it) }
    }

    private fun agregarFilaDesdeDB(e: EtiquetaGuardada) {
        val fila = TableRow(requireContext())
        val valores = listOf(
            e.claveProducto,
            e.descripcionArticulo,
            e.piezas,
            e.kilos,
            e.lote,
            e.fecha,
            e.hora
        )

        valores.forEach { texto ->
            val textView = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setPadding(20,20,20,20)
                text = texto
            }
            fila.addView(textView)
        }

        binding.tableLayout.addView(fila)
    }

    private fun generarReportePantalla() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, reportesActivity())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG = "EscanearEtiquetas"
        private const val ACTION_SUNMI = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val EXTRA_SUNMI = "data"
        private const val ACTION_ZEBRA = "com.example.etiquetas.SCAN"
        private const val EXTRA_ZEBRA = "com.symbol.datawedge.data_string"
    }
}