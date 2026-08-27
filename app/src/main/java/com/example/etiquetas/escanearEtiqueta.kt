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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.etiquetas.database.DataBase
import com.example.etiquetas.database.MovimientoGuardado
import com.example.etiquetas.database.methods.CamaraGuardada
import com.example.etiquetas.database.methods.EtiquetaGuardada
import com.example.etiquetas.database.methods.updateEtiqueta
import com.example.etiquetas.databinding.EscanearEtiquetaFragmentBinding
import com.example.etiquetas.factory.dialog.dialoFactory
import com.example.etiquetas.factory.dialog.tablerow.TableCellFactory
import com.example.etiquetas.utils.DateBuilders
import com.example.etiquetas.utils.MakeSounds
import com.example.etiquetas.utils.ScanerAccess
import com.example.etiquetas.utils.Separador
import com.example.etiquetas.utils.UserSession

class EscanearEtiquetaFragment : Fragment() {
    private var _binding: EscanearEtiquetaFragmentBinding? = null
    private val binding get() = _binding!!
    private var turnoSeleccionado: String? = null
    private var movimientoSeleccionado: String? = null
    private var toneGenerator: ToneGenerator? = null
    private var soundHelper: MakeSounds? = null
    private lateinit var db: DataBase
    private var scannerManager: ScanerAccess? = null
    private var camaraSeleccionada: CamaraGuardada? = null
    private val turnos = arrayOf("Turno 1", "Turno 2", "Turno 3")
    private var listaMovimientos: List<MovimientoGuardado> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

