package com.example.etiquetas

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.methods.CamaraGuardada
import com.example.etiquetas.database.methods.getConteoCamara
import com.example.etiquetas.databinding.FragmentCameraDetailsBinding
import com.example.etiquetas.factory.dialog.tablerow.TableCellFactory

class CameraDetails : Fragment() {

    private var _binding: FragmentCameraDetailsBinding? = null
    private val binding get() = _binding!!

    private var camara: CamaraGuardada? = null
    private lateinit var db: DataBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        camara = arguments?.let { args ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable(ARG_CAMARA_OBJ, CamaraGuardada::class.java)
            } else {
                @Suppress("DEPRECATION") args.getParcelable(ARG_CAMARA_OBJ)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DataBase.getInstance(requireContext())
        camara?.let { cam ->
            binding.cameraName.text = cam.nombreCamara
        }

        loadData()
        actualizarTablaFiltrada()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun loadData() {
        val peso = db.camaras.getCameraWeight(camara?.id)
        val totalCestas = db.camaras.getTotalCestas(camara?.id)

        binding.cameraWeight.text = "Peso en la camara: $peso kg"
        binding.totalCestas.text = "Total de Cestas: $totalCestas"
    }

    private fun agregarFilaDesdeDB(data: getConteoCamara) {
        val context = requireContext()
        val fila = TableRow(requireContext())

        val valores = listOf(
            data.descripcion, data.cantidadCestas, data.kilosPorProducto
        )

        valores.forEach { texto ->
            val textView = TableCellFactory.createCelda(context, texto.toString())
            fila.addView(textView)
        }

        binding.tableLayout.addView(fila)
    }

    private fun actualizarTablaFiltrada() {
        val conteoIndividual = db.camaras.getConteoCestas(camara?.id)
        Log.d("ConteoIndividual", "${conteoIndividual}")

        val cantidadFilas = binding.tableLayout.childCount
        for (i in cantidadFilas - 1 downTo 1) {
            binding.tableLayout.removeViewAt(i)
        }

        conteoIndividual.forEach { agregarFilaDesdeDB(it) }
    }

    companion object {
        private const val ARG_CAMARA_OBJ = "camara_obj"

        fun newInstance(camara: CamaraGuardada): CameraDetails = CameraDetails().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CAMARA_OBJ, camara)
            }
        }
    }
}
