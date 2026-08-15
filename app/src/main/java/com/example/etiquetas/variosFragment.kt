package com.example.etiquetas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.etiquetas.databinding.FragmentVariosBinding


class VariosFragment : Fragment() {
    private var _binding: FragmentVariosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configButtons()
    }

    fun configButtons() {
        binding.movimientos.setOnClickListener { loadFragment(MovimientosFragment()) }
        binding.productos.setOnClickListener { loadFragment(SelectProductos()) }
        binding.camaras.setOnClickListener { loadFragment(CamarasFragment()) }
        binding.etiquetas.setOnClickListener { loadFragment(SeeAllEtiquetasFragment()) }
        binding.cerrarSesion.setOnClickListener {
            Toast.makeText(
                requireContext(), "Función en Desarrollo", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
    }


}