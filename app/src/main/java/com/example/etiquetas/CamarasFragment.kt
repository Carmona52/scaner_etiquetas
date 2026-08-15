package com.example.etiquetas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.methods.CamaraGuardada
import com.example.etiquetas.databinding.FragmentCamarasBinding
import com.example.etiquetas.factory.dialog.tablerow.TableCellFactory

class CamarasFragment : Fragment() {

    private var _binding: FragmentCamarasBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DataBase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCamarasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase.getInstance(requireContext())
        actualizarTabla()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun actualizarTabla() {
        val camaras = db.camaras.getAllCamaras()
        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        camaras.forEach { agregarFila(it) }
    }

    private fun agregarFila(camara: CamaraGuardada) {
        val context = requireContext()
        val fila = TableRow(context)
        val valores =
            listOf(camara.numCamara, camara.idZona, camara.nombreCamara, camara.descripcion ?: "")


        valores.forEachIndexed { index, texto ->
            val textView = TableCellFactory.createCelda(
                context = context,
                texto = texto.toString(),
            )
            fila.addView(textView)
        }
        binding.tableLayout.addView(fila)
    }
}