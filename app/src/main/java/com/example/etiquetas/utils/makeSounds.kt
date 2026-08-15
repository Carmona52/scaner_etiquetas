package com.example.etiquetas.utils

import android.media.ToneGenerator

class MakeSounds(private val toneGenerator: ToneGenerator?) {

    fun makeGoodSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
    }

    fun makeBadSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 550)
    }

    fun completeCicle() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 550)
    }

    fun makeDeleteSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 180)
    }
}