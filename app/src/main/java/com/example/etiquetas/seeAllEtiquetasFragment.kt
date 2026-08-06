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
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.EtiquetaGuardada
import com.example.etiquetas.databinding.FragmentSeeAllEtiquetasBinding
import java.time.format.DateTimeFormatter

class SeeAllEtiquetasFragment : Fragment() {

    private var _binding: FragmentSeeAllEtiquetasBinding? = null
    private val binding get() = _binding!!

    private var zonaSeleccionada: String? = null
    private var camaraSeleccionada: String? = null

    private val zonaCamaraMap = mapOf(
        "Camara Fresco" to listOf(
            "Camara de Fresco 1",
            "Camara de Fresco 2",
            "Camara de Fresco 3",
            "Camara de Fresco 4",
            "Camara de Fresco 7"
        ),
        "Camara Congelado" to listOf(
            "Camara de Congelacion 1", "Camara de Congelacion 2", "Camara de Congelacion 3"
        ),
        "Camara Conservacion" to listOf(
            "Camara de Conservacion 1",
            "Camara de Conservacion 2",
            "Camara de Conservacion 3",
            "Camara de Conservacion 4"
        ),
    )
    private lateinit var db: DataBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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
        actualizarTablaFiltrada()
        configurarSelectores()

    }

    @RequiresApi(Build.VERSION_CODES.O)

    private fun configurarSelectores() {
        val zonas = zonaCamaraMap.keys.toList()

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
    private fun actualizarTablaFiltrada() {
        val resultados = db.obtenerReporteFiltrado(
            camara = camaraSeleccionada
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
