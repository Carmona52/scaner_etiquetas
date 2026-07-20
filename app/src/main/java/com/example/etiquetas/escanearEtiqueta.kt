package com.example.etiquetas

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    private val etiquetasEscaneadas =LinkedHashSet<Pair<String, String>>()
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

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCancelar.setOnClickListener {
            cancelarEscaneo()
        }

        binding.btnGuardarCSV.setOnClickListener {
            guardarCSV()
        }

        binding.scanBtn.setOnClickListener {
            scanBtn()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.cameraPreview.surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.setAnalyzer(cameraExecutor, barLector { etiqueta ->
                        requireActivity().runOnUiThread {
                            ultimaEtiquetaDetectada = etiqueta
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Algo falló, reintente", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun scanBtn() {
        val etiqueta = ultimaEtiquetaDetectada

        if (etiqueta.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No hay etiqueta que leer", Toast.LENGTH_SHORT).show()
            return
        }

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fechaEscaneo = formato.format(Date())
        etiquetasEscaneadas.add(etiqueta to fechaEscaneo)
        binding.escaneos.text = etiquetasEscaneadas.joinToString("\n") { (valor, fecha) -> "$valor   ${fechaEscaneo}"  }
        Toast.makeText(requireContext(), "Etiqueta guardada: $etiqueta", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "No hay etiquetas para guardar", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "CSV guardado en Descargas/Etiquetas", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar CSV", e)
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permisos no otorgados por el usuario.",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Escanear Etiquetas"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
