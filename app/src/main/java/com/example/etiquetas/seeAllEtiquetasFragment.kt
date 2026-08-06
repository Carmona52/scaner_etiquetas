package com.example.etiquetas

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.etiquetas.database.CamaraGuardada
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.EtiquetaGuardada
import com.example.etiquetas.database.ZonaGuardada
import com.example.etiquetas.databinding.FragmentSeeAllEtiquetasBinding
import java.time.format.DateTimeFormatter

class SeeAllEtiquetasFragment : Fragment() {

    private var _binding: FragmentSeeAllEtiquetasBinding? = null
    private val binding get() = _binding!!

    private var zonaSeleccionada: ZonaGuardada? = null
    private var camaraSeleccionada: CamaraGuardada? = null

    private lateinit var db: DataBase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSeeAllEtiquetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())
        configurarSelectores()
        actualizarTablaFiltrada()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configurarSelectores() {
        val zonas = db.obtenerZonas()

        val adapterZona = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, zonas.map { it.nombreZona }
        )
        adapterZona.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZona.adapter = adapterZona

        binding.spinnerZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                zonaSeleccionada = zonas[position]
                actualizarCamaras(zonaSeleccionada!!.id)
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerCamara.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
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

        if (zonas.isNotEmpty()) {
            zonaSeleccionada = zonas[0]
            actualizarCamaras(zonas[0].id)
        }
    }

    private fun actualizarCamaras(idZona: Int) {
        val camaras = db.obtenerCamarasPorZona(idZona)
        val adapterCamara = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, camaras.map { it.nombreCamara }
        )
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCamara.adapter = adapterCamara
        camaraSeleccionada = camaras.firstOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun actualizarTablaFiltrada() {
        val resultados = db.obtenerReporteFiltrado(
            idZona = zonaSeleccionada?.id,
            idCamara = camaraSeleccionada?.id
        )

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        resultados.forEach { agregarFilaDesdeDB(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun agregarFilaDesdeDB(e: EtiquetaGuardada) {
        val fila = TableRow(requireContext())
        val valores = listOf(
            e.etiquetaEscaneada,
            e.claveProducto,
            e.descripcionArticulo,
            e.kilos,
            e.piezas,
            e.numEmpaque,
            e.lote,
            e.fecha,
            e.hora,
            e.zona,
            e.camara,
            e.turno,
            e.tipoMovimiento,
            e.escaneadoPor,
            formatDate(e.fechaEscaneo),
            e.notas
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val parsedDate = java.time.LocalDateTime.parse(dateString)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            parsedDate.format(formatter)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}