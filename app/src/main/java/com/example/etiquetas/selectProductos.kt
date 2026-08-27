package com.example.etiquetas

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.methods.productosGuardados
import com.example.etiquetas.databinding.FragmentSelectProductosBinding
import com.example.etiquetas.factory.dialog.tablerow.TableCellFactory

class SelectProductos : Fragment() {
    private var _binding: FragmentSelectProductosBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DataBase
    private var offsetActual = 0
    private val limitePorPagina = 40
    private var cargandoDatos = false
    private var hayMasDatos = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase.getInstance(requireContext())
        configurarScroll()
        tablaInicial()
        inicializarEventos()
    }

    private fun inicializarEventos() {
        binding.addProduct.setOnClickListener { verificarIdentidad(requireContext()) }
    }

    private fun tablaInicial() {
        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        offsetActual = 0
        hayMasDatos = true
        cargarProductos()
    }

    private fun agregarFila(producto: productosGuardados) {
        val context = requireContext()
        val fila = TableRow(context)
        val valores = listOf(producto.claveProducto, producto.descripcion)

        valores.forEachIndexed { index, texto ->

            val textView = TableCellFactory.createCelda(
                context = context, texto = texto)
            fila.addView(textView)
        }
        binding.tableLayout.addView(fila)
    }

    private fun configurarScroll() {
        binding.scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val seLlegoAlFinal = scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight)
                if (seLlegoAlFinal && !cargandoDatos && hayMasDatos) {
                    binding.arrowDrop.visibility = View.VISIBLE
                    cargarProductos()
                } else {
                    binding.arrowDrop.visibility = View.GONE
                }
            }
        )
    }

    private fun cargarProductos() {
        if (!hayMasDatos || cargandoDatos) return
        cargandoDatos = true

        val nuevosProductos = db.productos.getProductosPaginados(limitePorPagina, offsetActual)

        if (nuevosProductos.isNotEmpty()) {
            nuevosProductos.forEach { agregarFila(it) }
            offsetActual += limitePorPagina
            if (nuevosProductos.size < limitePorPagina) {
                hayMasDatos = false
            }
        } else {
            hayMasDatos = false
        }

        cargandoDatos = false
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
            inputType = android.text.InputType.TYPE_CLASS_PHONE
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
                    db.productos.upsertProducto(codigo, descripcion)
                    Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                    tablaInicial()
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