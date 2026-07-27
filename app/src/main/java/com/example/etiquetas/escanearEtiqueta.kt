package com.example.etiquetas

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import java.io.File
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

private val zonaCamaraMap = mapOf(
    "Zona 1" to listOf("Camara 1", "Camara 3"),
    "Zona 2" to listOf("Camara 2", "Camara 4"),
    "Zona 3" to listOf("Camara 5"),
    "Zona 4" to listOf("Camara 6", "Camara 7"),
    "Zona 5" to listOf("Camara 8", "Camara 9")
)

private var zonaSeleccionada: String? = null
private var camaraSeleccionada: String? = null

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private val etiquetasNormalizadas = mutableListOf<Etiqueta>()

    @Volatile
    private var ultimaEtiquetaDetectada = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EscanearEtiquetaFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarSelectores()

        binding.btnCancelar.setOnClickListener { cancelarEscaneo() }

        binding.btnGuardarCSV.setOnClickListener { guardarCSV() }

    }

    private val scanReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {

            val codigo = when (intent.action) {
                ACTION_SUNMI -> intent.getStringExtra(EXTRA_SUNMI)
                ACTION_ZEBRA -> intent.getStringExtra(EXTRA_ZEBRA)
                else -> null
            }

            if (!codigo.isNullOrEmpty()) {
                guardarEtiqueta(codigo)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ACTION_SUNMI)
            addAction(ACTION_ZEBRA)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        ContextCompat.registerReceiver(
            requireContext(),
            scanReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(scanReceiver)
    }

    private fun configurarSelectores() {
        val zonas = zonaCamaraMap.keys.toList()

        val zoneName = createTempFile(prefix = "zoneName", suffix = ".tmp")
        val cameraName = createTempFile(prefix = "cameraName", suffix = ".tmp")


        val adapterZona =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
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
                zoneName.writeText(zonaSeleccionada.toString())
                zone.zoneName = zoneName.absolutePath
                actualizarCamaras(zonaSeleccionada!!)
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
                val camaras = zonaCamaraMap[zonaSeleccionada] ?: emptyList()
                if (position < camaras.size) {
                    camaraSeleccionada = camaras[position]
                    cameraName.writeText(camaraSeleccionada.toString())
                    camera.cameraName = cameraName.absolutePath
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
    private fun guardarEtiqueta(etiqueta: String) {
        if (ultimaEtiquetaDetectada.contains(etiqueta)) {
            Toast.makeText(requireContext(), "Ya has escaneado esa etiqueta", Toast.LENGTH_SHORT)
                .show()
        } else {
            val util = separador()
            val etiquetaParseada = util.etiquetaseparation(etiqueta)

            if (etiquetaParseada == null) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "No hay etiqueta por procesar (long=${etiqueta.length})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            val etiquetaConTimestamp = etiquetaParseada.copy(fechaEscaneo = LocalDateTime.now())

            etiquetasNormalizadas.add(etiquetaConTimestamp)
            ultimaEtiquetaDetectada.add(etiqueta)
            requireActivity().runOnUiThread {
                agregarFila(etiquetaConTimestamp)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun agregarFila(e: Etiqueta) {
        val fila = TableRow(requireContext())

        val valores = listOf(
            e.claveProducto,
            e.piezas,
            e.kilos,
            e.lote,
            construirFecha(e),
            construirHora(e)
        )

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun construirFecha(e: Etiqueta): String {
        val fecha = LocalDateTime.now();
        val anio = fecha.year
        val firstDigitsofYear = anio.toString().substring(0, 3)

        return "${e.primDigDia}${e.segDigDia}/${e.primDigMes}${e.segDigMes}/${firstDigitsofYear}${e.ultDigAnio}"
    }

    private fun construirHora(e: Etiqueta): String {
        return "${e.primDigHora}${e.segDigHora}:${e.primDigMin}${e.segDigMin}:${e.primDigSeg}${e.segDigSeg}"
    }

    private fun cancelarEscaneo() {
        if (etiquetasNormalizadas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay etiquetas para borrar", Toast.LENGTH_SHORT)
                .show()
            return
        }

        etiquetasNormalizadas.removeAt(etiquetasNormalizadas.lastIndex)
        if (ultimaEtiquetaDetectada.isNotEmpty()) {
            ultimaEtiquetaDetectada.removeAt(ultimaEtiquetaDetectada.lastIndex)
        }

        val cantidadFilas = binding.tableLayout.childCount
        if (cantidadFilas > 1) {
            binding.tableLayout.removeViewAt(cantidadFilas - 1)
        }

        Toast.makeText(requireContext(), "Última etiqueta eliminada", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarCSV() {
        if (etiquetasNormalizadas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay etiquetas para guardar", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val tempFile = userNameCache.userNameRoute
        val zoneName = zone.zoneName
        val cameraName = camera.cameraName


        if (tempFile === null) {
            Log.e("Username", "No hay usuario")
        }

        if(zoneName === null){
            Log.e("Zone", "No hay zona Seleccionada")
        }

        if(cameraName === null) Log.e("Camera", "No hay Camara Seleccionada")



        val userName = File(tempFile).readText()
        val camara = File(cameraName).readText()
        val zona = File(zoneName).readText()
        val date = LocalDateTime.now()
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")


        val nombreArchivo = "etiquetas_${date.format(formato)}.csv"


        try {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Etiquetas")
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write("Clave Producto,Piezas,Kilos,Lote,Fecha,Hora,Notas, Escaneado por, Fecha Escaneo, Zona, Camara\n".toByteArray())
                    etiquetasNormalizadas.forEach { e ->
                        outputStream.write(
                            "${e.claveProducto},${e.piezas},${e.kilos},${e.lote},${construirFecha(e)},${
                                construirHora(e)
                            }, ${e.notas}, ${userName}, ${LocalDateTime.now()}, ${zona}, ${camara}\n".toByteArray()
                        )
                    }
                }
                Toast.makeText(
                    requireContext(),
                    "CSV guardado en Etiquetas/${nombreArchivo}",
                    Toast.LENGTH_LONG
                ).show()


                etiquetasNormalizadas.clear()
                ultimaEtiquetaDetectada.clear()
                val cantidadTotalFilas = binding.tableLayout.childCount
                for (i in cantidadTotalFilas - 1 downTo 1) {
                    binding.tableLayout.removeViewAt(i)
                }


            } else {
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "Escanear Etiquetas"
        private const val ACTION_SUNMI = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val EXTRA_SUNMI = "data"
        private const val ACTION_ZEBRA = "com.example.etiquetas.SCAN"
        private const val EXTRA_ZEBRA = "com.symbol.datawedge.data_string"
    }
}
