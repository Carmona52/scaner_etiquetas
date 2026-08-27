package com.example.etiquetas

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.methods.EtiquetaGuardada
import com.example.etiquetas.databinding.FragmentSeeAllEtiquetasBinding
import com.example.etiquetas.utils.DateBuilders

class SeeAllEtiquetasFragment : Fragment() {
    private var _binding: FragmentSeeAllEtiquetasBinding? = null
    private val binding get() = _binding!!
    private var camaraSeleccionadaId: Int? = 0


    private lateinit var db: DataBase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeeAllEtiquetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase.getInstance(requireContext())
        configurarSelectores()
        actualizarTablaFiltrada()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configurarSelectores() {
        val listaCamaras = db.camaras.getAllCamaras()
        val adapterCamara = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaCamaras.map { it.nombreCamara })
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCamara.adapter = adapterCamara
        binding.spinnerCamara.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                camaraSeleccionadaId = listaCamaras[position].id
                actualizarTablaFiltrada()
                setKilos()
                getTotalCestas()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun actualizarTablaFiltrada() {
        val resultados = db.reportes.obtenerReporteFiltrado(
            idCamara = camaraSeleccionadaId
        )

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        resultados.forEach { agregarFilaDesdeDB(it) }
    }

    private fun setKilos(){
        val peso = db.camaras.getCameraWeight(camaraSeleccionadaId)
        binding.totalWeight.setText("Kilos Totales en esta Camara ${peso}kg")
    }

    private fun getTotalCestas(){
        val cestas = db.camaras.getTotalTransacciones(camaraSeleccionadaId)
        binding.totalCestas.setText("Total de Transacciones: $cestas")
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
            DateBuilders().formDate(e.fechaEscaneo),
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}