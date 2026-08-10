package com.example.etiquetas.utils

import android.media.ToneGenerator

class MakeSounds(private val toneGenerator: ToneGenerator?) {

    fun makeGoodSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 550)
    }

    fun makeBadSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 550)
    }

    fun completeCicle() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 550)
    }
}