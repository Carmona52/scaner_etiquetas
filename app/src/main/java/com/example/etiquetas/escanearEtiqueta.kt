package com.example.etiquetas

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.etiquetas.database.CamaraGuardada
import com.example.etiquetas.database.ZonaGuardada
import com.example.etiquetas.utils.Etiqueta
import com.example.etiquetas.utils.Separador
import com.example.etiquetas.utils.userNameCache
import com.example.etiquetas.utils.MakeSounds

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private var zonaSeleccionada: ZonaGuardada? = null
    private var camaraSeleccionada: CamaraGuardada? = null
    private var turnoSeleccionado: String? = null
    private var movimientoSeleccionado: String? = null

    private var toneGenerator: ToneGenerator? = null
    private var soundHelper: MakeSounds? = null
    private lateinit var db: DataBase

    private val turnos = arrayOf("Turno 1", "Turno 2", "Turno 3")

    private val movimientos = arrayOf("Entrada", "Salida", "Inventario")

    private val scanReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val codigo = when (intent.action) {
                ACTION_SUNMI -> intent.getStringExtra(EXTRA_SUNMI)
                ACTION_ZEBRA -> intent.getStringExtra(EXTRA_ZEBRA)
                ACTION_HONEYWELL -> intent.getStringExtra(EXTRA_HONEYWELL)
                ACTION_HONEYWELL_AIDC -> {
                    intent.getStringExtra(EXTRA_HONEYWELL_AIDC)
                        ?: intent.getStringExtra("barcode_data")
                }

                else -> null
            }

            if (!codigo.isNullOrEmpty()) {
                guardarEtiqueta(codigo)
            } else {
                Toast.makeText(
                    context,
                    "El escáner envió la señal, pero el código está vacío.",
                    Toast.LENGTH_LONG
                ).show()
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
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        soundHelper = MakeSounds(toneGenerator)
        configurarSelectores()
        configurarEventos()


    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ACTION_SUNMI)
            addAction(ACTION_ZEBRA)
            addAction(ACTION_HONEYWELL)
            addAction(ACTION_HONEYWELL_AIDC)

            //Comentar si da problemas
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
        toneGenerator?.release()
        toneGenerator = null
        soundHelper = null
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun configurarEventos() {
        binding.btnCancelar.setOnClickListener { cancelarEscaneo() }
        binding.generarReporte.setOnClickListener { generarReportePantalla() }
//        binding.seeAnomalias.setOnClickListener { verAnomalias() }
    }


    private fun configurarSelectores() {
        val zonas = db.obtenerZonas()
        val listaTurnos = turnos.toList()
        val listaMovimientos = movimientos.toList()

        val adapterZona = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, zonas.map { it.nombreZona })
        adapterZona.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZona.adapter = adapterZona

        binding.spinnerZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                zonaSeleccionada = zonas[position]
                actualizarCamaras(zonaSeleccionada!!.id)
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerCamara.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val camaras =
                    zonaSeleccionada?.let { db.obtenerCamarasPorZona(it.id) } ?: emptyList()
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
            zonaSeleccionada = zonas[0]
            actualizarCamaras(zonas[0].id)
        }
    }

    private fun actualizarCamaras(idZona: Int) {
        val camaras = db.obtenerCamarasPorZona(idZona)
        val adapterCamara = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, camaras.map { it.nombreCamara })
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCamara.adapter = adapterCamara
        camaraSeleccionada = camaras.firstOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun guardarEtiqueta(etiqueta: String) {
        val zona = zonaSeleccionada
        val camara = camaraSeleccionada
        val turno = turnoSeleccionado
        val movimiento = movimientoSeleccionado

        if (zona == null || camara == null || turno == null || movimiento == null) {
            soundHelper?.makeBadSound()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Selecciona zona, cámara, turno y tipo de movimiento antes de escanear",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val idCamaraActual = db.obtenerCamaraActualId(etiqueta)

        when (movimiento) {
            "Entrada" -> {
                if (idCamaraActual != null) {
                    soundHelper?.makeBadSound()
                    val nombreCamaraActual = db.obtenerNombreCamara(idCamaraActual) ?: "otra cámara"
                    val mensaje = if (idCamaraActual == camara.id) {
                        "Esta etiqueta ya está dentro de esta cámara — falta registrar su salida"
                    } else {
                        "Esta etiqueta está dentro de $nombreCamaraActual — debe salir antes de entrar a ${camara.nombreCamara}"
                    }
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    }
                    return
                }
            }

            "Salida" -> {
                if (idCamaraActual != camara.id) {
                    soundHelper?.makeBadSound()
                    val mensaje = if (idCamaraActual == null) {
                        "Esta etiqueta no tiene una entrada registrada"
                    } else {
                        val nombreCamaraActual =
                            db.obtenerNombreCamara(idCamaraActual) ?: "otra cámara"
                        "Esta etiqueta está dentro de $nombreCamaraActual, no de ${camara.nombreCamara}"
                    }
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    }
                    return
                } else {
                    soundHelper?.makeGoodSound()
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Ha completado el ciclo correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        val util = Separador()
        val etiquetaParseada = util.etiquetaseparation(etiqueta)

        if (etiquetaParseada == null) {
            soundHelper?.makeBadSound()
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
            idZona = zona.id,
            idCamara = camara.id,
            turno = turno,
            escaneadoPor = userName,
            etiquetaEscaneada = etiqueta,
            tipoMovimiento = movimiento
        )

        if (!insertado) {
            soundHelper?.makeBadSound()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(), "Etiqueta Anomala, ver a Administrador", Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        soundHelper?.makeGoodSound()

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
            idZona = zonaSeleccionada?.id,
            idCamara = camaraSeleccionada?.id,
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
            e.claveProducto, e.descripcionArticulo, e.piezas, e.kilos, e.lote, e.fecha, e.hora
        )

        valores.forEach { texto ->
            val textView = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT
                )
                setPadding(20, 20, 20, 20)
                text = texto
            }
            fila.addView(textView)
        }

        binding.tableLayout.addView(fila)
    }

    private fun generarReportePantalla() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ReportesActivity()).addToBackStack(null).commit()
    }

    private fun verAnomalias() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ReportesActivity()).addToBackStack(null).commit()
    }

    companion object {
        private const val ACTION_SUNMI = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val EXTRA_SUNMI = "data"
        private const val ACTION_ZEBRA = "com.example.etiquetas.SCAN"
        private const val EXTRA_ZEBRA = "com.symbol.datawedge.data_string"

        const val ACTION_HONEYWELL = "com.honeywell.scanintent.action.SCAN"
        const val EXTRA_HONEYWELL = "com.honeywell.scanintent.extra.DATA"

        const val ACTION_HONEYWELL_AIDC = "com.honeywell.aidc.action.BARCODE_DATA"
        const val EXTRA_HONEYWELL_AIDC = "data"
    }
}