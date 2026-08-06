package com.example.etiquetas

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.productosGuardados
import com.example.etiquetas.databinding.FragmentSelectProductosBinding

class SelectProductos : Fragment() {

    private var _binding: FragmentSelectProductosBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DataBase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase(requireContext())
        actualizarTabla()
        inicializarEventos()
    }

    private fun inicializarEventos() {
        binding.addProduct.setOnClickListener { verificarIdentidad(requireContext()) }
    }

    private fun actualizarTabla() {
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
                text = texto
            }
            fila.addView(textView)
        }
        binding.tableLayout.addView(fila)
    }

    private fun verificarIdentidad(context: Context) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val input = EditText(context).apply {
            hint = "Ingrese la contraseña"
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(input)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Por favor ingrese la contraseña")
            .setView(layout)
            .setPositiveButton("Aceptar", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val password = input.text.toString().trim()
                if (password == "securePass") {
                    dialog.dismiss()
                    agregarProducto()
                } else {
                    Toast.makeText(requireContext(), "Contraseña incorrecta", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        dialog.show()
    }

    private fun agregarProducto() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val codeInput = EditText(requireContext()).apply {
            hint = "Ingrese el código del producto"
        }
        val descriptionInput = EditText(requireContext()).apply {
            hint = "Ingrese la descripción del producto"
        }

        layout.addView(codeInput)
        layout.addView(descriptionInput)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Insertar o actualizar elemento")
            .setView(layout)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            saveButton.setOnClickListener {
                val codigo = codeInput.text.toString().trim()
                val descripcion = descriptionInput.text.toString().trim()

                if (codigo.isNotEmpty() && descripcion.isNotEmpty()) {
                    db.upsertArticulo(codigo, descripcion)
                    Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                    actualizarTabla()
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Por favor completa ambos campos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}