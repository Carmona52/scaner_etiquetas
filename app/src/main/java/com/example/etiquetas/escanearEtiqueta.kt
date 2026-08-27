package com.example.etiquetas

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.etiquetas.adapters.etiquetas.EtiquetasAdapter
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.MovimientoGuardado
import com.example.etiquetas.database.methods.CamaraGuardada
import com.example.etiquetas.database.methods.EtiquetaGuardada
import com.example.etiquetas.database.methods.updateEtiqueta
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import com.example.etiquetas.factory.dialog.dialoFactory
import com.example.etiquetas.utils.DateBuilders
import com.example.etiquetas.utils.MakeSounds
import com.example.etiquetas.utils.ScanerAccess
import com.example.etiquetas.utils.Separador
import com.example.etiquetas.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DataBase
    private lateinit var etiquetasAdapter: EtiquetasAdapter
    private var toneGenerator: ToneGenerator? = null
    private var soundHelper: MakeSounds? = null
    private var scannerManager: ScanerAccess? = null
    private var turnoSeleccionado: String? = null
    private var movimientoSeleccionado: String? = null
    private var camaraSeleccionada: CamaraGuardada? = null
    private val scanMutex = Mutex()
    private var cargaTablaJob: Job? = null

    private val turnos = arrayOf("Turno 1", "Turno 2", "Turno 3")
    private var listaMovimientos: List<MovimientoGuardado> = emptyList()

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

        db = DataBase.getInstance(requireContext())
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        soundHelper = MakeSounds(toneGenerator)

        configurarRecyclerView()

        scannerManager = ScanerAccess(requireContext()) { codigoEscaneado ->
            if (_binding != null && isAdded) {
                guardarEtiqueta(codigoEscaneado)
            }
        }

        configurarSelectores()
        configurarEventos()
    }

    override fun onResume() {
        super.onResume()
        scannerManager?.startScaning()
    }

    override fun onPause() {
        super.onPause()
        scannerManager?.stopScaning()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cargaTablaJob?.cancel()
        cargaTablaJob = null
        toneGenerator?.release()
        toneGenerator = null
        soundHelper = null
        scannerManager = null
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configurarEventos() {
        binding.ingresoManual.setOnClickListener { addManual() }
    }

    private fun configurarRecyclerView() {
        etiquetasAdapter = EtiquetasAdapter { etiqueta ->
            etiqueta.id?.let { id ->
                verifyIdentity(id.toString())
            }
        }

        binding.recyclerEtiquetas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = etiquetasAdapter
            setHasFixedSize(true)
        }
    }

    private fun configurarSelectores() {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = withContext(Dispatchers.IO) {
                val movimientos = db.movimientos.getAllMovimientos()
                val camaras = db.camaras.getAllCamaras()
                Pair(movimientos, camaras)
            }

            listaMovimientos = datos.first
            val listaCamaras = datos.second
            val listaTurnos = turnos.toList()
            val adapterCamara = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listaCamaras.map { it.nombreCamara }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerCamara.adapter = adapterCamara
            binding.spinnerCamara.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        camaraSeleccionada = listaCamaras[position]
                        actualizarTablaFiltrada()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            val adapterTurno = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listaTurnos
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.turnoSpinner.adapter = adapterTurno
            binding.turnoSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        turnoSeleccionado = listaTurnos[position]
                        actualizarTablaFiltrada()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            val adapterMovimiento = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listaMovimientos.map { it.tipoMovimiento }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.tipoEntrada.adapter = adapterMovimiento
            binding.tipoEntrada.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        movimientoSeleccionado = listaMovimientos[position].tipoMovimiento
                        actualizarTablaFiltrada()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun guardarEtiqueta(etiqueta: String, nota: String? = null) {
        if (_binding == null || !isAdded) return

        viewLifecycleOwner.lifecycleScope.launch {
            scanMutex.withLock {
                procesarEtiqueta(etiqueta = etiqueta, nota = nota)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun procesarEtiqueta(etiqueta: String, nota: String?) {
        if (_binding == null || !isAdded) return

        val camara = camaraSeleccionada
        val turno = turnoSeleccionado
        val movimiento = movimientoSeleccionado

        if (camara == null || turno == null || movimiento == null) {
            soundHelper?.makeBadSound()
            Toast.makeText(
                requireContext(),
                "Selecciona cámara, turno y tipo de movimiento antes de escanear",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedMov = listaMovimientos.firstOrNull { it.tipoMovimiento == movimiento }
        if (selectedMov == null) {
            soundHelper?.makeBadSound()
            Toast.makeText(requireContext(), "Movimiento no válido", Toast.LENGTH_SHORT).show()
            return
        }

        val status = withContext(Dispatchers.IO) {
            db.etiquetas.getStatusActual(etiqueta)
        }

        val factorSeleccionado = selectedMov.factor
        val idMov = selectedMov.id

        Log.d("Movimiento", "Factor seleccionado: $factorSeleccionado, Status actual: $status")

        when (factorSeleccionado) {
            1 -> {
                if (status.factor == 1) {
                    soundHelper?.makeBadSound()
                    val nombreCamaraActual = if (status.idCamara != null) {
                        withContext(Dispatchers.IO) {
                            db.camaras.getCamaraName(status.idCamara)
                        } ?: "otra cámara"
                    } else {
                        "otra cámara"
                    }

                    val mensaje = if (status.idCamara == camara.id) {
                        "Esta etiqueta ya está dentro de esta cámara — falta registrar su salida"
                    } else {
                        "Esta etiqueta está dentro de $nombreCamaraActual — debe salir antes de entrar a ${camara.nombreCamara}"
                    }

                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    return
                }
            }

            -1 -> {
                if (status.factor != 1) {
                    soundHelper?.makeBadSound()
                    Toast.makeText(
                        requireContext(),
                        "Esta etiqueta no tiene una entrada registrada",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                if (status.idCamara != camara.id) {
                    soundHelper?.makeBadSound()
                    val nombreCamaraActual = if (status.idCamara != null) {
                        withContext(Dispatchers.IO) {
                            db.camaras.getCamaraName(status.idCamara)
                        } ?: "otra cámara"
                    } else {
                        "otra cámara"
                    }

                    Toast.makeText(
                        requireContext(),
                        "Esta etiqueta está dentro de $nombreCamaraActual, no de ${camara.nombreCamara}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                soundHelper?.completeCicle()
                Toast.makeText(
                    requireContext(),
                    "Ha completado el ciclo correctamente",
                    Toast.LENGTH_LONG
                ).show()
            }

            0 -> {
                if (status.factor != 1 || status.idCamara != camara.id) {
                    soundHelper?.makeBadSound()
                    val mensaje = if (status.factor != 1) {
                        "Esta etiqueta no está en inventario (no tiene entrada)"
                    } else {
                        val nombreCamaraActual = if (status.idCamara != null) {
                            withContext(Dispatchers.IO) {
                                db.camaras.getCamaraName(status.idCamara)
                            } ?: "otra cámara"
                        } else {
                            "otra cámara"
                        }
                        "Esta etiqueta está en $nombreCamaraActual, no en ${camara.nombreCamara}"
                    }

                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        val util = Separador()
        val etiquetaParseada = util.etiquetaseparation(etiqueta)

        if (etiquetaParseada == null) {
            soundHelper?.makeBadSound()
            Toast.makeText(
                requireContext(),
                "No hay etiqueta por procesar (long=${etiqueta.length})",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val userName = UserSession.obtener(requireContext())
        val dates = DateBuilders()
        val fecha = dates.makeDate(etiquetaParseada)
        val hora = dates.makeHour(etiquetaParseada)

        val insertado = withContext(Dispatchers.IO) {
            db.etiquetas.insertarEtiqueta(
                e = etiquetaParseada,
                fecha = fecha,
                hora = hora,
                idZona = camara.idZona,
                idCamara = camara.id,
                turno = turno,
                escaneadoPor = userName,
                etiquetaEscaneada = etiqueta,
                notas = nota,
                idMovimiento = idMov
            )
        }

        if (!insertado) {
            soundHelper?.makeBadSound()
            Toast.makeText(
                requireContext(),
                "Etiqueta Anómala, ver al Administrador",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nuevaEtiqueta = withContext(Dispatchers.IO) {
            db.etiquetas.getLastEtiqueta()
        }

        soundHelper?.makeGoodSound()

        nuevaEtiqueta?.let { nueva ->
            val nuevaLista =
                ArrayList<EtiquetaGuardada>(etiquetasAdapter.currentList.size + 1).apply {
                    add(nueva)
                    addAll(etiquetasAdapter.currentList)
                }

            etiquetasAdapter.submitList(nuevaLista)
            binding.recyclerEtiquetas.scrollToPosition(0)
        }
    }

    private fun actualizarTablaFiltrada() {
        val idCamara = camaraSeleccionada?.id
        val turno = turnoSeleccionado
        val movimiento = movimientoSeleccionado

        cargaTablaJob?.cancel()
        cargaTablaJob = viewLifecycleOwner.lifecycleScope.launch {
            val resultados = withContext(Dispatchers.IO) {
                db.reportes.obtenerReporteFiltrado(
                    idCamara = idCamara,
                    turno = turno,
                    movimiento = movimiento
                )
            }

            if (_binding == null) return@launch

            etiquetasAdapter.submitList(resultados)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addManual() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val etiquetaInput = EditText(requireContext()).apply {
            hint = "Ingrese la etiqueta"
            inputType = InputType.TYPE_CLASS_PHONE
        }

        layout.addView(etiquetaInput)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Insertar etiqueta")
            .setView(layout)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val etiqueta = etiquetaInput.text.toString().trim()
                if (etiqueta.isNotEmpty()) {
                    if (etiqueta.length > 15) {
                        guardarEtiqueta(etiqueta, "Ingreso manual")
                        Toast.makeText(requireContext(), "Etiqueta Ingresada", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Por favor ingrese una etiqueta válida",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Por favor ingrese una etiqueta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        dialog.show()
    }

    fun verifyIdentity(id: String) {
        val context = requireContext()
        val layout = dialoFactory.createContenedor(context)

        val passwordInput = dialoFactory.addInputField(
            container = layout,
            titulo = "",
            hint = "Ingrese la contraseña",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val dialog = dialoFactory.createDialog(
            context = context,
            title = "Para poder editar la etiqueta, ingrese la contraseña",
            contentView = layout
        )

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = passwordInput.text.toString().trim()
                if (password == "securePass") {
                    dialog.dismiss()
                    showID(id)
                } else {
                    Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showID(etiquetaId: String) {
        val context = requireContext()
        val etiquetaUpdate: updateEtiqueta? = db.etiquetas.getOneEtiqueta(etiquetaId)
        val layout = dialoFactory.createContenedor(context)

        val etiquetaInput = dialoFactory.addInputField(
            container = layout,
            titulo = "Etiqueta",
            valorInicial = etiquetaUpdate?.etiquetaEscaneada,
            habilitado = false
        )
        val claveProducto = dialoFactory.addInputField(
            container = layout,
            titulo = "Clave Producto",
            valorInicial = etiquetaUpdate?.claveProducto,
            habilitado = true
        )
        val loteProducto = dialoFactory.addInputField(
            container = layout,
            titulo = "Lote Producto",
            valorInicial = etiquetaUpdate?.lote,
            habilitado = true
        )
        val piezas = dialoFactory.addInputField(
            container = layout,
            titulo = "Piezas",
            valorInicial = etiquetaUpdate?.piezas,
            habilitado = true
        )
        val kilos = dialoFactory.addInputField(
            container = layout,
            titulo = "Kilos",
            valorInicial = etiquetaUpdate?.kilos,
            habilitado = true
        )

        val factorActual =
            listaMovimientos.firstOrNull { it.tipoMovimiento == etiquetaUpdate?.tipoMovimiento }?.factor
        val movimientosPermitidos = if (factorActual != null) {
            listaMovimientos.filter { it.factor == factorActual }
        } else {
            listaMovimientos
        }

        dialoFactory.addTitle(layout, "Tipo de Movimiento")
        val tipoMovimientoSpinner = Spinner(context)
        val nombresMovimiento = movimientosPermitidos.map { it.tipoMovimiento }
        val adapterMovimiento = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            nombresMovimiento
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        tipoMovimientoSpinner.adapter = adapterMovimiento
        val posicionActual = nombresMovimiento.indexOf(etiquetaUpdate?.tipoMovimiento)
        if (posicionActual >= 0) tipoMovimientoSpinner.setSelection(posicionActual)
        tipoMovimientoSpinner.isEnabled = nombresMovimiento.size > 1

        layout.addView(tipoMovimientoSpinner)

        val dialog = dialoFactory.createDialog(
            context = context,
            title = "Editar Etiqueta",
            contentView = layout,
            positiveText = "Actualizar",
            negativeText = "Eliminar"
        )


        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val res = updateEtiqueta(
                            etiquetaEscaneada = etiquetaInput.text.toString(),
                            claveProducto = claveProducto.text.toString(),
                            piezas = piezas.text.toString(),
                            kilos = kilos.text.toString(),
                            lote = loteProducto.text.toString(),
                            tipoMovimiento = tipoMovimientoSpinner.selectedItem.toString()
                        )


                    viewLifecycleOwner.lifecycleScope.launch {
                        try{
                            val exito = withContext(Dispatchers.IO){
                                db.etiquetas.upsertEtiqueta(etiquetaId,res)
                            }

                            if (exito) {
                                soundHelper?.makeGoodSound()
                                actualizarTablaFiltrada()
                                dialog.dismiss()
                                Toast.makeText(context, "Éxito al actualizar", Toast.LENGTH_SHORT).show()
                            } else {
                                soundHelper?.makeBadSound()
                                Toast.makeText(context, "Tipo de movimiento no reconocido, no se actualizó", Toast.LENGTH_LONG).show()
                            }
                        }catch (e: Exception){
                            Log.d("Error", "${e}")
                            Toast.makeText(context,"Error al Actualizar", Toast.LENGTH_LONG).show()
                        }
                    }


            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("¿Eliminar la etiqueta?")
                    .setMessage("Esto eliminará la etiqueta seleccionada")
                    .setPositiveButton("Sí, eliminar") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val id = etiquetaId.toLong()
                                withContext(Dispatchers.IO) {
                                    db.etiquetas.eliminarEtiqueta(id)
                                }
                                val nuevaLista = etiquetasAdapter.currentList.filter {
                                    it.id != id
                                }

                                etiquetasAdapter.submitList(nuevaLista)
                                dialog.dismiss()
                                soundHelper?.makeGoodSound()
                                Toast.makeText(context, "Éxito al borrar", Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: Exception) {
                                Log.d("Error", "${e}")
                                Toast.makeText(context, "Falló al borrar", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    }
                    .setNegativeButton("Cancelar", null)
                    .setCancelable(true)
                    .show()
            }
        }
        dialog.show()
    }
}