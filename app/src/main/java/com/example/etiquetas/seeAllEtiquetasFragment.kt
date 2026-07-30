package com.example.etiquetas

import android.R
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.EtiquetaGuardada
import com.example.etiquetas.databinding.FragmentSeeAllEtiquetasBinding
import java.text.DateFormat
import java.time.format.DateTimeFormatter

class seeAllEtiquetasFragment : Fragment() {

    private var _binding: FragmentSeeAllEtiquetasBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DataBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSeeAllEtiquetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())
        ActualizarTabla()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ActualizarTabla() {
        val etiquetas = db.obtenerEtiquetas()

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        etiquetas.forEach { agregarFila(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun agregarFila(etiqueta: EtiquetaGuardada) {
        val fila = TableRow(requireContext())
        val valores = listOf(
            etiqueta.etiquetaEscaneada,
            etiqueta.claveProducto,
            etiqueta.descripcionArticulo,
            etiqueta.kilos,
            etiqueta.piezas,
            etiqueta.lote,
            etiqueta.fecha,
            etiqueta.hora,
            etiqueta.zona,
            etiqueta.camara,
            etiqueta.turno,
            etiqueta.tipoMovimiento,
            etiqueta.escaneadoPor,
            formatDate(etiqueta.fechaEscaneo),
            etiqueta.notas
        )

        valores.forEach { texto ->
            val textView = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setPadding(20, 20, 20, 20)
                text = texto?.toString() ?: ""
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
