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
import android.widget.TableLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import com.example.etiquetas.separador
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private val etiquetasEscaneadas = LinkedHashSet<Pair<String, String>>()

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

        cameraExecutor = Executors.newSingleThreadExecutor()

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
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fecha = formato.format(Date())
        val util = separador()

        val mensaje = util.etiquetaseparation(etiqueta)
        println(mensaje)



        etiquetasEscaneadas.add(etiqueta to fecha)


        requireActivity().runOnUiThread {
            binding.escaneos.text = etiquetasEscaneadas.joinToString("\n") { (valor, f) -> "$valor - $f" }


        }
    }

    private fun cancelarEscaneo() {
        etiquetasEscaneadas.clear()
        ultimaEtiquetaDetectada = null
        binding.escaneos.text = ""
        Toast.makeText(requireContext(), "Escaneo cancelado", Toast.LENGTH_SHORT).show()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarCSV() {
        if (etiquetasEscaneadas.isEmpty()) {
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
                    outputStream.write("Etiqueta,Fecha escaneo\n".toByteArray())
                    etiquetasEscaneadas.forEach { (valor, fecha) ->
                        outputStream.write("$valor,$fecha\n".toByteArray())
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
