package com.example.etiquetas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.etiquetas.database.methods.CamaraGuardada
import com.example.etiquetas.databinding.FragmentCamarasBinding

import com.example.etiquetas.databinding.FragmentCameraDetailsBinding

class CameraDetails : Fragment() {
    private var camara: CamaraGuardada? = null
    private var _binding: FragmentCameraDetailsBinding? = null
    private val binding get() = _binding!!
    private var camaraID: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            camara = if (android.os.Build.VERSION.SDK_INT >= 33) {
                it.getParcelable(ARG_CAMARA_OBJ, CamaraGuardada::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_CAMARA_OBJ)
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
        camara?.let {
            binding.camaraID.text = "Nombre: ${it.nombreCamara}\nNúmero: ${it.numCamara}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object{
        private const val ARG_CAMARA_OBJ = "camara_obj"
        fun newInstance(camara: CamaraGuardada): CameraDetails{
            val fragment = CameraDetails()
            var args = Bundle()
            args.putParcelable(ARG_CAMARA_OBJ, camara)
            fragment.arguments = args
            return fragment
        }
    }
}