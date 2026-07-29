package com.example.etiquetas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.productosGuardados
import com.example.etiquetas.databinding.FragmentSelectProductosBinding

class SelectProductos : Fragment() {

    private var _binding: FragmentSelectProductosBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DataBase


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())
        ActualizarTabla()
    }

    private fun ActualizarTabla() {
        val productos = db.obtenerProductos()

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        productos.forEach { agregarFila(it) }
    }

    private fun agregarFila(producto: productosGuardados) {
        val fila = TableRow(requireContext())
        val valores = listOf(producto.claveProducto, producto.descripcion)

        valores.forEach { texto ->
            val textView = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(6, 6, 6, 6)
                text = texto.toString()
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
