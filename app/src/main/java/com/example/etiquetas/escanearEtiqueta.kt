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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private val etiquetasEscaneadas = LinkedHashSet<Pair<String, String>>()
    private val etiquetasNormalizadas = mutableListOf<Etiqueta>()
    @Volatile
    private var ultimaEtiquetaDetectada: String? = null

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

        binding.btnCancelar.setOnClickListener {
            cancelarEscaneo()
        }

        binding.btnGuardarCSV.setOnClickListener {
            guardarCSV()
        }

    }

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Broadcast recibido: ${intent.extras?.keySet()?.joinToString()}")
            val codigo = intent.getStringExtra("data")
            if (!codigo.isNullOrEmpty()) {
                guardarEtiqueta(codigo)
            }
        }
    }

    @SuppressLint(
        "UnspecifiedRegisterReceiverFlag",
        "SuspiciousIndentation",
        "IntentACTION_DATA_CODE_RECEIVED"
    )
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
        requireContext().registerReceiver(scanReceiver, filter)
        Log.d(TAG, "Receiver registrado, esperando broadcasts...")
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(scanReceiver)
    }


    private fun guardarEtiqueta(etiqueta: String) {
        val util = separador()
        val etiqueta = util.etiquetaseparation(etiqueta)

        if(etiqueta == null){
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(),"No hay etiqueta por procesar", Toast.LENGTH_SHORT).show()
            }
            return
        }

        etiquetasNormalizadas.add(etiqueta)

        requireActivity().runOnUiThread {
            agregarFila(etiqueta)
        }
    }

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

    private fun construirFecha(e: Etiqueta): String {
        return "${e.ultDigAnio}/${e.primDigMes}${e.segDigMes}/${e.primDigDia}${e.segDigDia}"
    }

    private fun construirHora(e: Etiqueta): String {
        return "${e.primDigHora}${e.segDigHora}:${e.primDigMin}${e.segDigMin}:${e.primDigSeg}${e.segDigSeg}"
    }

    private fun cancelarEscaneo() {
        etiquetasNormalizadas.clear()
        while (binding.tableLayout.childCount > 1) {
            binding.tableLayout.removeViewAt(1)
        }
        Toast.makeText(requireContext(), "Escaneo cancelado", Toast.LENGTH_SHORT).show()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarCSV() {
        if (etiquetasNormalizadas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay etiquetas para guardar", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val nombreArchivo = "etiquetas_${System.currentTimeMillis()}.csv"

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
                    outputStream.write("Clave Producto,Piezas,Kilos,Lote,Fecha,Hora\n".toByteArray())
                    etiquetasNormalizadas.forEach { e ->
                        outputStream.write(
                            "${e.claveProducto},${e.piezas},${e.kilos},${e.lote},${construirFecha(e)},${construirHora(e)}\n".toByteArray()
                        )
                    }
                }
                Toast.makeText(
                    requireContext(),
                    "CSV guardado en Descargas/Etiquetas",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar CSV", e)
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Escanear Etiquetas"
    }
}
