package com.mibejjh.ankipreview.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 영어 발음 읽기용 TextToSpeech 래퍼.
 * Compose 에서 [init] 후 [speak] 로 단어를 발음한다.
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false

    /** TextToSpeech 를 비동기 초기화한다. */
    fun init() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ready = true
            }
        }
    }

    /** 영어 단어를 발음한다. 초기화 전이거나 실패 시 무시한다. */
    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ankipreview-${System.currentTimeMillis()}")
    }

    /** 리소스 해제. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}