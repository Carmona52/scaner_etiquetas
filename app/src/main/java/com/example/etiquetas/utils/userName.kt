package com.example.etiquetas.utils

import android.content.Context

object UserSession {
    private const val PREFS = "sesion_usuario"
    private const val KEY = "username"

    fun guardar(context: Context, nombre: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, nombre)
            .apply()
    }

    fun obtener(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: "desconocido"
}
