package com.example.etiquetas.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.core.content.ContextCompat

class ScanerAccess(private val context: Context, private val onScanResult: (String) -> Unit) {

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val codigo = when (intent.action) {
                ACTION_SUNMI -> intent.getStringExtra(EXTRA_SUNMI)
                ACTION_ZEBRA -> intent.getStringExtra(EXTRA_ZEBRA)
                ACTION_HONEYWELL -> intent.getStringExtra(EXTRA_HONEYWELL)
                ACTION_HONEYWELL_AIDC -> {
                    intent.getStringExtra(EXTRA_HONEYWELL_AIDC)
                        ?: intent.getStringExtra("barcode_data")
                }

                else -> null
            }

            if (!codigo.isNullOrEmpty()) {
                onScanResult(codigo)
            } else {
                Toast.makeText(
                    context,
                    "El escáner envió la señal, pero el código está vacío.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun startScaning() {
        val filter = IntentFilter().apply {
            addAction(ACTION_SUNMI)
            addAction(ACTION_ZEBRA)
            addAction(ACTION_HONEYWELL)
            addAction(ACTION_HONEYWELL_AIDC)

            addCategory(Intent.CATEGORY_DEFAULT)
        }

        ContextCompat.registerReceiver(
            context, scanReceiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun stopScaning() {
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (e: IllegalArgumentException) {
        }
    }

    companion object {
        private const val ACTION_SUNMI = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val EXTRA_SUNMI = "data"
        private const val ACTION_ZEBRA = "com.example.etiquetas.SCAN"
        private const val EXTRA_ZEBRA = "com.symbol.datawedge.data_string"

        const val ACTION_HONEYWELL = "com.honeywell.scanintent.action.SCAN"
        const val EXTRA_HONEYWELL = "com.honeywell.scanintent.extra.DATA"

        const val ACTION_HONEYWELL_AIDC = "com.honeywell.aidc.action.BARCODE_DATA"
        const val EXTRA_HONEYWELL_AIDC = "data"
    }
}
