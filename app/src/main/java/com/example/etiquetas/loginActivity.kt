package com.example.etiquetas

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.etiquetas.databinding.FragmentLoginActivityBinding

class LoginActivity : Fragment() {

    private var _binding: FragmentLoginActivityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.loginBtn.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val fragmenScan = EscanearEtiquetaFragment()

        val userName = binding.userName.text
        val tempFile = createTempFile(prefix = "userName", suffix = ".tmp")
        tempFile.writeText(userName.toString())
        userNameCache.userNameRoute = tempFile.absolutePath


        if (userName.toString().length != 0) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragmenScan)
                .addToBackStack(null)
                .commit()
        } else {
            Toast.makeText(requireContext(), "Debe de Ingresar su nombre", Toast.LENGTH_SHORT)
                .show()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