    private fun configurarSelectores() {
        val listaTurnos = turnos.toList()
        listaMovimientos = db.movimientos.getAllMovimientos()
        val listaCamaras = db.camaras.getAllCamaras()

        val adapterCamara = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaCamaras.map { it.nombreCamara })
        adapterCamara.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCamara.adapter = adapterCamara
        binding.spinnerCamara.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                camaraSeleccionada = listaCamaras[position]
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val adapterTurno =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listaTurnos)
        adapterTurno.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.turnoSpinner.adapter = adapterTurno
        binding.turnoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                turnoSeleccionado = listaTurnos[position]
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val adapterMovimiento = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaMovimientos.map { it.tipoMovimiento })
        adapterMovimiento.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.tipoEntrada.adapter = adapterMovimiento
        binding.tipoEntrada.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                movimientoSeleccionado = listaMovimientos[position].tipoMovimiento
                actualizarTablaFiltrada()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun guardarEtiqueta(etiqueta: String, nota: String? = null) {
        if (_binding == null || !isAdded) return
        val camara = camaraSeleccionada
        val turno = turnoSeleccionado
        val movimiento = movimientoSeleccionado

        if (camara == null || turno == null || movimiento == null) {
            soundHelper?.makeBadSound()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Selecciona cámara, turno y tipo de movimiento antes de escanear",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val status = db.etiquetas.getStatusActual(etiqueta)
        val selectedMov = listaMovimientos.firstOrNull {
            it.tipoMovimiento == movimiento
        }

        if (selectedMov == null) {
            soundHelper?.makeBadSound()

            Toast.makeText(
                requireContext(), "Movimiento no válido", Toast.LENGTH_SHORT
            ).show()

            return
        }
        val factorSeleccionado = selectedMov.factor
        val idMov = selectedMov.id

        Log.d("Movimiento", "Factor seleccionado: $factorSeleccionado, Status actual: $status")

        if (factorSeleccionado == 1) {
            if (status.factor == 1) {
                soundHelper?.makeBadSound()
                val nombreCamaraActual =
                    status.idCamara?.let { db.camaras.getCamaraName(it) } ?: "otra cámara"
                val mensaje = if (status.idCamara == camara.id) {
                    "Esta etiqueta ya está dentro de esta cámara — falta registrar su salida"
                } else {
                    "Esta etiqueta está dentro de $nombreCamaraActual — debe salir antes de entrar a ${camara.nombreCamara}"
                }
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                }
                return
            }
        } else if (factorSeleccionado == -1) {
            if (status.factor != 1) {
                soundHelper?.makeBadSound()
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Esta etiqueta no tiene una entrada registrada",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            } else if (status.idCamara != camara.id) {
                soundHelper?.makeBadSound()
                val nombreCamaraActual =
                    status.idCamara?.let { db.camaras.getCamaraName(it) } ?: "otra cámara"
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Esta etiqueta está dentro de $nombreCamaraActual, no de ${camara.nombreCamara}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            } else {
                soundHelper?.completeCicle()
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(), "Ha completado el ciclo correctamente", Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else if (factorSeleccionado == 0) {
            if (status.factor != 1 || status.idCamara != camara.id) {
                soundHelper?.makeBadSound()
                val mensaje = if (status.factor != 1) {
                    "Esta etiqueta no está en inventario (no tiene entrada)"
                } else {
                    val nombreCamaraActual =
                        status.idCamara?.let { db.camaras.getCamaraName(it) } ?: "otra cámara"
                    "Esta etiqueta está en $nombreCamaraActual, no en ${camara.nombreCamara}"
                }
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        val util = Separador()
        val etiquetaParseada = util.etiquetaseparation(etiqueta)
        val userName = UserSession.obtener(requireContext())
        val dates = DateBuilders()

        if (etiquetaParseada == null) {
            soundHelper?.makeBadSound()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "No hay etiqueta por procesar (long=${etiqueta.length})",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val fecha = dates.makeDate(etiquetaParseada)
        val hora = dates.makeHour(etiquetaParseada)

        val insertado = db.etiquetas.insertarEtiqueta(
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

        if (!insertado) {
            soundHelper?.makeBadSound()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(), "Etiqueta Anomala, ver a Administrador", Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        soundHelper?.makeGoodSound()

        requireActivity().runOnUiThread {
            if (_binding != null) actualizarTablaFiltrada()
        }
    }

    private fun actualizarTablaFiltrada() {
        val resultados = db.reportes.obtenerReporteFiltrado(
            idCamara = camaraSeleccionada?.id,
            turno = turnoSeleccionado,
            movimiento = movimientoSeleccionado
        )

        if (binding.tableLayout.childCount > 1) {
            binding.tableLayout.removeViews(1, binding.tableLayout.childCount - 1)
        }

        resultados.forEach { agregarFilaDesdeDB(it) }
    }

    private fun agregarFilaDesdeDB(e: EtiquetaGuardada) {
        val fila = TableRow(requireContext())
        val context = requireContext()
        val valores = listOf(
            e.claveProducto, e.descripcionArticulo, e.piezas, e.kilos, e.lote, e.fecha, e.hora
        )

        valores.forEach { texto ->
            val textView = TableCellFactory.createCelda(context, texto.toString())
            fila.addView(textView)
        }

        val btnAccion = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setOnClickListener { verifyIdentity(e.id.toString()) }
        }

        fila.addView(btnAccion)
        binding.tableLayout.addView(fila)
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

        val dialog =
            AlertDialog.Builder(requireContext()).setTitle("Insertar etiqueta").setView(layout)
                .setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null)
                .setCancelable(true).create()

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
                        requireContext(), "Por favor ingrese una etiqueta", Toast.LENGTH_SHORT
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
        val tipoMovimientoSpinner = android.widget.Spinner(context)
        val nombresMovimiento = movimientosPermitidos.map { it.tipoMovimiento }
        val adapterMovimiento = ArrayAdapter(
            context, android.R.layout.simple_spinner_item, nombresMovimiento
        )
        adapterMovimiento.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
                try {
                    val exito = db.etiquetas.upsertEtiqueta(
                        etiquetaId, updateEtiqueta(
                            etiquetaEscaneada = etiquetaInput.text.toString(),
                            claveProducto = claveProducto.text.toString(),
                            piezas = piezas.text.toString(),
                            kilos = kilos.text.toString(),
                            lote = loteProducto.text.toString(),
                            tipoMovimiento = tipoMovimientoSpinner.selectedItem.toString()
                        )
                    )
                    if (exito) {
                        soundHelper?.makeGoodSound()
                        actualizarTablaFiltrada()
                        dialog.dismiss()
                        Toast.makeText(context, "Éxito al actualizar", Toast.LENGTH_SHORT).show()
                    } else {
                        soundHelper?.makeBadSound()
                        Toast.makeText(
                            context,
                            "Tipo de movimiento no reconocido, no se actualizó",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    soundHelper?.makeBadSound()
                    Toast.makeText(context, "Falló al actualizar", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                AlertDialog.Builder(requireContext()).setTitle("¿Eliminar la etiqueta?").setMessage(
                    "Esto eliminará la etiqueta seleccionada"
                ).setPositiveButton("Sí, eliminar") { _, _ ->
                    try {
                        db.etiquetas.eliminarEtiqueta(etiquetaId.toInt())
                        dialog.dismiss()
                        soundHelper?.makeGoodSound()
                        actualizarTablaFiltrada()
                        Toast.makeText(context, "Éxito al borrar", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Falló al borrar", Toast.LENGTH_SHORT).show()
                    }
                }.setNegativeButton("Cancelar", null).setCancelable(true).show()

            }
        }

        dialog.show()
    }
}
