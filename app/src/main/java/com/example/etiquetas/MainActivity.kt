package com.example.etiquetas

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.escanearEtiqueta -> {
                    loadFragment(EscanearEtiquetaFragment())
                    true
                }

                R.id.crearReportes -> {
                    loadFragment(ReportesActivity())
                    true
                }

                R.id.verProductos -> {
                    loadFragment(VariosFragment())
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            setBottomNavVisible(false)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginActivity()).commit()
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun setBottomNavVisible(visible: Boolean) {
        bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
    }
}